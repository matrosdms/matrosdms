/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity()
@Table(name = "Store", indexes = @Index(name = "store_uuid_index", columnList = "uuid"))
@NamedQueries({
		@NamedQuery(name = "DBStore.findAll", query = "SELECT c FROM DBStore c"),
		@NamedQuery(name = "DBStore.findByUUID", query = "SELECT c FROM DBStore c where c.uuid = :uuid"),
		@NamedQuery(name = "DBStore.findById", query = "SELECT c FROM DBStore c where c.id = :id")
})
public class DBStore extends AbstractDBInfoBaseEntityWithOrdinal {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false, name = "STORE_ID")
	private Long id;

	String shortname;

	// --- GETTERS & SETTERS ---

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getShortname() {
		return shortname;
	}

	public void setShortname(String shortname) {
		this.shortname = shortname;
	}

	public Long getStoreId() {
		return this.id;
	}

	@Override
	public Long getPK() {
		return id;
	}
}
