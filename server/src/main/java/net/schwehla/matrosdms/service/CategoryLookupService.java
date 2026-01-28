/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.domain.core.MCategory;
import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.repository.CategoryRepository;

@Service
public class CategoryLookupService {

	private static final Logger LOG = LoggerFactory.getLogger(CategoryLookupService.class);

	private final CategoryRepository categoryRepository;
	private final Map<String, String> categoryToRootMap = new ConcurrentHashMap<>();
	private final Map<String, MCategory> objectCache = new ConcurrentHashMap<>();

	public CategoryLookupService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public MCategory resolveCategory(DBCategory dbCat) {
		if (dbCat == null)
			return null;

		// Fast path: Full objects are pre-cached and contain essential data (Name,
		// Icon, UUID)
		return objectCache.computeIfAbsent(
				dbCat.getUuid(),
				uuid -> {
					MCategory m = new MCategory();
					m.setUuid(dbCat.getUuid());
					m.setName(dbCat.getName());
					m.setIcon(dbCat.getIcon());
					return m;
				});
	}

	@Transactional(readOnly = true)
	public String getRootFor(String categoryUuid) {
		return categoryToRootMap.computeIfAbsent(categoryUuid, this::calculateRoot);
	}

	private String calculateRoot(String uuid) {
		return categoryRepository
				.findByUuid(uuid)
				.map(
						cat -> {
							DBCategory current = cat;
							while (current.getParent() != null) {
								current = current.getParent();
							}
							return current.getUuid();
						})
				.orElse(null);
	}

	public void registerNewCategory(String childUuid, String parentUuid) {
		String root = getRootFor(parentUuid);
		if (root != null) {
			categoryToRootMap.put(childUuid, root);
		}
	}

	public void evictAll() {
		categoryToRootMap.clear();
		objectCache.clear();
	}

	@PostConstruct
	@Transactional(readOnly = true)
	public void warmup() {
		LOG.info("Warming up Category Lookup Cache...");
		categoryRepository
				.findAll()
				.forEach(
						cat -> {
							resolveCategory(cat);
							calculateRoot(cat.getUuid());
						});
		LOG.info("Warmup complete. Cached {} categories.", objectCache.size());
	}
}
