/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

// Inherits @NotBlank Name from BaseItemMessage (Required for PUT)
public class UpdateItemMessage extends BaseItemMessage {

	// Optimistic Locking
	private Long version;

	// Optional: Allow moving item to a different Context
	private String contextIdentifier;

	// Optional: Allow assigning to different User
	private String userIdentifier;

	public String getContextIdentifier() {
		return contextIdentifier;
	}

	public void setContextIdentifier(String contextIdentifier) {
		this.contextIdentifier = contextIdentifier;
	}

	public String getUserIdentifier() {
		return userIdentifier;
	}

	public void setUserIdentifier(String userIdentifier) {
		this.userIdentifier = userIdentifier;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
