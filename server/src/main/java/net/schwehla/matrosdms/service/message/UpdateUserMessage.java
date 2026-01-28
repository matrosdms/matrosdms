/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.Size;

public class UpdateUserMessage extends BaseUserMessage {

	// Optimistic Locking
	private Long version;

	@Size(min = 6, message = "Password must be at least 6 characters")
	private String password;

	// NEW: Allow updating preferences (Dark Mode, etc.)
	private Map<String, Object> preferences = new HashMap<>();

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public Map<String, Object> getPreferences() {
		return preferences;
	}

	public void setPreferences(Map<String, Object> preferences) {
		this.preferences = preferences;
	}
}
