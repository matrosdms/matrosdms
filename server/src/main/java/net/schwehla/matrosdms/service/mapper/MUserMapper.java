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

import net.schwehla.matrosdms.domain.core.MUser;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.service.message.CreateUserMessage;
import net.schwehla.matrosdms.service.message.UpdateUserMessage;

@Mapper(componentModel = "spring")
public interface MUserMapper extends BasicMapper {

	@Mapping(target = "password", ignore = true)
	@Mapping(target = "lifecycle", ignore = true)
	MUser entityToModel(DBUser dbUser);

	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "description", ignore = true)
	@Mapping(target = "icon", ignore = true)
	@Mapping(target = "password", ignore = true)
	DBUser modelToEntity(MUser user);

	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "version", ignore = true) // Ignore version on creation
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "description", ignore = true)
	@Mapping(target = "icon", ignore = true)
	@Mapping(target = "role", defaultValue = "USER")
	DBUser modelToEntity(CreateUserMessage user);

	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "description", ignore = true)
	@Mapping(target = "icon", ignore = true)
	@Mapping(target = "password", ignore = true)
	void updateEntity(UpdateUserMessage msg, @MappingTarget DBUser entity);

	List<MUser> map(List<DBUser> dbUserList);
}
