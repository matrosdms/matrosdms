/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline.steps;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.domain.api.EPipelineStatus;
import net.schwehla.matrosdms.repository.ItemRepository;
import net.schwehla.matrosdms.service.pipeline.DuplicateException;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;
import net.schwehla.matrosdms.store.FileUtils;

/**
 * Checks if the incoming file is a duplicate of an already archived item.
 * Uses both the original SHA256 and the canonical SHA256 to detect:
 * - Re-uploads of the same raw file
 * - Re-uploads of files downloaded from the DMS (with embedded metadata)
 */
@Component
@Order(2) // Run after EmailEmbeddingStep (Order 1) - checks after attachments downloaded
public class DuplicateCheckStep implements PipelineStep {

	private static final Logger log = LoggerFactory.getLogger(DuplicateCheckStep.class);

	@Autowired
	ItemRepository itemRepository;

	@Autowired
	FileUtils fileUtils;

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		ctx.log("Checking for duplicates...");

		String hash = ctx.getHash();

		// Check if this hash already exists in repository
		Optional<String> existingUuid = itemRepository.findDuplicateUuid(hash);

		if (existingUuid.isPresent()) {
			String uuid = existingUuid.get();
			log.info("Duplicate detected: {} matches existing item {}", hash, uuid);

			ctx.getCurrentState().setDoublette(uuid);
			ctx.getCurrentState().setStatus(EPipelineStatus.DUPLICATE);

			throw new DuplicateException(hash, uuid);
		}

		log.debug("No duplicate found for hash: {}", hash);
	}
}
