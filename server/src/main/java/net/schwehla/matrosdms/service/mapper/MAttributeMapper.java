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

import net.schwehla.matrosdms.domain.attribute.MAttributeType;
import net.schwehla.matrosdms.entity.DBAttributeType;
import net.schwehla.matrosdms.service.message.CreateAttributeMessage;

@Mapper(componentModel = "spring")
public interface MAttributeMapper extends BasicMapper {

	// Target MAttributeType has 'unit' and 'pattern' but DB entity does not
	// 'system' maps automatically because it exists in both
	@Mapping(target = "lifecycle", ignore = true)
	@Mapping(target = "pattern", ignore = true)
	@Mapping(target = "unit", ignore = true)
	MAttributeType entityToModel(DBAttributeType entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "ordinal", ignore = true)
	@Mapping(target = "dataType", source = "type")
	// 🛡️ SECURITY: Force 'system' to false for API creations
	@Mapping(target = "builtIn", constant = "false")
	DBAttributeType modelToEntity(CreateAttributeMessage message);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "ordinal", ignore = true)
	// SECURITY: Prevent overwriting the system flag during model conversion
	@Mapping(target = "builtIn", ignore = true)
	DBAttributeType modelToEntity(MAttributeType model);

	List<MAttributeType> mapTypes(List<DBAttributeType> entities);
}
