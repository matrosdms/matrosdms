/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;

import net.schwehla.matrosdms.domain.content.MDocumentStream;
import net.schwehla.matrosdms.domain.core.EArchivedState;
import net.schwehla.matrosdms.domain.core.MItem;
import net.schwehla.matrosdms.entity.DBContext;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.repository.ContextRepository;
import net.schwehla.matrosdms.repository.ItemRepository;
import net.schwehla.matrosdms.repository.UserRepository;
import net.schwehla.matrosdms.service.mapper.MItemMapper;
import net.schwehla.matrosdms.service.message.UpdateItemMessage;
import net.schwehla.matrosdms.store.MatrosObjectStoreService;

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

	@Caching(evict = {
			@CacheEvict(value = "items", key = "#uuid"),
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true),
			@CacheEvict(value = "contexts", allEntries = true)
	})
	public MItem updateItem(String uuid, UpdateItemMessage msg) {
		DBItem dbItem = itemRepository
				.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

		itemMapper.updateEntity(msg, dbItem);

		if (msg.getContextIdentifier() != null && !msg.getContextIdentifier().isBlank()) {
			if (!msg.getContextIdentifier().equals(dbItem.getInfoContext().getUuid())) {
				DBContext newContext = contextRepository
						.findByUuid(msg.getContextIdentifier())
						.orElseThrow(
								() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "New Context not found"));
				dbItem.setInfoContext(newContext);
			}
		}
		if (msg.getUserIdentifier() != null && !msg.getUserIdentifier().isBlank()) {
			if (!msg.getUserIdentifier().equals(dbItem.getUser().getUuid())) {
				DBUser newUser = userRepository
						.findByUuid(msg.getUserIdentifier())
						.orElseThrow(
								() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "New User not found"));
				dbItem.setUser(newUser);
			}
		}
		DBItem saved = itemRepository.save(dbItem);

		scheduler.schedule(
				indexItemTask.instance(
						"idx-" + saved.getUuid() + "-" + System.currentTimeMillis(), saved.getId()),
				Instant.now());

		return itemMapper.entityToModel(saved);
	}

	public MDocumentStream loadItemContentStream(String uuid) {
		DBItem dbItem = itemRepository
				.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

		MDocumentStream stream = storeService.load(uuid);

		// FIX: Correctly set Metadata from DB, overriding Store defaults
		// This ensures correct MIME type (PDF, Image, Email) reaches the browser
		if (dbItem.getFile() != null) {
			stream.setFilename(dbItem.getFile().getFilename());
			stream.setContentType(dbItem.getFile().getMimetype());
		}
		return stream;
	}

	@Cacheable(value = "items", key = "#uuid")
	public MItem loadItem(String uuid) {
		DBItem dbItem = itemRepository
				.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

		// Removed extra content loading to keep JSON slim.
		return itemMapper.entityToModel(dbItem);
	}

	@Transactional(readOnly = true)
	public Page<MItem> loadItemPage(
			String contextIdentifier, String query, EArchivedState archiveState, Pageable pageable) {
		if (EArchivedState.INCLUDEALL == archiveState) {
			return itemRepository
					.findAllByContextAndQuery(contextIdentifier, query, pageable)
					.map(itemMapper::entityToModel);
		} else {
			return itemRepository
					.findActiveByContextAndQuery(contextIdentifier, query, pageable)
					.map(itemMapper::entityToModel);
		}
	}

	@Caching(evict = {
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "items", key = "#uuid"),
			@CacheEvict(value = "contexts", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true)
	})
	public void deleteItem(String uuid) {
		DBItem item = itemRepository
				.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
		itemRepository.delete(item);
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
