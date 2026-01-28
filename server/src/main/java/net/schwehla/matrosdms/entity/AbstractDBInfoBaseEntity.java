/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Base-Entity
 *
 * @author Martin
 */
@MappedSuperclass
public abstract class AbstractDBInfoBaseEntity extends DBBaseEntity {

	public abstract Long getPK();

	@Column(unique = true, nullable = false, updatable = false, length = 16)
	String uuid;

	@Column(unique = false, nullable = false, updatable = true)
	String name;

	@Column(unique = false, nullable = true, updatable = true)
	String icon;

	@Column(unique = false, nullable = true, updatable = true)
	String description;

	@Temporal(TemporalType.TIMESTAMP)
	LocalDateTime dateCreated;

	@Temporal(TemporalType.TIMESTAMP)
	LocalDateTime dateUpdated;

	@Temporal(TemporalType.TIMESTAMP)
	LocalDateTime dateArchived;

	public LocalDateTime getDateUpdated() {
		return dateUpdated;
	}

	public LocalDateTime getDateArchived() {
		return dateArchived;
	}

	public void setDateArchived(LocalDateTime dateArchived) {
		this.dateArchived = dateArchived;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public LocalDateTime getDateCreated() {
		return dateCreated;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/** automatic property set before any database persistence */
	@PreUpdate
	@PrePersist
	public void updateDates() {

		LocalDateTime d = LocalDateTime.now();

		if (dateCreated == null) {
			dateCreated = d;
		}

		dateUpdated = d;
	}

	@Override
	public String toString() {
		return getName();
	}
}
