/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.mapper;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import net.schwehla.matrosdms.domain.core.MCategory;
import net.schwehla.matrosdms.domain.core.MCategoryList;
import net.schwehla.matrosdms.domain.core.MContext;
import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.entity.DBContext;
import net.schwehla.matrosdms.entity.view.VW_CONTEXT;
import net.schwehla.matrosdms.repository.CategoryRepository;
import net.schwehla.matrosdms.service.CategoryLookupService;
import net.schwehla.matrosdms.service.message.CreateContextMessage;
import net.schwehla.matrosdms.service.message.UpdateContextMessage;

@Mapper(componentModel = "spring", uses = { MCategoryMapper.class })
public abstract class MContextMapper implements BasicMapper {

	@Autowired
	protected MCategoryMapper categoryMapper;
	@Autowired
	protected CategoryLookupService lookupService;
	@Autowired
	protected CategoryRepository categoryRepository;

	public List<MContext> mapViews(List<VW_CONTEXT> viewList) {
		if (viewList == null)
			return null;
		List<MContext> list = new ArrayList<>(viewList.size());
		for (VW_CONTEXT view : viewList) {
			list.add(viewToModel(view));
		}
		return list;
	}

	// LIST VIEW
	@Mapping(target = "dictionary", ignore = true)
	@Mapping(target = "lifecycle", ignore = true)
	@Mapping(target = "visible", ignore = true)
	@Mapping(target = "dateRunUntil", ignore = true)
	@Mapping(target = "itemCount", source = "sum") // Maps SQL View Sum
	public abstract MContext viewToModel(VW_CONTEXT view);

	@AfterMapping
	protected void afterViewMapping(VW_CONTEXT source, @MappingTarget MContext target) {
		mapLifecycle(source, target);
		mapCategoriesToDictionary(source.getCategoryList(), target);
	}

	// DETAIL VIEW
	@Mapping(target = "dictionary", ignore = true)
	@Mapping(target = "lifecycle", ignore = true)
	@Mapping(target = "visible", ignore = true)
	@Mapping(target = "itemCount", ignore = true) // Handled in Service
	public abstract MContext entityToModel(DBContext entity);

	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "categoryList", ignore = true)
	@Mapping(target = "itemList", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "dateRunUntil", ignore = true)
	public abstract DBContext modelToEntity(MContext element);

	// CREATE
	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "categoryList", ignore = true) // Manual mapping
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "itemList", ignore = true)
	@Mapping(target = "dateRunUntil", ignore = true)
	public abstract DBContext modelToEntity(CreateContextMessage message);

	@AfterMapping
	protected void mapCategoriesFromMessage(
			CreateContextMessage msg, @MappingTarget DBContext entity) {
		if (msg.getCategoryList() != null && !msg.getCategoryList().isEmpty()) {
			for (String uuid : msg.getCategoryList()) {
				categoryRepository.findByUuid(uuid).ifPresent(cat -> entity.getCategoryList().add(cat));
			}
		}
	}

	// UPDATE
	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "categoryList", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "itemList", ignore = true)
	@Mapping(target = "dateRunUntil", ignore = true)
	public abstract void updateEntity(UpdateContextMessage message, @MappingTarget DBContext entity);

	/**
	 * FIX: Added this method to handle category updates.
	 * 1. Clears existing categories.
	 * 2. Adds the new list from the message.
	 */
	@AfterMapping
	protected void mapCategoriesFromUpdateMessage(UpdateContextMessage msg, @MappingTarget DBContext entity) {
		if (msg.getCategoryList() != null) {
			entity.getCategoryList().clear();
			for (String uuid : msg.getCategoryList()) {
				categoryRepository.findByUuid(uuid).ifPresent(cat -> entity.getCategoryList().add(cat));
			}
		}
	}

	@AfterMapping
	protected void afterEntityMapping(DBContext source, @MappingTarget MContext target) {
		mapCategoriesToDictionary(source.getCategoryList(), target);
	}

	protected void mapCategoriesToDictionary(List<DBCategory> categories, MContext target) {
		if (categories == null || categories.isEmpty())
			return;

		for (DBCategory dbCat : categories) {
			MCategory mCat = categoryMapper.entityToModel(dbCat);
			String rootKey = lookupService.getRootFor(dbCat.getUuid());
			if (rootKey != null) {
				target.getDictionary().computeIfAbsent(rootKey, k -> new MCategoryList()).add(mCat);
			}
		}
	}
}