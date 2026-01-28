/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.manager;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.api.EPipelineStatus;
import net.schwehla.matrosdms.domain.core.EItemSource;
import net.schwehla.matrosdms.domain.inbox.InboxFile;
import net.schwehla.matrosdms.domain.inbox.SourceMetadata;
import net.schwehla.matrosdms.service.InboxPipelineService;
import net.schwehla.matrosdms.service.message.PipelineStatusMessage;
import net.schwehla.matrosdms.store.FileUtils;

@Component
public class InboxFileManager {

	public static final String FOLDER_MAIL = "mail";
	public static final String FOLDER_SCAN = "scan";

	@Autowired
	AppServerSpringConfig config;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	FileUtils fileUtils;
	@Autowired
	InboxPipelineService pipelineService;

	private static final Logger log = LoggerFactory.getLogger(InboxFileManager.class);

	@PostConstruct
	public void init() {
		Path root = Paths.get(config.getServer().getInbox().getPath());
		try {
			Files.createDirectories(root.resolve(FOLDER_MAIL));
			Files.createDirectories(root.resolve(FOLDER_SCAN));
			Files.createDirectories(Paths.get(config.getServer().getTemp().getPath()));
		} catch (IOException e) {
			log.error("Init folders failed", e);
		}
	}

	public InboxFile uploadFile(MultipartFile file) {
		try {
			Path tempUpload = Files.createTempFile("matros-upload-", ".tmp");
			file.transferTo(tempUpload);

			String hash = fileUtils.getSHA256(tempUpload);
			String ext = fileUtils.getExtension(file.getOriginalFilename());
			Path stagingDir = Paths.get(config.getServer().getTemp().getPath(), hash);

			if (Files.exists(stagingDir)) {
				Files.delete(tempUpload);
				return getInboxFileDto(hash);
			}

			Files.createDirectories(stagingDir);
			Path targetFile = stagingDir.resolve(hash + ext);
			Files.move(tempUpload, targetFile, StandardCopyOption.REPLACE_EXISTING);

			SourceMetadata meta = new SourceMetadata(file.getOriginalFilename(), "UPLOAD");
			objectMapper.writeValue(stagingDir.resolve("source.info").toFile(), meta);

			pipelineService.triggerPipeline(hash);

			return getInboxFileDto(hash);

		} catch (Exception e) {
			throw new RuntimeException("Upload failed", e);
		}
	}

	public List<InboxFile> loadInboxList() {
		List<InboxFile> items = new ArrayList<>();
		Path tempRoot = Paths.get(config.getServer().getTemp().getPath());
		if (!Files.exists(tempRoot))
			return items;

		try (Stream<Path> jobDirs = Files.list(tempRoot)) {
			jobDirs
					.filter(Files::isDirectory)
					.forEach(
							jobDir -> {
								InboxFile item = readJobState(jobDir);
								if (item != null)
									items.add(item);
							});
		} catch (IOException e) {
			log.error("Failed to list inbox", e);
		}

		items.sort(Comparator.comparing(InboxFile::getSha256).reversed());
		return items;
	}

	public InboxFile getInboxFileDto(String hash) {
		Path jobDir = Paths.get(config.getServer().getTemp().getPath(), hash);
		return Files.exists(jobDir) ? readJobState(jobDir) : null;
	}

	public Path getInboxFile(String hash) {
		Path jobDir = Paths.get(config.getServer().getTemp().getPath(), hash);
		if (!Files.exists(jobDir))
			throw new IllegalArgumentException("File not found");
		try {
			return findMainFile(jobDir, hash);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void ignoreFile(String hash) {
		deleteRecursively(Paths.get(config.getServer().getTemp().getPath(), hash));
	}

	public void moveToProcessed(String hash) {
		deleteRecursively(Paths.get(config.getServer().getTemp().getPath(), hash));
	}

	private InboxFile readJobState(Path jobDir) {
		try {
			String hash = jobDir.getFileName().toString();
			InboxFile f = new InboxFile();
			f.setSha256(hash);
			f.setSource(EItemSource.UPLOAD);

			String originalName = hash;
			Path metaFile = jobDir.resolve("source.info");
			if (Files.exists(metaFile)) {
				try {
					SourceMetadata meta = objectMapper.readValue(metaFile.toFile(), SourceMetadata.class);
					originalName = meta.originalFilename;
					if (meta.sourceFolder != null) {
						try {
							f.setSource(EItemSource.valueOf(meta.sourceFolder.toUpperCase()));
						} catch (Exception e) {
						}
					}
				} catch (Exception e) {
				}
			}

			f.getFileInfo().setOriginalFilename(originalName);
			if (originalName.toLowerCase().endsWith(".eml"))
				f.setSource(EItemSource.EMAIL);

			Path resultFile = jobDir.resolve("pipeline.json");
			if (Files.exists(resultFile)) {
				PipelineStatusMessage msg = objectMapper.readValue(resultFile.toFile(), PipelineStatusMessage.class);
				if (msg.getFileState() != null) {
					return msg.getFileState();
				}
			} else {
				f.setStatus(EPipelineStatus.PROCESSING);
				f.setProgressMessage("Processing...");
			}

			return f;

		} catch (Exception e) {
			return null;
		}
	}

	private Path findMainFile(Path jobDir, String hash) throws IOException {
		try (Stream<Path> s = Files.list(jobDir)) {
			return s.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().startsWith(hash) || Files.isRegularFile(p))
					.filter(
							p -> {
								String n = p.getFileName().toString();
								return !n.endsWith(".json")
										&& !n.endsWith(".tmp")
										&& !n.equals("source.info")
										&& !n.equals("textlayer.txt");
							})
					.findFirst()
					.orElseThrow(() -> new IOException("No content file in " + jobDir));
		}
	}

	private void deleteRecursively(Path path) {
		try (Stream<Path> walk = Files.walk(path)) {
			walk.sorted(Comparator.reverseOrder())
					.forEach(
							p -> {
								try {
									Files.delete(p);
								} catch (IOException e) {
								}
							});
		} catch (IOException e) {
		}
	}
}
