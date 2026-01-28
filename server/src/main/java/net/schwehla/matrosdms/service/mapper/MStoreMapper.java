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
import org.mapstruct.MappingTarget;

import net.schwehla.matrosdms.domain.core.MStore;
import net.schwehla.matrosdms.entity.DBStore;

@Mapper(componentModel = "spring")
public interface MStoreMapper extends BasicMapper {

	@Mapping(target = "lifecycle", ignore = true)
	MStore entityToModel(DBStore dbUser);

	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "ordinal", ignore = true)
	DBStore modelToEntity(MStore user);

	// New Update Method
	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "ordinal", ignore = true)
	void updateEntity(MStore source, @MappingTarget DBStore target);

	List<MStore> map(List<DBStore> dbUserList);
}
