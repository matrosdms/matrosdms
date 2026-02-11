/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.facade;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;

import net.schwehla.matrosdms.domain.api.EPipelineStatus;
import net.schwehla.matrosdms.domain.core.EItemSource;
import net.schwehla.matrosdms.domain.core.MItem;
import net.schwehla.matrosdms.domain.inbox.InboxFile;
import net.schwehla.matrosdms.entity.DBContext;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.entity.DBItemMetadata;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.exception.MatrosServiceException;
import net.schwehla.matrosdms.manager.InboxFileManager;
import net.schwehla.matrosdms.repository.ContextRepository;
import net.schwehla.matrosdms.repository.ItemRepository;
import net.schwehla.matrosdms.repository.UserRepository;
import net.schwehla.matrosdms.service.InboxPipelineService;
import net.schwehla.matrosdms.service.mapper.MItemMapper;
import net.schwehla.matrosdms.service.message.CreateItemMessage;
import net.schwehla.matrosdms.service.message.PipelineStatusMessage;
import net.schwehla.matrosdms.store.FileUtils;
import net.schwehla.matrosdms.store.MatrosObjectStoreService;
import net.schwehla.matrosdms.store.StoreResult;
import net.schwehla.matrosdms.util.UUIDProvider;

@Service
public class ItemIngestionFacade {

	private static final Logger log = LoggerFactory.getLogger(ItemIngestionFacade.class);

	@Autowired ItemRepository itemRepository;
	@Autowired ContextRepository contextRepository;
	@Autowired UserRepository userRepository;
	@Autowired MItemMapper itemMapper;
	@Autowired InboxPipelineService pipelineService;
	@Autowired MatrosObjectStoreService storeService;
	@Autowired InboxFileManager inboxManager;
	@Autowired Scheduler scheduler;
	@Autowired Task<Long> indexItemTask;
	@Autowired ObjectMapper objectMapper;
	@Autowired UUIDProvider uuidProvider;
    @Autowired FileUtils fileUtils;

	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true),
			@CacheEvict(value = "contexts", allEntries = true)
	})
	public MItem ingestItem(CreateItemMessage itemMessage) {

		log.info("FACADE: Starting ingestion for Inbox File Hash: {}", itemMessage.getSha256());

		DBContext dbContext = contextRepository
				.findByUuid(itemMessage.getContextIdentifier())
				.orElseThrow(
						() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
								"Context not found: " + itemMessage.getContextIdentifier()));

		DBUser dbUser = userRepository
				.findByUuid(itemMessage.getUserIdentifier())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"User not found: " + itemMessage.getUserIdentifier()));

		DBItem dbItem = itemMapper.modelToEntity(itemMessage);
		dbItem.setUser(dbUser);
		dbItem.setInfoContext(dbContext);

		PipelineStatusMessage result = pipelineService.getOrWaitForResult(itemMessage.getSha256());

		if (result == null || result.getStatus() == EPipelineStatus.ERROR) {
			throw new MatrosServiceException("File processing failed or timed out.");
		}
		if (result.getStatus() == EPipelineStatus.DUPLICATE) {
			throw new MatrosServiceException("Duplicate File detected.");
		}

		dbItem.setUuid(uuidProvider.getTimeBasedUUID());

		InboxFile state = result.getFileState();

		EItemSource sourceEnum = (state != null && state.getSource() != null) ? state.getSource() : EItemSource.UPLOAD;
		dbItem.setSource(sourceEnum);

		DBItemMetadata metadata = new DBItemMetadata();
		metadata.setSource(sourceEnum.name());

		String filename = state != null ? state.getFileInfo().getOriginalFilename() : "unknown";
		String mimetype = state != null ? state.getFileInfo().getContentType() : "application/octet-stream";
		String extension = state != null ? state.getFileInfo().getExtension() : ".bin";
		String hashOriginal = state != null ? state.getSha256() : "unknown";

		metadata.setFilename(filename);
		metadata.setMimetype(mimetype);
        
        // 1. Set Gatekeeper Hash
		metadata.setSha256Original(hashOriginal);

		try {
			Path processedFile = pipelineService.getProcessedFile(hashOriginal, extension);
			Path textFile = pipelineService.getTextLayerFile(hashOriginal);

			if (Files.exists(textFile) && Files.size(textFile) > 0) {
				dbItem.setTextParsed(true);
			} else {
				dbItem.setTextParsed(false);
			}

            // 2. Calculate Canonical Hash (The file after processing, before encryption)
            String hashCanonical = fileUtils.getSHA256(processedFile);
            metadata.setSha256Canonical(hashCanonical);

            // 3. Store (Encrypts file)
			StoreResult storeResult = storeService.persist(processedFile, textFile, dbItem.getUuid(), filename);

            // 4. Set Vault Guard Hash
			metadata.setFilesize(Files.size(processedFile));
			metadata.setSha256Stored(storeResult.getSHA256());
			metadata.setCryptSettings(storeResult.getCryptSettings());
            
			dbItem.setFile(metadata);

			DBItem saved = itemRepository.save(dbItem);

			scheduler.schedule(
					indexItemTask.instance("idx-" + saved.getUuid(), saved.getId()), Instant.now());

			inboxManager.moveToProcessed(hashOriginal);
			pipelineService.cleanup(hashOriginal);

			return itemMapper.entityToModel(saved);

		} catch (Exception e) {
			throw new RuntimeException("Error processing item: " + e.getMessage(), e);
		}
	}
}