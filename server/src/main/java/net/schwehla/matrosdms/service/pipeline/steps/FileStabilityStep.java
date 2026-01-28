/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline.steps;

import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;

@Component
@Order(0) // CRITICAL: Must run before Metadata Extraction
public class FileStabilityStep implements PipelineStep {

	private static final Logger log = LoggerFactory.getLogger(FileStabilityStep.class);

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		ctx.log("Checking file stability...");
		log.debug("Checking stability: {}", ctx.getOriginalFile());

		long lastSize = -1;
		int attempts = 0;

		while (attempts < 8) {
			if (!Files.exists(ctx.getOriginalFile()))
				throw new IllegalStateException("File vanished");

			try (FileChannel channel = FileChannel.open(ctx.getOriginalFile(), StandardOpenOption.READ)) {
				long currentSize = channel.size();
				// If size is stable and > 0, we proceed
				if (currentSize == lastSize && currentSize > 0)
					return;
				lastSize = currentSize;
			} catch (OverlappingFileLockException e) {
				// Locked by OS (upload in progress)
			}

			TimeUnit.MILLISECONDS.sleep(500 * (attempts + 1));
			attempts++;
		}

		throw new IllegalStateException("Timeout waiting for file lock: " + ctx.getOriginalFile());
	}
}
