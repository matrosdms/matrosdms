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