/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import net.schwehla.matrosdms.domain.core.MUser;

public class LoginResponse {
	private String token;
	private MUser user;

	public LoginResponse(String token, MUser user) {
		this.token = token;
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public MUser getUser() {
		return user;
	}

	public void setUser(MUser user) {
		this.user = user;
	}
}
