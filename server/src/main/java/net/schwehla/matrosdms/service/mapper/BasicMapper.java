/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

import net.schwehla.matrosdms.domain.core.MBaseElement;
import net.schwehla.matrosdms.domain.core.MLifecycle;
import net.schwehla.matrosdms.entity.AbstractDBInfoBaseEntity;

public interface BasicMapper {

	@AfterMapping
	default void mapLifecycle(AbstractDBInfoBaseEntity entity, @MappingTarget MBaseElement item) {
		MLifecycle result = getLifecycle(entity);
		item.setLifecycle(result);

		// Map the Optimistic Lock Version
		item.setVersion(entity.getVersion());
	}

	default MLifecycle getLifecycle(AbstractDBInfoBaseEntity item) {
		MLifecycle result = new MLifecycle(
				item.getPK(), item.getDateCreated(), item.getDateUpdated(), item.getDateArchived());
		return result;
	}
}
