/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

public class UpdateCategoryMessage extends BaseCategoryMessage {

	// Optimistic Locking
	private Long version;

	// Optional: Allow moving (UUID)
	private String parentIdentifier;

	public String getParentIdentifier() {
		return parentIdentifier;
	}

	public void setParentIdentifier(String parentIdentifier) {
		this.parentIdentifier = parentIdentifier;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
