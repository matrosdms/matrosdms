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
import org.mapstruct.NullValuePropertyMappingStrategy;

import net.schwehla.matrosdms.domain.action.MAction;
import net.schwehla.matrosdms.entity.DBAction;
import net.schwehla.matrosdms.service.message.CreateActionMessage;
import net.schwehla.matrosdms.service.message.UpdateActionMessage;

@Mapper(componentModel = "spring", uses = {
		MUserMapper.class }, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MActionMapper extends BasicMapper {

	@Mapping(target = "itemIdentifier", source = "item.uuid")
	@Mapping(target = "contextIdentifier", source = "context.uuid")
	@Mapping(target = "lifecycle", ignore = true)
	MAction entityToModel(DBAction entity);

	// CREATION: Maps 'status' from BaseActionMessage.
	// If null in msg, DBAction default (OPEN) is used.
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "completedDate", ignore = true)
	@Mapping(target = "externalEtag", ignore = true)
	@Mapping(target = "assignee", ignore = true)
	@Mapping(target = "creator", ignore = true)
	@Mapping(target = "item", ignore = true)
	@Mapping(target = "context", ignore = true)
	@Mapping(target = "externalId", ignore = true)
	@Mapping(target = "externalActionTracker", ignore = true)
	@Mapping(target = "description", ignore = true)
	@Mapping(target = "icon", ignore = true)
	@Mapping(target = "status")
	DBAction modelToEntity(CreateActionMessage msg);

	// UPDATE: Maps 'status' if provided.
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "uuid", ignore = true)
	@Mapping(target = "dateCreated", ignore = true)
	@Mapping(target = "dateUpdated", ignore = true)
	@Mapping(target = "dateArchived", ignore = true)
	@Mapping(target = "creator", ignore = true)
	@Mapping(target = "item", ignore = true)
	@Mapping(target = "context", ignore = true)
	@Mapping(target = "externalId", ignore = true)
	@Mapping(target = "externalEtag", ignore = true)
	@Mapping(target = "externalActionTracker", ignore = true)
	@Mapping(target = "icon", ignore = true)
	@Mapping(target = "completedDate", ignore = true) // Handled in Service logic
	@Mapping(target = "assignee", ignore = true)
	@Mapping(target = "status")
	void updateEntity(UpdateActionMessage msg, @MappingTarget DBAction entity);

	List<MAction> map(List<DBAction> entities);
}
