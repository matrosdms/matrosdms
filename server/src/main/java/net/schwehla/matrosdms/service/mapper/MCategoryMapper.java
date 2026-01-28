/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import net.schwehla.matrosdms.domain.core.MCategory;
import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.service.message.CreateCategoryMessage;

@Mapper(componentModel = "spring")
public interface MCategoryMapper extends BasicMapper {

	@Mapping(target = "parents", ignore = true)
	@Mapping(target = "children", ignore = true)
	@Mapping(target = "lifecycle", ignore = true) // Handled by BasicMapper
	// Calculated getters in MCategory, no source in DBCategory
	@Mapping(target = "rootElements", ignore = true)
	@Mapping(target = "selfAndAllTransitiveChildren", ignore = true)
	@Mapping(target = "selfAndTransitiveParents", ignore = true)
	MCategory entityToModel(DBCategory entity);

	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "parent", ignore = true)
	@Mapping(target = "children", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "ordinal", ignore = true)
	DBCategory modelToEntity(MCategory user);

	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "ordinal", ignore = true)
	@Mapping(target = "parent", ignore = true)
	@Mapping(target = "children", ignore = true)
	@Mapping(target = "object", ignore = true)
	DBCategory modelToEntity(CreateCategoryMessage user);

	List<MCategory> map(List<MCategory> dbUserList);
}
