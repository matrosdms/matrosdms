/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import jakarta.validation.constraints.NotBlank;

public class CreateItemMessage extends BaseItemMessage {

	@NotBlank(message = "Context ID (UUID) is required")
	private String contextIdentifier;

	// RENAMED from inboxFilenameHash to sha256 to match the rest of the app
	@NotBlank(message = "File Hash (SHA256) is required")
	private String sha256;

	@NotBlank(message = "User ID (UUID) is required")
	private String userIdentifier;

	public String getContextIdentifier() {
		return contextIdentifier;
	}

	public void setContextIdentifier(String contextIdentifier) {
		this.contextIdentifier = contextIdentifier;
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public String getUserIdentifier() {
		return userIdentifier;
	}

	public void setUserIdentifier(String userIdentifier) {
		this.userIdentifier = userIdentifier;
	}
}
