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

import jakarta.validation.constraints.NotBlank;

public class CreateUserMessage extends BaseUserMessage {

	@NotBlank(message = "Password is required")
	private String password;

	// NEW: Allow setting defaults on creation
	private Map<String, Object> preferences = new HashMap<>();

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Map<String, Object> getPreferences() {
		return preferences;
	}

	public void setPreferences(Map<String, Object> preferences) {
		this.preferences = preferences;
	}
}
