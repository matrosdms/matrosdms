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
import jakarta.persistence.Table;

import net.schwehla.matrosdms.domain.attribute.EAttributeType;

@Entity
@Table(name = "Attributetype", indexes = {
		@jakarta.persistence.Index(columnList = "uuid", name = "idx_attributetype_uuid") })
public class DBAttributeType extends AbstractDBInfoBaseEntityWithOrdinal {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false, name = "ATTRIBUTETYPE_ID")
	private Long id;

	// Defines the input type for the Frontend
	// Values: "TEXT", "NUMBER", "BOOLEAN", "DATE", "CURRENCY"
	@Column(name = "data_type", nullable = false)
	EAttributeType dataType;

	@Column(name = "built_in", nullable = false)
	private Boolean builtIn = false;

	@Override
	public Long getPK() {
		return id;
	}

	// Setter for MapStruct/JPA
	public void setId(Long id) {
		this.id = id;
	}

	// --- Getters and Setters ---

	public EAttributeType getDataType() {
		return dataType;
	}

	public void setDataType(EAttributeType dataType) {
		this.dataType = dataType;
	}

	public Boolean getBuiltIn() {
		return builtIn;
	}

	public void setBuiltIn(Boolean builtIn) {
		this.builtIn = builtIn;
	}
}
