/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.api.EBroadcastSource;
import net.schwehla.matrosdms.domain.api.EBroadcastType;
import net.schwehla.matrosdms.domain.api.EPipelineStatus;
import net.schwehla.matrosdms.domain.core.EItemSource;
import net.schwehla.matrosdms.domain.inbox.FileMetadata;
import net.schwehla.matrosdms.domain.inbox.InboxFile;
import net.schwehla.matrosdms.domain.inbox.SourceMetadata;
import net.schwehla.matrosdms.manager.InboxFileManager;
import net.schwehla.matrosdms.messagebus.VUEMessageBus;
import net.schwehla.matrosdms.service.message.PipelineStatusMessage;
import net.schwehla.matrosdms.service.pipeline.PipelineEvents.FileDetectedEvent;
import net.schwehla.matrosdms.store.FileUtils;

@Component
public class InboxWatchService implements Runnable, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(InboxWatchService.class);

	@Autowired
	AppServerSpringConfig config;
	@Autowired
	FileUtils fileUtils;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	ApplicationEventPublisher publisher;
	@Autowired
	InboxPipelineService pipelineService;
	@Autowired
	VUEMessageBus messageBus;

	@PostConstruct
	public void recoverStagingArea() {
		Path tempRoot = Paths.get(config.getServer().getTemp().getPath());
		if (!Files.exists(tempRoot))
			return;
		try (Stream<Path> stream = Files.list(tempRoot)) {
			stream.filter(Files::isDirectory).forEach(this::recoverJob);
		} catch (IOException e) {
			log.warn("Recovery scan failed", e);
		}
	}

	private void recoverJob(Path jobDir) {
		if (Files.exists(jobDir.resolve("source.info"))) {
			String hash = jobDir.getFileName().toString();
			log.info("♻️ Recovering Job: {}", hash);
			pipelineService.triggerPipeline(hash);
		} else {
			deleteRecursively(jobDir);
		}
	}

	@Scheduled(fixedDelay = 2000)
	public void sweepAndStage() {
		Path root = Paths.get(config.getServer().getInbox().getPath());
		List<String> sources = List.of(InboxFileManager.FOLDER_MAIL, InboxFileManager.FOLDER_SCAN, "upload");

		for (String source : sources) {
			Path dir = root.resolve(source);
			if (Files.exists(dir)) {
				try (Stream<Path> stream = Files.list(dir)) {
					stream
							.filter(Files::isRegularFile)
							.filter(this::isStable)
							.forEach(path -> processFile(path, source));
				} catch (IOException e) {
					log.error("Sweep error", e);
				}
			}
		}
	}

	private void processFile(Path sourceFile, String sourceFolderName) {
		try {
			String hash = fileUtils.getSHA256(sourceFile);
			String ext = fileUtils.getExtension(sourceFile.getFileName().toString());
			String originalName = sourceFile.getFileName().toString();

			Path jobDir = Paths.get(config.getServer().getTemp().getPath(), hash);

			if (Files.exists(jobDir)) {
				Files.delete(sourceFile);
				messageBus.sendMessageToGUI(
						EBroadcastSource.INBOX,
						EBroadcastType.STATUS,
						PipelineStatusMessage.duplicate(hash, originalName));
				return;
			}

			Files.createDirectories(jobDir);
			Path targetFile = jobDir.resolve(hash + ext);
			Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);

			SourceMetadata meta = new SourceMetadata(originalName, sourceFolderName);
			objectMapper.writeValue(jobDir.resolve("source.info").toFile(), meta);

			pipelineService.triggerPipeline(hash);

			InboxFile uiModel = new InboxFile();
			uiModel.setSha256(hash);
			uiModel.setStatus(EPipelineStatus.PROCESSING);
			uiModel.setProgressMessage("Queued...");

			FileMetadata fm = new FileMetadata();
			fm.setOriginalFilename(originalName);
			fm.setExtension(ext);
			uiModel.setFileInfo(fm);

			if (originalName.toLowerCase().endsWith(".eml"))
				uiModel.setSource(EItemSource.EMAIL);
			else if ("scan".equalsIgnoreCase(sourceFolderName))
				uiModel.setSource(EItemSource.SCAN);
			else
				uiModel.setSource(EItemSource.UPLOAD);

			publisher.publishEvent(new FileDetectedEvent(uiModel));

		} catch (Exception e) {
			log.error("Failed to stage file {}", sourceFile, e);
		}
	}

	private boolean isStable(Path p) {
		try (FileChannel c = FileChannel.open(p, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void deleteRecursively(Path path) {
		try (Stream<Path> w = Files.walk(path)) {
			w.sorted(java.util.Comparator.reverseOrder())
					.forEach(
							p -> {
								try {
									Files.delete(p);
								} catch (Exception e) {
								}
							});
		} catch (Exception e) {
		}
	}

	public void run() {
	}

	@Override
	public void destroy() {
	}
}
