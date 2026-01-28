/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import net.schwehla.matrosdms.domain.admin.EConfigKey;

public class ConfigMessage {
	private EConfigKey key;
	private String value;

	public ConfigMessage() {
	}

	public ConfigMessage(EConfigKey key, String value) {
		this.key = key;
		this.value = value;
	}

	public EConfigKey getKey() {
		return key;
	}

	public void setKey(EConfigKey key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
