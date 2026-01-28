/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.entity.DBContext;
import net.schwehla.matrosdms.repository.CategoryRepository;
import net.schwehla.matrosdms.repository.ContextRepository;
import net.schwehla.matrosdms.repository.ItemRepository;

@Service
@Transactional
public class ItemBatchService {

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
		for (String uuid : itemUuids) {
			itemRepository.findByUuid(uuid).ifPresent(item -> itemRepository.delete(item));
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
