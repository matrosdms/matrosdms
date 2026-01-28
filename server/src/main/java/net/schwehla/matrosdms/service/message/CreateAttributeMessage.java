/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import net.schwehla.matrosdms.domain.attribute.EAttributeType;
import net.schwehla.matrosdms.domain.core.MBaseElement;

public class CreateAttributeMessage extends MBaseElement {

	private static final long serialVersionUID = 1L;

	String key;
	EAttributeType type;

	String defaultValueScript;
	String validateScript;
	String pattern;
	String unit;

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getValidateScript() {
		return validateScript;
	}

	public void setValidateScript(String validateScript) {
		this.validateScript = validateScript;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getDefaultValueScript() {
		return defaultValueScript;
	}

	public void setDefaultValueScript(String defaultValueScript) {
		this.defaultValueScript = defaultValueScript;
	}

	public EAttributeType getType() {
		return type;
	}

	public void setType(EAttributeType type) {
		this.type = type;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
