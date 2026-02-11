/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import net.schwehla.matrosdms.domain.attribute.MAttribute;
import net.schwehla.matrosdms.domain.core.MCategoryList;
import net.schwehla.matrosdms.domain.core.MFileMetadata;
import net.schwehla.matrosdms.domain.core.MItem;
import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.entity.DBItemMetadata;
import net.schwehla.matrosdms.entity.DBStore;
import net.schwehla.matrosdms.repository.CategoryRepository;
import net.schwehla.matrosdms.repository.StoreRepository;
import net.schwehla.matrosdms.service.CategoryLookupService;
import net.schwehla.matrosdms.service.domain.AttributeLookupService;
import net.schwehla.matrosdms.service.message.BaseItemMessage;
import net.schwehla.matrosdms.service.message.CreateItemMessage;
import net.schwehla.matrosdms.service.message.UpdateItemMessage;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class MItemMapper implements BasicMapper {

	@Autowired protected CategoryLookupService categoryCache;
	@Autowired protected AttributeLookupService attributeLookup;
	@Autowired protected CategoryRepository categoryRepository;
	@Autowired protected StoreRepository storeRepository;
	@Autowired protected MCategoryMapper categoryMapper;

	public abstract List<MItem> map(List<DBItem> items);

	@Mapping(target = "lifecycle", ignore = true)
	@Mapping(target = "metadata", source = "file")
	@Mapping(target = "context", source = "infoContext")
	@Mapping(target = "kindList", ignore = true)
	@Mapping(target = "attributeList", ignore = true)
	@Mapping(target = "storeIdentifier", source = "store.uuid")
	@Mapping(target = "storeItemNumber", source = "storageItemIdentifier")
	public abstract MItem entityToModel(DBItem dbItem);

	@AfterMapping
	protected void afterEntityToModel(DBItem source, @MappingTarget MItem target) {
		if (source.getKindList() != null) {
			MCategoryList list = new MCategoryList();
			for (DBCategory dbCat : source.getKindList()) {
				list.add(categoryCache.resolveCategory(dbCat));
			}
			target.setKindList(list);
		}

		if (source.getAttributes() != null) {
			List<MAttribute> attrList = new ArrayList<>();
			for (Map.Entry<String, Object> entry : source.getAttributes().entrySet()) {
				String typeUuid = entry.getKey();
				String name = attributeLookup.getName(typeUuid);
				if (name != null) {
					MAttribute attr = new MAttribute();
					attr.setUuid(typeUuid);
					attr.setName(name);
					attr.setType(attributeLookup.getType(typeUuid));
					Object val = entry.getValue();
					if (val instanceof Map) {
						Map<?, ?> mapVal = (Map<?, ?>) val;
						if (mapVal.containsKey("value")) {
							val = mapVal.get("value");
						}
					}
					attr.setValue(val);
					attrList.add(attr);
				}
			}
			target.setAttributeList(attrList);
		}
	}

    // UPDATED MAPPING: Included Canonical Hash
	@Mapping(target = "sha256", source = "sha256Original")
    @Mapping(target = "sha256Canonical", source = "sha256Canonical")
	@Mapping(target = "textLayer", ignore = true)
	public abstract MFileMetadata mapMetadata(DBItemMetadata file);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "attributes", ignore = true)
	@Mapping(target = "kindList", ignore = true)
	@Mapping(target = "store", ignore = true)
	@Mapping(target = "infoContext", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "file", ignore = true)
	@Mapping(target = "storageItemIdentifier", source = "storeItemNumber")
	public abstract DBItem modelToEntity(CreateItemMessage message);

	@AfterMapping
	protected void afterCreate(CreateItemMessage msg, @MappingTarget DBItem entity) {
		sharedAfterMapping(msg, entity);
	}

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "infoContext", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "file", ignore = true)
	@Mapping(target = "kindList", ignore = true)
	@Mapping(target = "store", ignore = true)
	@Mapping(target = "attributes", ignore = true)
	@Mapping(target = "storageItemIdentifier", source = "storeItemNumber")
	public abstract void updateEntity(UpdateItemMessage message, @MappingTarget DBItem entity);

	@AfterMapping
	protected void afterUpdate(UpdateItemMessage msg, @MappingTarget DBItem entity) {
		sharedAfterMapping(msg, entity);
	}

	protected void sharedAfterMapping(BaseItemMessage msg, DBItem entity) {
		if (msg.getAttributes() != null) {
			Map<String, Object> cleanMap = new HashMap<>();
			for (Map.Entry<String, Object> entry : msg.getAttributes().entrySet()) {
				if (attributeLookup.getName(entry.getKey()) != null) {
					cleanMap.put(entry.getKey(), entry.getValue());
				} else {
					throw new IllegalArgumentException("Unknown Attribute Type UUID: " + entry.getKey());
				}
			}
			entity.setAttributes(cleanMap);
		}
		if (msg.getKindList() != null) {
			entity.getKindList().clear();
			for (String catUuid : msg.getKindList()) {
				categoryRepository.findByUuid(catUuid).ifPresent(c -> entity.getKindList().add(c));
			}
		}
		if (msg.getStoreIdentifier() != null && !msg.getStoreIdentifier().isBlank()) {
			if (entity.getStore() == null || !entity.getStore().getUuid().equals(msg.getStoreIdentifier())) {
				DBStore store = storeRepository.findByUuid(msg.getStoreIdentifier()).orElse(null);
				entity.setStore(store);
			}
		} else if (msg.getStoreIdentifier() != null && msg.getStoreIdentifier().isBlank()) {
			entity.setStore(null);
		}
	}
}