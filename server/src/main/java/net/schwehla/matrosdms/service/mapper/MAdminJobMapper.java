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

import net.schwehla.matrosdms.domain.admin.MAdminJob;
import net.schwehla.matrosdms.domain.admin.MAdminJobLog;
import net.schwehla.matrosdms.entity.admin.DBAdminJob;
import net.schwehla.matrosdms.entity.admin.DBAdminJobLog;

@Mapper(componentModel = "spring")
public interface MAdminJobMapper {

	MAdminJob entityToModel(DBAdminJob entity);

	// Explicitly ignore the back-reference to prevent cycles if you were to map
	// back
	// (though here we are just mapping Entity -> Model)
	MAdminJobLog logEntityToModel(DBAdminJobLog entity);

	List<MAdminJob> map(List<DBAdminJob> entities);
}
