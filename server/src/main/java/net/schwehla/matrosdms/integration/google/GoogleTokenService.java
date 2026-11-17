/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.entity.DBGoogleAccount;
import net.schwehla.matrosdms.integration.google.GoogleOAuthClient.TokenResponse;
import net.schwehla.matrosdms.repository.GoogleAccountRepository;

/** Short-lived access tokens, cached in memory and refreshed from the stored refresh token. */
@Service
public class GoogleTokenService {

	private static final Logger log = LoggerFactory.getLogger(GoogleTokenService.class);

	static final String REVOKED_MESSAGE = "Google access was revoked or has expired. Reconnect under My Profile.";
	private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(60);

	private record Cached(String token, Instant expiresAt) {
	}

	private final Map<String, Cached> cache = new ConcurrentHashMap<>();
	private final Map<String, Object> locks = new ConcurrentHashMap<>();

	private final GoogleOAuthClient oauth;
	private final GoogleTokenCipher cipher;
	private final GoogleAccountRepository accounts;

	public GoogleTokenService(GoogleOAuthClient oauth, GoogleTokenCipher cipher, GoogleAccountRepository accounts) {
		this.oauth = oauth;
		this.cipher = cipher;
		this.accounts = accounts;
	}

	public String accessToken(DBGoogleAccount account) {
		String userUuid = account.getUserUuid();
		String cached = fresh(userUuid);
		if (cached != null) {
			return cached;
		}
		synchronized (locks.computeIfAbsent(userUuid, k -> new Object())) {
			cached = fresh(userUuid);
			if (cached != null) {
				return cached;
			}
			if (!account.isConnected()) {
				throw new GoogleApiException(0, "Google account is not connected", false);
			}
			try {
				TokenResponse t = oauth.refresh(cipher.decrypt(account.getRefreshToken()));
				put(userUuid, t.accessToken(), t.expiresInSeconds());
				return t.accessToken();
			} catch (GoogleApiException e) {
				if (e.isInvalidGrant()) {
					log.warn("Google refresh token for user {} was rejected - marking the account disconnected",
							userUuid);
					accounts.markDisconnected(userUuid, REVOKED_MESSAGE, LocalDateTime.now());
					cache.remove(userUuid);
					throw new GoogleApiException(401, REVOKED_MESSAGE, false);
				}
				throw e;
			} catch (IllegalStateException e) {
				// Undecryptable token (store password changed): only a reconnect helps
				throw new GoogleApiException(0, e.getMessage(), false);
			}
		}
	}

	public void put(String userUuid, String accessToken, long expiresInSeconds) {
		if (accessToken != null) {
			cache.put(userUuid, new Cached(accessToken, Instant.now().plusSeconds(expiresInSeconds)));
		}
	}

	public void invalidate(String userUuid) {
		cache.remove(userUuid);
	}

	private String fresh(String userUuid) {
		Cached c = cache.get(userUuid);
		return c != null && c.expiresAt().isAfter(Instant.now().plus(EXPIRY_MARGIN)) ? c.token() : null;
	}
}
