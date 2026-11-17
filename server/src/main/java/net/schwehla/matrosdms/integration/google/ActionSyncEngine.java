/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.domain.action.ESyncTarget;
import net.schwehla.matrosdms.entity.DBActionSync;
import net.schwehla.matrosdms.entity.DBGoogleAccount;
import net.schwehla.matrosdms.integration.google.ActionSyncTarget.RemoteState;
import net.schwehla.matrosdms.repository.ActionRepository;
import net.schwehla.matrosdms.repository.ActionSyncRepository;
import net.schwehla.matrosdms.repository.GoogleAccountRepository;
import net.schwehla.matrosdms.service.domain.ActionRemoteChangeService;
import net.schwehla.matrosdms.service.domain.ActionRemoteChangeService.RemoteChange;

/**
 * Brings one action and its remote copies in line. Runs inside a db-scheduler job or
 * the poll job, never on a request thread.
 *
 * <p>Two phases:
 * <ol>
 * <li><b>Pull</b>: read each remote copy and fold changes made in Google into the
 * action - ticked off / reopened (Tasks) and the due date (Tasks and Calendar).
 * <li><b>Push</b>: write the merged state to every target.
 * </ol>
 * Pulling first means a push can never overwrite a change made in Google that has not
 * been fetched yet.
 *
 * <p>Changes are detected with a three-way comparison against the state last synced
 * ({@link DBActionSync#getSyncedCompleted()}, {@link DBActionSync#getSyncedDue()}): a
 * field counts as changed in Google only if it differs from that base, so the echo of
 * our own push is never mistaken for a user change. If both sides changed the same
 * field, the later change wins. Title and notes are not pulled (MatrosDMS writes the
 * notes itself), and neither are deletions: a deleted copy is recreated.
 *
 * <p>No events are replayed: the engine looks at the current state on both sides, so
 * duplicate, late or retried jobs are harmless. Jobs for the same action are
 * serialized.
 */
@Service
public class ActionSyncEngine {

	private static final Logger log = LoggerFactory.getLogger(ActionSyncEngine.class);

	private final ActionRepository actions;
	private final GoogleAccountRepository accounts;
	private final ActionSyncRepository syncs;
	private final List<ActionSyncTarget> targets;
	private final GoogleTokenService tokens;
	private final ActionRemoteChangeService remoteChanges;
	private final TransactionOperations tx;

	private final ReentrantLock[] actionLocks = new ReentrantLock[64];
	private final Map<String, ReentrantLock> containerLocks = new ConcurrentHashMap<>();

	@Autowired
	public ActionSyncEngine(ActionRepository actions, GoogleAccountRepository accounts, ActionSyncRepository syncs,
			List<ActionSyncTarget> targets, GoogleTokenService tokens, ActionRemoteChangeService remoteChanges,
			PlatformTransactionManager txManager) {
		this(actions, accounts, syncs, targets, tokens, remoteChanges, readOnly(txManager));
	}

	ActionSyncEngine(ActionRepository actions, GoogleAccountRepository accounts, ActionSyncRepository syncs,
			List<ActionSyncTarget> targets, GoogleTokenService tokens, ActionRemoteChangeService remoteChanges,
			TransactionOperations tx) {
		this.actions = actions;
		this.accounts = accounts;
		this.syncs = syncs;
		this.targets = targets;
		this.tokens = tokens;
		this.remoteChanges = remoteChanges;
		this.tx = tx;
		for (int i = 0; i < actionLocks.length; i++) {
			actionLocks[i] = new ReentrantLock();
		}
	}

	private static TransactionOperations readOnly(PlatformTransactionManager txManager) {
		TransactionTemplate t = new TransactionTemplate(txManager);
		t.setReadOnly(true);
		return t;
	}

	private record State(ActionSnapshot action, Map<ESyncTarget, DBActionSync> rows,
			Map<String, DBGoogleAccount> accounts) {
	}

	/** Per-job bookkeeping: whose account the current remote call goes to, and who succeeded. */
	private static final class Run {
		String current;
		final Set<String> succeeded = new HashSet<>();
		final Set<String> failed = new HashSet<>();
		GoogleApiException retry;

		void on(DBGoogleAccount account) {
			current = account.getUserUuid();
		}
	}

	/**
	 * @throws GoogleApiException a retryable failure, so db-scheduler tries again
	 *             later. Permanent failures are recorded on the account and swallowed.
	 */
	public void reconcile(String actionUuid) {
		ReentrantLock lock = actionLocks[Math.floorMod(actionUuid.hashCode(), actionLocks.length)];
		lock.lock();
		try {
			doReconcile(actionUuid);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Whether a remote copy differs from the last synced state, i.e. was changed in
	 * Google. Used by the poll job to pick the actions worth a reconcile.
	 */
	public static boolean changedInGoogle(ActionSyncTarget target, DBActionSync row, RemoteState remote) {
		if (!row.hasBase() || remote.gone()) {
			return false;
		}
		boolean completion = remote.completed() != null && !remote.completed().equals(row.getSyncedCompleted());
		return completion || !target.sameDue(remote.due(), row.getSyncedDue());
	}

	private void doReconcile(String actionUuid) {
		State state = tx.execute(status -> load(actionUuid));
		Run run = new Run();
		Set<ESyncTarget> gone = EnumSet.noneOf(ESyncTarget.class);
		Set<ESyncTarget> unreadable = EnumSet.noneOf(ESyncTarget.class);

		// 1. Pull
		if (state.action() != null) {
			Merge merge = new Merge(state.action());
			for (ActionSyncTarget target : targets) {
				DBActionSync row = state.rows().get(target.type());
				if (row == null) {
					continue;
				}
				run.current = null;
				try {
					RemoteState remote = fetchForPull(run, target, row, state.accounts());
					if (remote == null) {
						continue;
					}
					if (remote.gone()) {
						gone.add(target.type());
					} else {
						merge.consider(target, row, remote);
					}
					succeeded(run);
				} catch (GoogleApiException e) {
					failed(run, target, e, actionUuid);
					// never push over changes that could not be read
					unreadable.add(target.type());
				}
			}
			if (!merge.isEmpty()) {
				log.info("Action {}: taking over from {}: {}", actionUuid, merge.source(), merge.log());
				remoteChanges.apply(actionUuid, merge.toChange(), merge.source());
				state = tx.execute(status -> load(actionUuid));
			}
		}

		// 2. Push
		for (ActionSyncTarget target : targets) {
			if (unreadable.contains(target.type())) {
				continue;
			}
			run.current = null;
			try {
				pushTarget(run, target, state.action(), state.rows().get(target.type()), state.accounts(),
						gone.contains(target.type()));
				succeeded(run);
			} catch (GoogleApiException e) {
				failed(run, target, e, actionUuid);
			}
		}

		// A clean run clears an old error, so the profile page does not keep showing it
		for (String userUuid : run.succeeded) {
			DBGoogleAccount account = state.accounts().get(userUuid);
			if (!run.failed.contains(userUuid) && account != null && account.getLastError() != null) {
				accounts.updateLastError(userUuid, null, null);
			}
		}

		if (run.retry != null) {
			throw run.retry;
		}
	}

	private void succeeded(Run run) {
		if (run.current != null) {
			run.succeeded.add(run.current);
		}
	}

	private void failed(Run run, ActionSyncTarget target, GoogleApiException e, String actionUuid) {
		if (run.current != null) {
			run.failed.add(run.current);
			if (e.getStatus() == 401) {
				tokens.invalidate(run.current);
			}
			accounts.updateLastError(run.current, target.label() + ": " + ActionSnapshot.truncate(e.getMessage(), 900),
					LocalDateTime.now());
		}
		if (e.isRetryable()) {
			log.info("Sync of action {} with {} failed, will retry: {}", actionUuid, target.label(), e.getMessage());
			if (run.retry == null) {
				run.retry = e;
			}
		} else {
			log.warn("Sync of action {} with {} failed permanently: {}", actionUuid, target.label(), e.getMessage());
		}
	}

	private State load(String actionUuid) {
		ActionSnapshot action = actions.findByUuid(actionUuid).map(ActionSnapshot::of).orElse(null);

		Map<ESyncTarget, DBActionSync> rows = new EnumMap<>(ESyncTarget.class);
		Set<String> users = new HashSet<>();
		for (DBActionSync row : syncs.findByActionUuid(actionUuid)) {
			rows.put(row.getTarget(), row);
			users.add(row.getAccountUserUuid());
		}
		if (action != null && action.assigneeUuid() != null) {
			users.add(action.assigneeUuid());
		}

		Map<String, DBGoogleAccount> byUser = new HashMap<>();
		if (!users.isEmpty()) {
			for (DBGoogleAccount account : accounts.findByUserUuidIn(users)) {
				byUser.put(account.getUserUuid(), account);
			}
		}
		return new State(action, rows, byUser);
	}

	/** The remote copy, if it is one we may read and merge from; otherwise null. */
	private RemoteState fetchForPull(Run run, ActionSyncTarget target, DBActionSync row,
			Map<String, DBGoogleAccount> accountsByUser) {
		DBGoogleAccount owner = accountsByUser.get(row.getAccountUserUuid());
		if (owner == null || !owner.isConnected() || !target.isEnabled(owner) || !row.hasBase()) {
			return null;
		}
		run.on(owner);
		return target.fetch(tokens.accessToken(owner), row.getContainerId(), row.getExternalId());
	}

	/** Collects what to take over from Google, field by field. */
	private static final class Merge {
		private final ActionSnapshot action;
		private EActionStatus status;
		private String statusLog;
		private boolean dueChanged;
		private LocalDateTime due;
		private Instant dueChangedAt;
		private String dueLog;
		private final Set<String> sources = new LinkedHashSet<>();

		Merge(ActionSnapshot action) {
			this.action = action;
		}

		void consider(ActionSyncTarget target, DBActionSync base, RemoteState remote) {
			// Only for a field both sides changed does the timestamp decide
			boolean googleIsLater = isLater(remote.updated(), action.dateUpdated());

			Boolean baseCompleted = base.getSyncedCompleted();
			if (remote.completed() != null && !remote.completed().equals(baseCompleted)) {
				boolean changedHereToo = action.completed() != baseCompleted;
				if ((!changedHereToo || googleIsLater) && action.completed() != remote.completed()) {
					status = remote.completed() ? EActionStatus.DONE : EActionStatus.OPEN;
					statusLog = (remote.completed() ? "Completed in " : "Reopened in ") + target.label();
					sources.add(target.label());
				}
			}

			if (!target.sameDue(remote.due(), base.getSyncedDue())) {
				boolean changedHereToo = !target.sameDue(action.dueDate(), base.getSyncedDue());
				LocalDateTime adopted = target.adoptDue(remote.due(), action.dueDate());
				boolean laterThanOtherTarget = dueChangedAt == null || isLater(remote.updated(), dueChangedAt);
				if ((!changedHereToo || googleIsLater) && !Objects.equals(adopted, action.dueDate())
						&& laterThanOtherTarget) {
					dueChanged = true;
					due = adopted;
					dueChangedAt = remote.updated();
					dueLog = adopted == null
							? "Due date removed in " + target.label()
							: "Due date set to " + adopted.toLocalDate() + " in " + target.label();
					sources.add(target.label());
				}
			}
		}

		boolean isEmpty() {
			return status == null && !dueChanged;
		}

		String source() {
			return sources.size() == 1 ? sources.iterator().next() : "Google";
		}

		List<String> log() {
			List<String> entries = new ArrayList<>();
			if (statusLog != null) {
				entries.add(statusLog);
			}
			if (dueLog != null) {
				entries.add(dueLog);
			}
			return entries;
		}

		RemoteChange toChange() {
			return new RemoteChange(status, dueChanged, due, log());
		}

		private static boolean isLater(Instant remote, LocalDateTime localServerTime) {
			if (remote == null) {
				return false;
			}
			if (localServerTime == null) {
				return true;
			}
			return remote.isAfter(localServerTime.atZone(ZoneId.systemDefault()).toInstant());
		}

		private static boolean isLater(Instant a, Instant b) {
			return a != null && (b == null || a.isAfter(b));
		}
	}

	private void pushTarget(Run run, ActionSyncTarget target, ActionSnapshot action, DBActionSync row,
			Map<String, DBGoogleAccount> accountsByUser, boolean remoteGone) {

		// Where should the action live? In its assignee's account, if that account is
		// connected, has this target switched on, and the target wants the action.
		DBGoogleAccount desired = null;
		if (action != null && action.assigneeUuid() != null) {
			DBGoogleAccount candidate = accountsByUser.get(action.assigneeUuid());
			boolean alreadySynced = row != null && row.getAccountUserUuid().equals(action.assigneeUuid());
			if (candidate != null && candidate.isConnected() && target.isEnabled(candidate)
					&& target.wants(action, alreadySynced)) {
				desired = candidate;
			}
		}

		// Remove a remote copy that should no longer exist (action deleted or
		// finished, target switched off, or the action was reassigned)
		if (row != null) {
			DBGoogleAccount owner = accountsByUser.get(row.getAccountUserUuid());
			if (owner == null) {
				// The Google link itself is gone: nothing left to clean up remotely
				syncs.delete(row);
				row = null;
			} else if (!owner.isConnected()) {
				// Paused: leave it untouched, the reconnect resync picks it up again
				return;
			} else if (desired == null || !desired.getUserUuid().equals(owner.getUserUuid())) {
				run.on(owner);
				if (!remoteGone) {
					target.delete(tokens.accessToken(owner), row.getContainerId(), row.getExternalId());
				}
				syncs.delete(row);
				row = null;
			}
		}

		if (desired == null) {
			return;
		}

		run.on(desired);
		String token = tokens.accessToken(desired);

		if (row != null && !remoteGone) {
			try {
				String etag = target.update(token, row.getContainerId(), row.getExternalId(), action);
				row.setExternalEtag(etag);
				recordBase(row, action);
				syncs.save(row);
				return;
			} catch (GoogleApiException e) {
				if (!e.isNotFound()) {
					throw e;
				}
				// Deleted in Google: MatrosDMS still has the action, so bring it back
			}
		}

		String containerId = container(target, desired, token, null);
		ActionSyncTarget.Remote remote;
		try {
			remote = target.create(token, containerId, action);
		} catch (GoogleApiException e) {
			if (!e.isNotFound()) {
				throw e;
			}
			// The whole list/calendar was deleted in Google: make a new one, once
			containerId = container(target, desired, token, containerId);
			remote = target.create(token, containerId, action);
		}

		DBActionSync saved = row != null ? row : new DBActionSync();
		saved.setActionUuid(action.uuid());
		saved.setTarget(target.type());
		saved.setAccountUserUuid(desired.getUserUuid());
		saved.setContainerId(containerId);
		saved.setExternalId(remote.id());
		saved.setExternalEtag(remote.etag());
		recordBase(saved, action);
		syncs.save(saved);
	}

	/** What Google holds now: the base for telling the next change in Google apart from our own echo. */
	private static void recordBase(DBActionSync row, ActionSnapshot action) {
		row.setSyncedCompleted(action.completed());
		row.setSyncedDue(action.dueDate());
		row.setDateSynced(LocalDateTime.now());
	}

	/**
	 * The account's list/calendar id, creating it on first use. Serialized per account
	 * and target, so parallel jobs right after connecting do not create several.
	 *
	 * @param staleId an id that just turned out to be deleted, or null
	 */
	private String container(ActionSyncTarget target, DBGoogleAccount account, String token, String staleId) {
		ReentrantLock lock = containerLocks.computeIfAbsent(account.getUserUuid() + ":" + target.type(),
				k -> new ReentrantLock());
		lock.lock();
		try {
			String current = accounts.findByUserUuid(account.getUserUuid()).map(target::containerId).orElse(null);
			if (current != null && !current.equals(staleId)) {
				return current;
			}
			String created = target.createContainer(token);
			if (target.type() == ESyncTarget.GOOGLE_TASKS) {
				accounts.updateTaskListId(account.getUserUuid(), created);
			} else {
				accounts.updateCalendarId(account.getUserUuid(), created);
			}
			log.info("{} container for user {} is now {}", target.label(), account.getUserUuid(), created);
			return created;
		} finally {
			lock.unlock();
		}
	}
}
