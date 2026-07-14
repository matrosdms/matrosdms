/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.entity.DBContext;
import net.schwehla.matrosdms.repository.CategoryRepository;
import net.schwehla.matrosdms.repository.ContextRepository;
import net.schwehla.matrosdms.repository.ItemRepository;
import net.schwehla.matrosdms.store.MatrosObjectStoreService;

@Service
@Transactional
public class ItemBatchService {

	private static final Logger log = LoggerFactory.getLogger(ItemBatchService.class);

	@Autowired
	MatrosObjectStoreService storeService;
	@Autowired
	net.schwehla.matrosdms.service.SemanticSearchService semanticSearchService;
	@Autowired
	ItemRepository itemRepository;
	@Autowired
	ContextRepository contextRepository;
	@Autowired
	CategoryRepository categoryRepository;
	@Autowired
	EntityManager em;

	@CacheEvict(value = { "items", "itemList", "contexts", "contextList" }, allEntries = true)
	public void batchMove(List<String> itemUuids, String contextUuid) {
		DBContext target = contextRepository
				.findByUuid(contextUuid)
				.orElseThrow(() -> new IllegalArgumentException("Target Context not found"));

		for (String uuid : itemUuids) {
			itemRepository.findByUuid(uuid).ifPresent(item -> item.setInfoContext(target));
		}
	}

	@CacheEvict(value = { "items", "itemList" }, allEntries = true)
	public void batchDelete(List<String> itemUuids) {
		// Same semantics as ItemService.hardDeleteItem: the stored (encrypted)
		// file must go to trash too, otherwise batch delete leaves orphan blobs
		List<String> deleted = new ArrayList<>();
		for (String uuid : itemUuids) {
			itemRepository.findByUuid(uuid).ifPresent(item -> {
				itemRepository.delete(item);
				deleted.add(uuid);
			});
		}

		if (!deleted.isEmpty()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					for (String uuid : deleted) {
						try {
							storeService.moveToTrash(uuid);
						} catch (Exception e) {
							log.warn("Failed to move file to trash after batch delete: {}", uuid, e);
						}
						try {
							semanticSearchService.removeItem(uuid);
						} catch (Exception e) {
							log.warn("Failed to remove embedding after batch delete: {}", uuid, e.getMessage());
						}
					}
				}
			});
		}
	}

	@CacheEvict(value = { "items", "itemList" }, allEntries = true)
	public void batchTag(List<String> itemUuids, List<String> addTags, List<String> removeTags) {
		// Pre-fetch tags to avoid N+1
		List<DBCategory> toAdd = (addTags != null)
				? addTags.stream()
						.map(uuid -> categoryRepository.findByUuid(uuid).orElse(null))
						.filter(java.util.Objects::nonNull)
						.toList()
				: List.of();

		for (String uuid : itemUuids) {
			itemRepository
					.findByUuid(uuid)
					.ifPresent(
							item -> {
								if (removeTags != null) {
									item.getKindList().removeIf(cat -> removeTags.contains(cat.getUuid()));
								}
								for (DBCategory cat : toAdd) {
									if (!item.getKindList().contains(cat)) {
										item.getKindList().add(cat);
									}
								}
							});
		}
	}
}
