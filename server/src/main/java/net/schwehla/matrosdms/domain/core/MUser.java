/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import java.util.HashMap;
import java.util.Map;

public class MUser extends MBaseElement {

	public MUser() {
		super();
	}

	private static final long serialVersionUID = 1L;

	private String firstname;
	private String email;
	private String password;
	public EUserRole role;

	// JSON Preferences
	private Map<String, Object> preferences = new HashMap<>();

	public EUserRole getRole() {
		return role;
	}

	public void setRole(EUserRole role) {
		this.role = role;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

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
