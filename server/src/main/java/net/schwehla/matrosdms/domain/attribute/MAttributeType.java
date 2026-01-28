/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.attribute;

import java.io.Serializable;

import net.schwehla.matrosdms.domain.core.MBaseElement;

/**
 * DEFINITION: Defines a field that exists in the system (The Palette). E.g.
 * "There is a field
 * called 'Amount' and it is a Number".
 */
public class MAttributeType extends MBaseElement implements Serializable {

	private static final long serialVersionUID = 1L;

	// Enum as String: "TEXT", "NUMBER", "BOOLEAN", "DATE", "CURRENCY"
	private EAttributeType dataType;

	// Validation / Formatting
	private String unit;
	private String pattern;

	/**
	 * Flags this attribute as essential to the system core. Effect: 1. Cannot be
	 * deleted via API. 2.
	 * Key cannot be renamed. 3. Often auto-populated by backend logic.
	 */
	private Boolean system;

	public EAttributeType getDataType() {
		return dataType;
	}

	public void setDataType(EAttributeType dataType) {
		this.dataType = dataType;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public Boolean getSystem() {
		return system;
	}

	public void setSystem(Boolean system) {
		this.system = system;
	}
}
