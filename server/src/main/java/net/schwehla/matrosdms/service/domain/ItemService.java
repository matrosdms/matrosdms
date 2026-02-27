/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;

import net.schwehla.matrosdms.domain.content.MDocumentStream;
import net.schwehla.matrosdms.domain.core.EArchiveFilter;
import net.schwehla.matrosdms.domain.core.MItem;
import net.schwehla.matrosdms.entity.DBContext;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.repository.ContextRepository;
import net.schwehla.matrosdms.repository.ItemRepository;
import net.schwehla.matrosdms.repository.UserRepository;
import net.schwehla.matrosdms.service.SearchService;
import net.schwehla.matrosdms.service.mapper.MItemMapper;
import net.schwehla.matrosdms.service.message.UpdateItemMessage;
import net.schwehla.matrosdms.store.MatrosObjectStoreService;
import net.schwehla.matrosdms.store.util.FileExtensionService;

@Service
@Transactional
public class ItemService {

	@Autowired
	ItemRepository itemRepository;
	@Autowired
	MItemMapper itemMapper;
	@Autowired
	ContextRepository contextRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	MatrosObjectStoreService storeService;
	@Autowired
	Scheduler scheduler;
	@Autowired
	Task<Long> indexItemTask;
	@Autowired
	SearchService searchService;
	@Autowired
	FileExtensionService extensionService;

	@Caching(evict = {
			@CacheEvict(value = "items", key = "#uuid"),
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true),
			@CacheEvict(value = "contexts", allEntries = true)
	})
	public MItem updateItem(String uuid, UpdateItemMessage msg) {
		DBItem dbItem = itemRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

		itemMapper.updateEntity(msg, dbItem);

		if (msg.getContextIdentifier() != null && !msg.getContextIdentifier().isBlank()) {
			if (!msg.getContextIdentifier().equals(dbItem.getInfoContext().getUuid())) {
				DBContext newContext = contextRepository.findByUuid(msg.getContextIdentifier())
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "New Context not found"));
				dbItem.setInfoContext(newContext);
			}
		}
		if (msg.getUserIdentifier() != null && !msg.getUserIdentifier().isBlank()) {
			if (!msg.getUserIdentifier().equals(dbItem.getUser().getUuid())) {
				DBUser newUser = userRepository.findByUuid(msg.getUserIdentifier())
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "New User not found"));
				dbItem.setUser(newUser);
			}
		}
		DBItem saved = itemRepository.save(dbItem);

		scheduler.schedule(
				indexItemTask.instance("idx-" + saved.getUuid() + "-" + System.currentTimeMillis(), saved.getId()),
				Instant.now());

		return itemMapper.entityToModel(saved);
	}

	public MDocumentStream loadItemContentStream(String uuid) {
		DBItem dbItem = itemRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

		MDocumentStream stream = storeService.load(uuid);
		if (dbItem.getFile() != null) {
			String filename = dbItem.getFile().getFilename();
			String mimetype = dbItem.getFile().getMimetype();

			// Detect if item is missing extension entirely, add the correct extension based
			// on mimetype
			if (filename != null && extensionService.getExtension(filename).isEmpty()) {
				String ext = extensionService.getExtensionForMimeType(mimetype);
				if (ext != null && !ext.isEmpty() && !".bin".equals(ext)) {
					filename += ext;
				}
			}

			stream.setFilename(filename);
			stream.setContentType(mimetype);
		}
		return stream;
	}

	@Cacheable(value = "items", key = "#uuid")
	public MItem loadItem(String uuid) {
		DBItem dbItem = itemRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
		return itemMapper.entityToModel(dbItem);
	}

	@Transactional(readOnly = true)
	public Page<MItem> loadItemPage(
			String contextIdentifier, String query, EArchiveFilter archiveState, Pageable pageable) {

		if (EArchiveFilter.ALL == archiveState) {
			return itemRepository.findAllByContextAndQuery(contextIdentifier, query, pageable)
					.map(itemMapper::entityToModel);
		}

		if (EArchiveFilter.ARCHIVED_ONLY == archiveState) {
			return itemRepository.findArchivedByContextAndQuery(contextIdentifier, query, pageable)
					.map(itemMapper::entityToModel);
		}

		// Default: ACTIVE_ONLY
		return itemRepository.findActiveByContextAndQuery(contextIdentifier, query, pageable)
				.map(itemMapper::entityToModel);
	}

	@Caching(evict = {
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "items", key = "#uuid"),
			@CacheEvict(value = "contextList", allEntries = true)
	})
	public void archiveItem(String uuid) {
		DBItem item = itemRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		item.setDateArchived(LocalDateTime.now());
		itemRepository.save(item);

		searchService.indexSingleItem(item.getId());
	}

	@Caching(evict = {
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "items", key = "#uuid"),
			@CacheEvict(value = "contextList", allEntries = true)
	})
	public void restoreItem(String uuid) {
		DBItem item = itemRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		item.setDateArchived(null);
		itemRepository.save(item);

		searchService.indexSingleItem(item.getId());
	}

	@Caching(evict = {
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "items", key = "#uuid"),
			@CacheEvict(value = "contexts", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true)
	})
	public void hardDeleteItem(String uuid) {
		DBItem item = itemRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

		itemRepository.delete(item);

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					storeService.moveToTrash(uuid);
				} catch (Exception e) {
					System.err.println("WARN: Failed to move file to trash: " + uuid);
				}
			}
		});
	}

	@Caching(evict = {
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "items", allEntries = true),
			@CacheEvict(value = "contexts", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true)
	})
	public void deleteAllItems() {
		itemRepository.deleteAll();
	}
}