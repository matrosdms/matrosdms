/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.attribute;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum ESystemAttribute {

	// --- The Parties (Agnostic: Works for Email, Letter, Invoice) ---
	SENDER("ATTR_SENDER", "Sender / From", EAttributeType.TEXT), RECIPIENT("ATTR_RECIPIENT", "Recipient / To",
			EAttributeType.TEXT),

	// --- Financials & Classification ---
	AMOUNT("ATTR_AMOUNT", "Amount", EAttributeType.CURRENCY), TAX_YEAR("ATTR_TAXYEAR", "Tax Year",
			EAttributeType.NUMBER);

	private final String uuid;
	private final String defaultLabel;
	private final EAttributeType type;

	ESystemAttribute(String uuid, String defaultLabel, EAttributeType type) {
		this.uuid = uuid;
		this.defaultLabel = defaultLabel;
		this.type = type;
	}

	public String getUuid() {
		return uuid;
	}

	public String getDefaultLabel() {
		return defaultLabel;
	}

	public EAttributeType getType() {
		return type;
	}
}
