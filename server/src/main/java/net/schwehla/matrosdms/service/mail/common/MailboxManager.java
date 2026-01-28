/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.mail.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.manager.InboxFileManager;
import net.schwehla.matrosdms.store.FileUtils;

@Component
public class MailboxManager {

	private static final Logger log = LoggerFactory.getLogger(MailboxManager.class);
	private final AppServerSpringConfig config;

	@Autowired
	private FileUtils fileUtils;

	public MailboxManager(AppServerSpringConfig config) {
		this.config = config;
	}

	public enum MailboxType {
		INBOX, PROCESSING, ARCHIVE
	}

	public record MailboxFolder(MailboxType type, String name, Path path) {
	}

	public MailboxFolder resolve(String folderName) {
		String upper = folderName.replace("\"", "").toUpperCase();
		Path rootInbox = Paths.get(config.getServer().getInbox().getPath());

		if (upper.equals("INBOX")) {
			return new MailboxFolder(
					MailboxType.INBOX, "INBOX", rootInbox.resolve(InboxFileManager.FOLDER_MAIL));
		}
		if (upper.contains("PROCESSING") || upper.contains("TEMP")) {
			return new MailboxFolder(
					MailboxType.PROCESSING, "Staging", Paths.get(config.getServer().getTemp().getPath()));
		}
		if (upper.contains("ARCHIVE")) {
			return new MailboxFolder(
					MailboxType.ARCHIVE, "Archive", Paths.get(config.getServer().getProcessed().getPath()));
		}
		return null;
	}

	public List<Path> listMessages(MailboxFolder folder) {
		if (folder == null || !Files.exists(folder.path()))
			return Collections.emptyList();
		try (Stream<Path> stream = Files.walk(folder.path(), 1)) {
			return stream
					.filter(Files::isRegularFile)
					.filter(p -> p.toString().toLowerCase().endsWith(".eml"))
					.sorted(Comparator.comparing(Path::getFileName))
					.collect(Collectors.toList());
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	public Path storeMessage(MailboxFolder folder, byte[] data) throws IOException {
		if (!Files.exists(folder.path()))
			Files.createDirectories(folder.path());

		// HASH IMMEDIATELY: No UUIDs. Content determines identity.
		String hash = fileUtils.getSHA256(data);
		String filename = hash + ".eml";

		Path target = folder.path().resolve(filename);

		// Write (or Overwrite if duplicate content).
		// This effectively acts as a "Debounce" for identical emails arriving at the
		// same time.
		Files.write(target, data);

		log.info("📧 Stored email: {}", filename);
		return target;
	}
}
