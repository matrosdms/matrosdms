/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Base-Entity
 *
 * @author Martin
 */
@MappedSuperclass
public abstract class AbstractDBInfoBaseEntityWithOrdinal extends AbstractDBInfoBaseEntity {

	@Column(unique = false, nullable = false, updatable = true)
	int ordinal;

	public int getOrdinal() {
		return ordinal;
	}

	public void setOrdinal(int Ordinal) {
		this.ordinal = Ordinal;
	}
}
