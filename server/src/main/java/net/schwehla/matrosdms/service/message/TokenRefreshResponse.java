/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import net.schwehla.matrosdms.domain.core.MUser;

public class TokenRefreshResponse {
	private String accessToken;
	private String refreshToken;
	private MUser user; // Added user object

	public TokenRefreshResponse(String accessToken, String refreshToken, MUser user) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.user = user;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public MUser getUser() {
		return user;
	}
}