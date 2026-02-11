/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.inbox.SourceMetadata;
import net.schwehla.matrosdms.service.message.PipelineStatusMessage;
import net.schwehla.matrosdms.service.pipeline.*;
import net.schwehla.matrosdms.service.pipeline.PipelineEvents.*;

@Service
public class InboxPipelineService {

	private static final Logger log = LoggerFactory.getLogger(InboxPipelineService.class);

	@Autowired
	List<PipelineStep> pipelineSteps;
	@Autowired
	AppServerSpringConfig config;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	ApplicationEventPublisher publisher;

	@Async("taskExecutor")
	public void triggerPipeline(String hash) {
		Path jobDir = Paths.get(config.getServer().getTemp().getPath(), hash);

		String originalName = hash;
		try {
			SourceMetadata meta = objectMapper.readValue(jobDir.resolve("source.info").toFile(), SourceMetadata.class);
			originalName = meta.originalFilename;
		} catch (Exception e) {
		}

		Path contentFile = findContentFile(jobDir, hash);
		if (contentFile == null) {
			log.error("Pipeline failed: Content file missing for {}", hash);
			return;
		}

		log.info("Pipeline START: {} ({})", hash, originalName);
		int totalSteps = pipelineSteps.size();

		// Context holds the accumulator (InboxFile)
		PipelineContext ctx = new PipelineContext(hash, jobDir, contentFile, originalName, publisher, totalSteps);

		try {
			for (int i = 0; i < totalSteps; i++) {
				PipelineStep step = pipelineSteps.get(i);
				int currentStep = i + 1;
				ctx.setCurrentStepIndex(currentStep);
                
                // FIX: Added 'originalName' to the constructor to match the new PipelineProgressEvent signature
				publisher.publishEvent(
						new PipelineProgressEvent(
								hash, originalName, "Step " + currentStep + "/" + totalSteps, currentStep, totalSteps));
                                
				step.execute(ctx);
			}

			// Save Result - Hash goes to fileHash, UUID remains null
			PipelineStatusMessage result = PipelineStatusMessage.success(hash, ctx.getCurrentState(),
					ctx.getWarnings());

			objectMapper.writeValue(jobDir.resolve("pipeline.json").toFile(), result);
			publisher.publishEvent(new PipelineResultEvent(result));

		} catch (Exception e) {
			log.error("Pipeline crashed for {}", hash, e);
			publisher.publishEvent(new PipelineErrorEvent(hash, e.getMessage()));
		}
	}

	// ... Helpers ...
	public PipelineStatusMessage getOrWaitForResult(String hash) {
		Path meta = Paths.get(config.getServer().getTemp().getPath(), hash, "pipeline.json");
		if (Files.exists(meta)) {
			try {
				return objectMapper.readValue(meta.toFile(), PipelineStatusMessage.class);
			} catch (Exception e) {
			}
		}
		return null;
	}

	public Path getProcessedFile(String hash, String extension) {
		return Paths.get(config.getServer().getTemp().getPath(), hash, hash + extension);
	}

	public Path getTextLayerFile(String hash) {
		return Paths.get(config.getServer().getTemp().getPath(), hash, "textlayer.txt");
	}

	public void cleanup(String hash) {
		Path jobDir = Paths.get(config.getServer().getTemp().getPath(), hash);
		if (Files.exists(jobDir)) {
			try (Stream<Path> walk = Files.walk(jobDir)) {
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

	private Path findContentFile(Path dir, String hash) {
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(Files::isRegularFile)
					.filter(p -> !p.toString().endsWith(".json") && !p.toString().endsWith(".txt"))
					.findFirst()
					.orElse(null);
		} catch (IOException e) {
			return null;
		}
	}
}