/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import net.schwehla.matrosdms.domain.core.EUserRole;

public abstract class BaseUserMessage {

	@NotBlank(message = "Username is required")
	private String name;

	@NotBlank(message = "Firstname is required")
	private String firstname;

	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String email;

	@NotNull(message = "Role must be specified (e.g. USER)")
	private EUserRole role = EUserRole.USER;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public EUserRole getRole() {
		return role;
	}

	public void setRole(EUserRole role) {
		this.role = role;
	}
}
