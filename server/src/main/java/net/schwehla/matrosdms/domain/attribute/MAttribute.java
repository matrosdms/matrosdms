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
 * DATA: Represents a specific value assigned to an item. Used in the JSON-based
 * Flexfield system.
 *
 * <p>
 * Although named "Abstract" for legacy compatibility, this is now a CONCRETE
 * class.
 */
public class MAttribute extends MBaseElement implements Serializable {

	private static final long serialVersionUID = 1L;

	// The display name (resolved from UUID via AttributeLookupService)
	private String name;

	// The actual data (String, Number, Boolean, etc.) from the JSON
	private Object value;

	// Optional: The data type for frontend rendering hints (if needed)
	private EAttributeType type;

	private Boolean system;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public EAttributeType getType() {
		return type;
	}

	public void setType(EAttributeType type) {
		this.type = type;
	}

	public Boolean getSystem() {
		return system;
	}

	public void setSystem(Boolean system) {
		this.system = system;
	}
}
