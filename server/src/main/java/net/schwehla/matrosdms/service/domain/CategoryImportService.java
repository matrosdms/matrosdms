/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.repository.CategoryRepository;
import net.schwehla.matrosdms.service.CategoryLookupService;
import net.schwehla.matrosdms.util.UUIDProvider;

@Service
public class CategoryImportService {

	private static final Logger log = LoggerFactory.getLogger(CategoryImportService.class);

	@Autowired
	CategoryRepository categoryRepository;
	@Autowired
	CategoryLookupService categoryLookupService;
	@Autowired
	UUIDProvider uuidProvider;

	@Autowired
	@Qualifier("yamlMapper")
	ObjectMapper yamlMapper;

	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "categories", allEntries = true),
			@CacheEvict(value = "items", allEntries = true),
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "contexts", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true)
	})
	public void importFromText(String rootUuid, String yamlContent, boolean replace) {
		if (!StringUtils.hasText(yamlContent)) {
			return;
		}

		Object structure;
		try {
			structure = yamlMapper.readValue(yamlContent, Object.class);
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Invalid YAML syntax. Use 'Name:' for parents and '- Name' for lists.", e);
		}

		if (structure instanceof Map<?, ?> map && map.containsKey(rootUuid)) {
			log.info("Detected wrapped structure. Extracting content for: {}", rootUuid);
			structure = map.get(rootUuid);
		}

		importTree(rootUuid, structure, replace);
	}

	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "categories", allEntries = true),
			@CacheEvict(value = "items", allEntries = true),
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "contexts", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true)
	})
	public void importTree(String rootUuid, Object treeStructure, boolean replace) {
		if (treeStructure == null)
			return;

		DBCategory root = categoryRepository
				.findByUuid(rootUuid)
				.orElseThrow(
						() -> new IllegalArgumentException("Root category not found: " + rootUuid));

		long childCount = categoryRepository.countByParent(root);

		// STRICT CHECK:
		// If the root has children (count > 0), we MUST FAIL unless replace=true.
		// This prevents accidental messy merges.
		if (childCount > 0) {
			if (!replace) {
				// FIX: Provide a helpful error message listing the blockers
				List<String> existingNames = categoryRepository.findByParent(root).stream()
						.limit(3)
						.map(DBCategory::getName)
						.collect(Collectors.toList());

				throw new IllegalStateException(
						String.format(
								"Safety Block: Category '%s' is not empty. It contains %d items (e.g. %s). "
										+ "Set 'replace=true' to overwrite, or delete these items manually.",
								root.getName(), childCount, existingNames));
			}

			log.info("REPLACE MODE: Deleting {} existing children of {}", childCount, root.getName());
			List<DBCategory> children = categoryRepository.findByParent(root);
			categoryRepository.deleteAll(children);
			categoryRepository.flush(); // Ensure deletion is committed before inserting
		}

		processLevel(root, treeStructure);
	}

	private void processLevel(DBCategory parent, Object node) {
		if (node == null)
			return;

		Map<String, Object> currentLevelItems = normalizeLevel(node);
		if (currentLevelItems.isEmpty())
			return;

		List<DBCategory> toSave = new ArrayList<>();

		// In Replace mode, we know the parent is empty, so we just add everything.
		for (String rawName : currentLevelItems.keySet()) {
			ParsedName pn = parseName(rawName);

			DBCategory cat = new DBCategory();
			cat.setUuid(uuidProvider.getTimeBasedUUID());
			cat.setName(pn.name);
			cat.setIcon(pn.icon);
			cat.setParent(parent);
			cat.setOrdinal(0);
			toSave.add(cat);
		}

		if (!toSave.isEmpty()) {
			List<DBCategory> saved = categoryRepository.saveAll(toSave);
			categoryRepository.flush();

			// Map Names back to Saved Entities to process recursion
			Map<String, DBCategory> savedMap = saved.stream().collect(Collectors.toMap(DBCategory::getName, c -> c));

			String rootUuid = categoryLookupService.getRootFor(parent.getUuid());
			if (rootUuid == null)
				rootUuid = parent.getUuid();

			for (DBCategory s : saved) {
				categoryLookupService.registerNewCategory(s.getUuid(), rootUuid);
			}

			// Recurse
			for (Map.Entry<String, Object> entry : currentLevelItems.entrySet()) {
				ParsedName pn = parseName(entry.getKey());
				DBCategory childEntity = savedMap.get(pn.name);
				if (entry.getValue() != null) {
					processLevel(childEntity, entry.getValue());
				}
			}
		}
	}

	private Map<String, Object> normalizeLevel(Object node) {
		Map<String, Object> result = new LinkedHashMap<>();
		if (node instanceof List<?> list) {
			for (Object item : list) {
				if (item instanceof String s)
					result.put(s, null);
				else if (item instanceof Map<?, ?> m)
					m.forEach((k, v) -> result.put(String.valueOf(k), v));
			}
		} else if (node instanceof Map<?, ?> map) {
			map.forEach((k, v) -> result.put(String.valueOf(k), v));
		} else if (node instanceof String s) {
			result.put(s, null);
		}
		return result;
	}

	private record ParsedName(String name, String icon) {
	}

	private ParsedName parseName(String raw) {
		String name = raw.trim();
		String icon = null;
		if (name.startsWith("[") && name.contains("]")) {
			int end = name.indexOf("]");
			icon = name.substring(1, end);
			name = name.substring(end + 1).trim();
		}
		return new ParsedName(name, icon);
	}
}
