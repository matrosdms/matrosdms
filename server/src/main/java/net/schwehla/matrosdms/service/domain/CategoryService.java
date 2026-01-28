/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.domain.core.MCategory;
import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.exception.EntityNotFoundException;
import net.schwehla.matrosdms.repository.CategoryRepository;
import net.schwehla.matrosdms.service.CategoryLookupService;
import net.schwehla.matrosdms.service.mapper.MCategoryMapper;
import net.schwehla.matrosdms.service.message.CreateCategoryMessage;
import net.schwehla.matrosdms.service.message.UpdateCategoryMessage;
import net.schwehla.matrosdms.util.UUIDProvider;

@Service
@Transactional(readOnly = true)
public class CategoryService {

	@Autowired
	CategoryRepository categoryRepository;
	@Autowired
	MCategoryMapper categoryMapper;
	@Autowired
	UUIDProvider uuidProvider;
	@Autowired
	CategoryLookupService categoryLookupService;

	@Transactional
	@CacheEvict(value = "categories", allEntries = true)
	public MCategory createCategory(CreateCategoryMessage message, String parentUUID) {
		DBCategory dbCategory = categoryMapper.modelToEntity(message);
		DBCategory parent = categoryRepository
				.findByUuid(parentUUID)
				.orElseThrow(() -> new EntityNotFoundException("Parent Category not found"));
		dbCategory.setParent(parent);
		dbCategory.setUuid(uuidProvider.getTimeBasedUUID());
		DBCategory result = categoryRepository.save(dbCategory);
		categoryLookupService.registerNewCategory(result.getUuid(), parentUUID);
		return mapResult(result);
	}

	@Transactional
	// FIX: Flush everything because categories are embedded in Items and Contexts
	@Caching(evict = {
			@CacheEvict(value = "categories", allEntries = true),
			@CacheEvict(value = "items", allEntries = true),
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "contexts", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true)
	})
	public MCategory updateCategory(String uuid, UpdateCategoryMessage message) {
		DBCategory category = categoryRepository
				.findByUuid(uuid)
				.orElseThrow(() -> new EntityNotFoundException("Category not found"));
		category.setName(message.getName());
		category.setDescription(message.getDescription());
		category.setIcon(message.getIcon());
		if (message.getParentIdentifier() != null
				&& !message.getParentIdentifier().equals(category.getParent().getUuid())) {
			DBCategory newParent = categoryRepository
					.findByUuid(message.getParentIdentifier())
					.orElseThrow(() -> new EntityNotFoundException("New Parent not found"));
			category.setParent(newParent);
			categoryLookupService.evictAll(); // Tree changed, wipe cache
		}
		return mapResult(categoryRepository.save(category));
	}

	@Cacheable(value = "categories", key = "#uuid + '-' + #transitive")
	public MCategory getCategory(String uuid, boolean transitive) {
		var result = categoryRepository
				.findByUuid(uuid)
				.orElseThrow(() -> new EntityNotFoundException("Category not found: " + uuid));
		MCategory root = categoryMapper.entityToModel(result);
		if (transitive)
			mapRecursive(result, root);
		return root;
	}

	private void mapRecursive(DBCategory root, MCategory mapped) {
		for (DBCategory child : root.getChildren()) {
			MCategory infoChild = categoryMapper.entityToModel(child);
			mapped.connectWithChild(infoChild);
			mapRecursive(child, infoChild);
		}
	}

	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "categories", allEntries = true),
			@CacheEvict(value = "items", allEntries = true),
			@CacheEvict(value = "itemList", allEntries = true),
			@CacheEvict(value = "contexts", allEntries = true),
			@CacheEvict(value = "contextList", allEntries = true)
	})
	public void deleteCategory(String uuid) {
		categoryRepository.delete(
				categoryRepository
						.findByUuid(uuid)
						.orElseThrow(() -> new EntityNotFoundException("Category not found")));
		categoryLookupService.evictAll();
	}

	private MCategory mapResult(DBCategory result) {
		MCategory returnVal = categoryMapper.entityToModel(result);
		if (result.getChildren() != null) {
			for (DBCategory child : result.getChildren()) {
				returnVal.connectWithChild(categoryMapper.entityToModel(child));
			}
		}
		return returnVal;
	}
}
