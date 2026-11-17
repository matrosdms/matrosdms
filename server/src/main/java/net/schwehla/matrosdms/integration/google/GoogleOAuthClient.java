/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig.GoogleConfig;

/** Google OAuth 2.0 authorization-code flow with PKCE, for an installed app. */
@Component
public class GoogleOAuthClient {

	private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);

	public static final String SCOPE_TASKS = "https://www.googleapis.com/auth/tasks";
	/**
	 * Lets MatrosDMS create its own secondary calendar and manage events on it, and
	 * nothing else: it cannot read the user's other calendars.
	 */
	public static final String SCOPE_CALENDAR = "https://www.googleapis.com/auth/calendar.app.created";

	/**
	 * Connecting asks for Google Tasks only; the calendar permission is asked for when the user
	 * switches the calendar on (incremental authorization, so the Tasks grant stays).
	 */
	static String scopes(boolean withCalendar) {
		return withCalendar
				? String.join(" ", "openid", "email", SCOPE_TASKS, SCOPE_CALENDAR)
				: String.join(" ", "openid", "email", SCOPE_TASKS);
	}

	static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
	static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";

	public record TokenResponse(String accessToken, long expiresInSeconds, String refreshToken, String scope,
			String idToken) {
	}

	private final AppServerSpringConfig config;
	private final GoogleHttp http;

	public GoogleOAuthClient(AppServerSpringConfig config, GoogleHttp http) {
		this.config = config;
		this.http = http;
	}

	public String authorizationUrl(String redirectUri, String state, String codeVerifier, boolean withCalendar) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("client_id", google().getClientId());
		params.put("redirect_uri", redirectUri);
		params.put("response_type", "code");
		params.put("scope", scopes(withCalendar));
		// Keeps what the user granted before: adding the calendar later does not drop Tasks
		params.put("include_granted_scopes", "true");
		// offline + consent: Google only hands out a refresh token on a fresh consent
		params.put("access_type", "offline");
		params.put("prompt", "consent");
		params.put("state", state);
		params.put("code_challenge", codeChallenge(codeVerifier));
		params.put("code_challenge_method", "S256");
		return AUTH_URL + "?" + GoogleHttp.formEncode(params);
	}

	public TokenResponse exchangeCode(String code, String redirectUri, String codeVerifier) {
		Map<String, String> form = new LinkedHashMap<>();
		form.put("code", code);
		form.put("client_id", google().getClientId());
		form.put("client_secret", google().getClientSecret());
		form.put("redirect_uri", redirectUri);
		form.put("grant_type", "authorization_code");
		form.put("code_verifier", codeVerifier);
		return toTokenResponse(http.postForm(TOKEN_URL, form));
	}

	/** @throws GoogleApiException with {@code isInvalidGrant()} when the user revoked access */
	public TokenResponse refresh(String refreshToken) {
		Map<String, String> form = new LinkedHashMap<>();
		form.put("client_id", google().getClientId());
		form.put("client_secret", google().getClientSecret());
		form.put("refresh_token", refreshToken);
		form.put("grant_type", "refresh_token");
		return toTokenResponse(http.postForm(TOKEN_URL, form));
	}

	/** Best effort: a failed revoke must not stop a local disconnect. */
	public void revoke(String token) {
		try {
			http.postForm(REVOKE_URL, Map.of("token", token));
		} catch (Exception e) {
			log.warn("Google token revoke failed (the account stays disconnected locally): {}", e.getMessage());
		}
	}

	/**
	 * The id_token comes straight from Google's token endpoint over TLS, so its claims
	 * can be read without verifying the signature (OpenID Connect Core 3.1.3.7).
	 */
	public String emailFromIdToken(String idToken) {
		if (idToken == null) {
			return null;
		}
		String[] parts = idToken.split("\\.");
		if (parts.length < 2) {
			return null;
		}
		try {
			byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
			return http.mapper().readTree(payload).path("email").asText(null);
		} catch (Exception e) {
			return null;
		}
	}

	static String codeChallenge(String codeVerifier) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static TokenResponse toTokenResponse(JsonNode n) {
		return new TokenResponse(
				n.path("access_token").asText(null),
				n.path("expires_in").asLong(3600),
				n.path("refresh_token").asText(null),
				n.path("scope").asText(""),
				n.path("id_token").asText(null));
	}

	private GoogleConfig google() {
		return config.getIntegrations().getGoogle();
	}
}
