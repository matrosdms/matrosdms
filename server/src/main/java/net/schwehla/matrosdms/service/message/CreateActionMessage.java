/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

public class CreateActionMessage extends BaseActionMessage {

	// Only Data Links allowed. User links are handled by the JWT.
	private String itemIdentifier;
	private String contextIdentifier;

	public String getItemIdentifier() {
		return itemIdentifier;
	}

	public void setItemIdentifier(String itemIdentifier) {
		this.itemIdentifier = itemIdentifier;
	}

	public String getContextIdentifier() {
		return contextIdentifier;
	}

	public void setContextIdentifier(String contextIdentifier) {
		this.contextIdentifier = contextIdentifier;
	}
}
