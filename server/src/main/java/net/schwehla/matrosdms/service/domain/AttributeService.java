/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import net.schwehla.matrosdms.domain.attribute.MAttributeType;
import net.schwehla.matrosdms.entity.DBAttributeType;
import net.schwehla.matrosdms.repository.AttributeTypeRepository;
import net.schwehla.matrosdms.service.mapper.MAttributeMapper;
import net.schwehla.matrosdms.service.message.CreateAttributeMessage;
import net.schwehla.matrosdms.service.message.UpdateAttributeMessage;
import net.schwehla.matrosdms.util.UUIDProvider;

@Service
@Transactional
public class AttributeService {

	@Autowired
	AttributeTypeRepository attributeTypeRepository;
	@Autowired
	MAttributeMapper attributeMapper;
	@Autowired
	UUIDProvider uuidProvider;
	@Autowired
	AttributeLookupService lookupService;

	@Cacheable(value = "attributeTypes")
	public List<MAttributeType> loadAttributeTypes() {
		List<DBAttributeType> entities = attributeTypeRepository.findAll();
		return attributeMapper.mapTypes(entities);
	}

	@CacheEvict(value = "attributeTypes", allEntries = true)
	public MAttributeType createAttributeType(CreateAttributeMessage message) {
		if (attributeTypeRepository.existsByName(message.getName())) {
			throw new IllegalArgumentException(
					"Attribute with name " + message.getName() + " already exists");
		}

		DBAttributeType entity = attributeMapper.modelToEntity(message);
		entity.setUuid(uuidProvider.getTimeBasedUUID());
		entity = attributeTypeRepository.save(entity);
		lookupService.refresh();
		return attributeMapper.entityToModel(entity);
	}

	@Caching(evict = {
			@CacheEvict(value = "attributeTypes", allEntries = true),
			// FIX: Evict items because Attribute names are embedded in MItem DTOs
			@CacheEvict(value = "items", allEntries = true),
			@CacheEvict(value = "itemList", allEntries = true)
	})
	public MAttributeType updateAttributeType(String uuid, UpdateAttributeMessage message) {
		DBAttributeType entity = attributeTypeRepository
				.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type not found"));

		entity.setName(message.getName());
		entity.setDescription(message.getDescription());
		entity.setIcon(message.getIcon());

		entity = attributeTypeRepository.save(entity);
		lookupService.refresh();
		return attributeMapper.entityToModel(entity);
	}

	@Caching(evict = {
			@CacheEvict(value = "attributeTypes", allEntries = true),
			@CacheEvict(value = "items", allEntries = true),
			@CacheEvict(value = "itemList", allEntries = true)
	})
	public void deleteAttributeType(String uuid) {
		DBAttributeType type = attributeTypeRepository
				.findByUuid(uuid)
				.orElseThrow(
						() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type not found: " + uuid));

		// GUARD: Prevent deletion of System Attributes
		if (Boolean.TRUE.equals(type.getBuiltIn())) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT, "Cannot delete system attribute: " + type.getName());
		}

		attributeTypeRepository.delete(type);
		lookupService.refresh();
	}
}
