/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import net.schwehla.matrosdms.domain.attribute.EAttributeType;

public abstract class BaseAttributeMessage {

	@NotBlank(message = "Attribute name is required")
	private String name;

	private String description;

	@NotNull(message = "Attribute Type must be defined (e.g. TEXT, NUMBER)")
	private EAttributeType type;

	private String unit;
	private String icon;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public EAttributeType getType() {
		return type;
	}

	public void setType(EAttributeType type) {
		this.type = type;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
}
