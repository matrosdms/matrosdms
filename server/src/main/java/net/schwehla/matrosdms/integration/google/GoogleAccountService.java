/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.action.ESyncTarget;
import net.schwehla.matrosdms.entity.DBGoogleAccount;
import net.schwehla.matrosdms.integration.google.GoogleOAuthClient.TokenResponse;
import net.schwehla.matrosdms.repository.ActionSyncRepository;
import net.schwehla.matrosdms.repository.GoogleAccountRepository;
import net.schwehla.matrosdms.service.message.GoogleIntegrationStatus;

/** Connecting, configuring and disconnecting a user's Google account. */
@Service
public class GoogleAccountService {

	private static final Logger log = LoggerFactory.getLogger(GoogleAccountService.class);

	private static final Duration STATE_TTL = Duration.ofMinutes(10);

	/** An authorization the user started and Google has not called back for yet. */
	private record PendingAuth(String userUuid, String codeVerifier, String redirectUri, boolean withCalendar,
			Instant expiresAt) {
	}

	private final Map<String, PendingAuth> pending = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();

	private final AppServerSpringConfig config;
	private final GoogleOAuthClient oauth;
	private final GoogleTokenCipher cipher;
	private final GoogleTokenService tokens;
	private final GoogleAccountRepository accounts;
	private final ActionSyncRepository syncs;
	private final ActionSyncDispatcher dispatcher;
	private final TransactionTemplate tx;

	public GoogleAccountService(AppServerSpringConfig config, GoogleOAuthClient oauth, GoogleTokenCipher cipher,
			GoogleTokenService tokens, GoogleAccountRepository accounts, ActionSyncRepository syncs,
			ActionSyncDispatcher dispatcher, PlatformTransactionManager txManager) {
		this.config = config;
		this.oauth = oauth;
		this.cipher = cipher;
		this.tokens = tokens;
		this.accounts = accounts;
		this.syncs = syncs;
		this.dispatcher = dispatcher;
		this.tx = new TransactionTemplate(txManager);
	}

	public GoogleIntegrationStatus status(String userUuid) {
		boolean configured = config.getIntegrations().getGoogle().isConfigured();
		DBGoogleAccount a = accounts.findByUserUuid(userUuid).orElse(null);
		if (a == null) {
			return new GoogleIntegrationStatus(configured, false, null, false, false, false, false, null, null, null, 0,
					0);
		}
		return new GoogleIntegrationStatus(
				configured,
				a.isConnected(),
				a.getGoogleEmail(),
				a.isTasksEnabled(),
				a.isCalendarEnabled(),
				a.hasScope(GoogleOAuthClient.SCOPE_TASKS),
				a.hasScope(GoogleOAuthClient.SCOPE_CALENDAR),
				a.getLastError(),
				a.getLastErrorAt(),
				a.getLastPulledAt(),
				syncs.countByAccountUserUuidAndTarget(userUuid, ESyncTarget.GOOGLE_TASKS),
				syncs.countByAccountUserUuidAndTarget(userUuid, ESyncTarget.GOOGLE_CALENDAR));
	}

	/**
	 * @param calendar the user is switching the calendar on: ask Google for that permission too.
	 *        A reconnect asks for it again if the calendar was on before.
	 * @return the Google consent URL the browser is sent to
	 */
	public String startAuthorization(String userUuid, String redirectUri, boolean calendar) {
		requireConfigured();
		Instant now = Instant.now();
		pending.values().removeIf(p -> p.expiresAt().isBefore(now));

		boolean withCalendar = calendar
				|| accounts.findByUserUuid(userUuid).map(DBGoogleAccount::isCalendarEnabled).orElse(false);
		String state = randomToken();
		String verifier = randomToken();
		pending.put(state, new PendingAuth(userUuid, verifier, redirectUri, withCalendar, now.plus(STATE_TTL)));
		return oauth.authorizationUrl(redirectUri, state, verifier, withCalendar);
	}

	/**
	 * Google's redirect back. The state is single-use and ties the callback to the
	 * user who started it, because the callback request itself carries no login.
	 */
	public void completeAuthorization(String state, String code) {
		requireConfigured();
		PendingAuth auth = state != null ? pending.remove(state) : null;
		if (auth == null || auth.expiresAt().isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"The Google sign-in expired or was already used. Start it again.");
		}

		TokenResponse token = oauth.exchangeCode(code, auth.redirectUri(), auth.codeVerifier());
		if (token.refreshToken() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google returned no refresh token");
		}
		String email = oauth.emailFromIdToken(token.idToken());
		String userUuid = auth.userUuid();

		tx.executeWithoutResult(status -> {
			DBGoogleAccount account = accounts.findByUserUuid(userUuid).orElseGet(DBGoogleAccount::new);
			boolean otherGoogleAccount = account.getGoogleEmail() != null && email != null
					&& !account.getGoogleEmail().equalsIgnoreCase(email);
			if (otherGoogleAccount) {
				// What was pushed lives in the previous Google account: forget it, a
				// resync pushes everything into the new one
				log.info("User {} switched Google account; dropping sync state of the previous one", userUuid);
				syncs.deleteByAccount(userUuid);
				account.setTaskListId(null);
				account.setCalendarId(null);
			}
			boolean firstConnect = account.getId() == null;
			account.setUserUuid(userUuid);
			account.setGoogleEmail(email);
			account.setRefreshToken(cipher.encrypt(token.refreshToken()));
			account.setGrantedScopes(token.scope());
			boolean tasksGranted = account.hasScope(GoogleOAuthClient.SCOPE_TASKS);
			boolean calendarGranted = account.hasScope(GoogleOAuthClient.SCOPE_CALENDAR);
			// First connect: Tasks on. A reconnect keeps the user's toggles, minus anything
			// no longer granted. The calendar is opt-in: Google Calendar already shows
			// Google Tasks on their due date, so with both on every task shows up twice.
			// It goes on when the user asked for it with this sign-in, or had it on before.
			boolean keepToggles = !firstConnect && !otherGoogleAccount;
			account.setTasksEnabled(tasksGranted && (!keepToggles || account.isTasksEnabled()));
			account.setCalendarEnabled(
					calendarGranted && (auth.withCalendar() || keepToggles && account.isCalendarEnabled()));
			account.setLastError(null);
			account.setLastErrorAt(null);
			account.setDateConnected(LocalDateTime.now());
			if (firstConnect || otherGoogleAccount) {
				// Nothing in this account comes from MatrosDMS yet. A reconnect keeps the
				// old value, so changes made in Google while disconnected are picked up.
				account.setLastPulledAt(LocalDateTime.now());
			}
			accounts.save(account);
		});

		tokens.put(userUuid, token.accessToken(), token.expiresInSeconds());
		log.info("User {} connected Google account {} (scopes: {})", userUuid, email, token.scope());
		dispatcher.resyncUser(userUuid);
	}

	public GoogleIntegrationStatus updateSettings(String userUuid, boolean tasksEnabled, boolean calendarEnabled) {
		DBGoogleAccount account = accounts.findByUserUuid(userUuid)
				.filter(DBGoogleAccount::isConnected)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No Google account connected"));
		if (tasksEnabled && !account.hasScope(GoogleOAuthClient.SCOPE_TASKS)
				|| calendarEnabled && !account.hasScope(GoogleOAuthClient.SCOPE_CALENDAR)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Google has not granted that permission yet. Switch it on again and allow it on Google's page.");
		}
		account.setTasksEnabled(tasksEnabled);
		account.setCalendarEnabled(calendarEnabled);
		accounts.save(account);
		// Switching a target off removes what was pushed there; on pushes everything
		dispatcher.resyncUser(userUuid);
		return status(userUuid);
	}

	public int resync(String userUuid) {
		accounts.findByUserUuid(userUuid)
				.filter(DBGoogleAccount::isConnected)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No Google account connected"));
		return dispatcher.resyncUser(userUuid);
	}

	/**
	 * Revokes access and pauses the sync. The sync state is kept: reconnecting the
	 * same Google account carries on without duplicating anything. What was pushed
	 * stays in Google until the user deletes the list or calendar there.
	 */
	public GoogleIntegrationStatus disconnect(String userUuid) {
		DBGoogleAccount account = accounts.findByUserUuid(userUuid).orElse(null);
		if (account != null && account.isConnected()) {
			String stored = account.getRefreshToken();
			account.setRefreshToken(null);
			account.setLastError(null);
			account.setLastErrorAt(null);
			accounts.save(account);
			tokens.invalidate(userUuid);
			Thread.ofVirtual().name("google-revoke").start(() -> {
				try {
					oauth.revoke(cipher.decrypt(stored));
				} catch (Exception e) {
					log.warn("Google revoke skipped: {}", e.getMessage());
				}
			});
			log.info("User {} disconnected Google account {}", userUuid, account.getGoogleEmail());
		}
		return status(userUuid);
	}

	private void requireConfigured() {
		if (!config.getIntegrations().getGoogle().isConfigured()) {
			// Not 503: the frontend reads 503 as "backend offline"
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Google sync is not configured on this server (see doc/google-sync.md)");
		}
	}

	private String randomToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
