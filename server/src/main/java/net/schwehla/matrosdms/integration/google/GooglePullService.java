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
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.entity.DBActionSync;
import net.schwehla.matrosdms.entity.DBGoogleAccount;
import net.schwehla.matrosdms.integration.google.ActionSyncTarget.RemoteState;
import net.schwehla.matrosdms.repository.ActionSyncRepository;
import net.schwehla.matrosdms.repository.GoogleAccountRepository;

/**
 * Notices changes made in Google (a task ticked off on the phone, an event moved) by
 * asking for everything modified since the last poll. Google Tasks has no push
 * notifications, and Calendar's need a public HTTPS endpoint, which a local install
 * does not have - so this polls.
 *
 * <p>The poll only finds candidates; the {@link ActionSyncEngine} does the actual merge,
 * with the same locking and conflict rules as every other sync.
 */
@Service
public class GooglePullService {

	private static final Logger log = LoggerFactory.getLogger(GooglePullService.class);

	/** Overlap between polls, against clock skew between this machine and Google. */
	static final Duration OVERLAP = Duration.ofMinutes(2);
	/** First poll of an account that was never polled. */
	static final Duration FIRST_WINDOW = Duration.ofDays(1);

	private final AppServerSpringConfig config;
	private final GoogleAccountRepository accounts;
	private final ActionSyncRepository syncs;
	private final List<ActionSyncTarget> targets;
	private final GoogleTokenService tokens;
	private final ActionSyncEngine engine;

	public GooglePullService(AppServerSpringConfig config, GoogleAccountRepository accounts,
			ActionSyncRepository syncs, List<ActionSyncTarget> targets, GoogleTokenService tokens,
			ActionSyncEngine engine) {
		this.config = config;
		this.accounts = accounts;
		this.syncs = syncs;
		this.targets = targets;
		this.tokens = tokens;
		this.engine = engine;
	}

	public void pollAll() {
		if (!config.getIntegrations().getGoogle().isConfigured()) {
			return;
		}
		for (DBGoogleAccount account : accounts.findByRefreshTokenIsNotNull()) {
			try {
				poll(account);
			} catch (GoogleApiException e) {
				if (e.isRetryable()) {
					// Offline or Google hiccup: the next poll covers the same window again
					log.debug("Google poll for user {} skipped: {}", account.getUserUuid(), e.getMessage());
				} else {
					log.warn("Google poll for user {} failed: {}", account.getUserUuid(), e.getMessage());
					accounts.updateLastError(account.getUserUuid(),
							"Checking Google for changes: " + ActionSnapshot.truncate(e.getMessage(), 900),
							LocalDateTime.now());
				}
			} catch (Exception e) {
				log.warn("Google poll for user {} failed", account.getUserUuid(), e);
			}
		}
	}

	/** @return the number of actions that had changes in Google */
	int poll(DBGoogleAccount account) {
		Instant started = Instant.now();
		Instant since = since(account, started);

		Set<String> changed = new LinkedHashSet<>();
		String token = null;
		for (ActionSyncTarget target : targets) {
			String containerId = target.containerId(account);
			if (!target.isEnabled(account) || containerId == null) {
				continue;
			}
			if (token == null) {
				token = tokens.accessToken(account);
			}
			Map<String, DBActionSync> rows = syncs.findByAccountUserUuidAndTarget(account.getUserUuid(), target.type())
					.stream()
					.collect(Collectors.toMap(DBActionSync::getExternalId, Function.identity(), (a, b) -> a));
			for (RemoteState remote : target.changedSince(token, containerId, since)) {
				DBActionSync row = rows.get(remote.id());
				if (row != null && ActionSyncEngine.changedInGoogle(target, row, remote)) {
					changed.add(row.getActionUuid());
				}
			}
		}

		boolean complete = true;
		for (String actionUuid : changed) {
			try {
				engine.reconcile(actionUuid);
			} catch (Exception e) {
				// Keep the window open: the next poll finds this change again
				complete = false;
				log.info("Taking over Google change for action {} failed, retrying next poll: {}", actionUuid,
						e.getMessage());
			}
		}
		if (complete) {
			accounts.updateLastPulledAt(account.getUserUuid(), LocalDateTime.ofInstant(started, ZoneId.systemDefault()));
		}
		if (!changed.isEmpty()) {
			log.info("Google poll for user {}: {} action(s) changed in Google", account.getUserUuid(), changed.size());
		}
		return changed.size();
	}

	static Instant since(DBGoogleAccount account, Instant now) {
		LocalDateTime last = account.getLastPulledAt() != null ? account.getLastPulledAt() : account.getDateConnected();
		if (last == null) {
			return now.minus(FIRST_WINDOW);
		}
		return last.atZone(ZoneId.systemDefault()).toInstant().minus(OVERLAP);
	}
}
