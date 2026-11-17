package net.schwehla.matrosdms.integration.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;

import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.domain.action.ESyncTarget;
import net.schwehla.matrosdms.entity.DBAction;
import net.schwehla.matrosdms.entity.DBActionSync;
import net.schwehla.matrosdms.entity.DBGoogleAccount;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.integration.google.ActionSyncTarget.RemoteState;
import net.schwehla.matrosdms.repository.ActionRepository;
import net.schwehla.matrosdms.repository.ActionSyncRepository;
import net.schwehla.matrosdms.repository.GoogleAccountRepository;
import net.schwehla.matrosdms.service.domain.ActionRemoteChangeService;
import net.schwehla.matrosdms.service.domain.ActionRemoteChangeService.RemoteChange;

class ActionSyncEngineTest {

	private static final String ACTION = "ACTION000000001";
	private static final String ALICE = "ALICE0000000001";
	private static final String BOB = "BOB000000000001";
	private static final LocalDateTime NOV_20 = LocalDateTime.of(2026, 11, 20, 0, 0);

	private ActionRepository actions;
	private GoogleAccountRepository accounts;
	private ActionSyncRepository syncs;
	private GoogleTokenService tokens;
	private ActionRemoteChangeService remoteChanges;
	private FakeTarget target;
	private ActionSyncEngine engine;

	private final List<DBGoogleAccount> knownAccounts = new ArrayList<>();
	private final List<DBActionSync> knownRows = new ArrayList<>();
	private DBAction action;

	@BeforeEach
	void setUp() {
		actions = mock(ActionRepository.class);
		accounts = mock(GoogleAccountRepository.class);
		syncs = mock(ActionSyncRepository.class);
		tokens = mock(GoogleTokenService.class);
		remoteChanges = mock(ActionRemoteChangeService.class);
		target = new FakeTarget();

		when(tokens.accessToken(any())).thenReturn("token");
		when(actions.findByUuid(anyString())).thenAnswer(inv -> Optional.ofNullable(action));
		when(syncs.findByActionUuid(ACTION)).thenAnswer(inv -> knownRows);
		when(accounts.findByUserUuidIn(anyCollection())).thenAnswer(inv -> {
			Collection<?> wanted = inv.getArgument(0);
			return knownAccounts.stream().filter(a -> wanted.contains(a.getUserUuid())).toList();
		});
		when(accounts.findByUserUuid(anyString())).thenAnswer(inv -> knownAccounts.stream()
				.filter(a -> a.getUserUuid().equals(inv.getArgument(0))).findFirst());
		// Applying a remote change really changes the action, like the real service
		doAnswer(inv -> {
			RemoteChange change = inv.getArgument(1);
			if (change.status() != null) {
				action.setStatus(change.status());
			}
			if (change.dueChanged()) {
				action.setDueDate(change.due());
			}
			return null;
		}).when(remoteChanges).apply(anyString(), any(), anyString());

		engine = new ActionSyncEngine(actions, accounts, syncs, List.of(target), tokens, remoteChanges,
				TransactionOperations.withoutTransaction());
	}

	// --- push ---

	@Test
	void pushesNewOpenActionToAssignee() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		action.setDueDate(NOV_20);

		engine.reconcile(ACTION);

		assertThat(target.calls).containsExactly("create list-ALICE");
		DBActionSync saved = savedRow();
		assertThat(saved.getAccountUserUuid()).isEqualTo(ALICE);
		assertThat(saved.getContainerId()).isEqualTo("list-ALICE");
		assertThat(saved.getExternalId()).isEqualTo("remote-1");
		assertThat(saved.getTarget()).isEqualTo(ESyncTarget.GOOGLE_TASKS);
		// the base for recognising later changes in Google
		assertThat(saved.getSyncedCompleted()).isFalse();
		assertThat(saved.getSyncedDue()).isEqualTo(NOV_20);
	}

	@Test
	void updatesExistingCopy() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		row(ALICE, "task-7");

		engine.reconcile(ACTION);

		assertThat(target.calls).containsExactly("update list-ALICE task-7");
		assertThat(savedRow().getExternalEtag()).isEqualTo("etag-2");
	}

	@Test
	void deletesRemoteCopyOfDeletedAction() {
		account(ALICE, true);
		DBActionSync row = row(ALICE, "task-7");

		engine.reconcile(ACTION);

		assertThat(target.calls).containsExactly("delete list-ALICE task-7");
		verify(syncs).delete(row);
	}

	@Test
	void reassignmentToUserWithoutGoogleRemovesTheOldCopy() {
		account(ALICE, true);
		action(BOB, EActionStatus.OPEN);
		DBActionSync row = row(ALICE, "task-7");

		engine.reconcile(ACTION);

		assertThat(target.calls).containsExactly("delete list-ALICE task-7");
		verify(syncs).delete(row);
		verify(syncs, never()).save(any());
	}

	@Test
	void reassignmentBetweenConnectedUsersMovesTheCopy() {
		account(ALICE, true);
		account(BOB, true);
		action(BOB, EActionStatus.OPEN);
		row(ALICE, "task-7");

		engine.reconcile(ACTION);

		assertThat(target.calls).containsExactly("delete list-ALICE task-7", "create list-BOB");
		assertThat(savedRow().getAccountUserUuid()).isEqualTo(BOB);
	}

	@Test
	void disconnectedAccountIsLeftAloneUntilReconnect() {
		account(ALICE, false);
		DBActionSync row = row(ALICE, "task-7");

		engine.reconcile(ACTION);

		assertThat(target.calls).isEmpty();
		verify(syncs, never()).delete(row);
	}

	@Test
	void switchedOffTargetRemovesTheCopy() {
		DBGoogleAccount alice = account(ALICE, true);
		alice.setTasksEnabled(false);
		action(ALICE, EActionStatus.OPEN);
		row(ALICE, "task-7");

		engine.reconcile(ACTION);

		assertThat(target.calls).containsExactly("delete list-ALICE task-7");
	}

	@Test
	void copyDeletedInGoogleIsRecreated() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		row(ALICE, "task-7");
		target.failUpdate = new GoogleApiException(404, "gone", false);

		engine.reconcile(ACTION);

		assertThat(target.calls).containsExactly("update list-ALICE task-7", "create list-ALICE");
		assertThat(savedRow().getExternalId()).isEqualTo("remote-1");
	}

	@Test
	void deletedListIsReplacedOnce() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		target.failCreateOnce = new GoogleApiException(404, "list gone", false);

		engine.reconcile(ACTION);

		assertThat(target.calls).containsExactly("create list-ALICE", "createContainer", "create new-list");
		verify(accounts).updateTaskListId(ALICE, "new-list");
		assertThat(savedRow().getContainerId()).isEqualTo("new-list");
	}

	@Test
	void firstSyncCreatesTheList() {
		DBGoogleAccount alice = account(ALICE, true);
		alice.setTaskListId(null);
		action(ALICE, EActionStatus.OPEN);

		engine.reconcile(ACTION);

		assertThat(target.calls).containsExactly("createContainer", "create new-list");
		verify(accounts).updateTaskListId(ALICE, "new-list");
	}

	@Test
	void retryableFailureIsRecordedAndRethrown() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		target.failCreate = new GoogleApiException(503, "Google API 503: backend", true);

		assertThatThrownBy(() -> engine.reconcile(ACTION)).isInstanceOf(GoogleApiException.class);
		verify(accounts).updateLastError(eq(ALICE), contains("503"), any(LocalDateTime.class));
	}

	@Test
	void permanentFailureIsRecordedButNotRetried() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		target.failCreate = new GoogleApiException(403, "Google API 403: Tasks API has not been used", false);

		engine.reconcile(ACTION);

		verify(accounts).updateLastError(eq(ALICE), contains("Tasks API"), any(LocalDateTime.class));
	}

	@Test
	void successClearsAnOldError() {
		DBGoogleAccount alice = account(ALICE, true);
		alice.setLastError("Google Tasks: earlier failure");
		action(ALICE, EActionStatus.OPEN);

		engine.reconcile(ACTION);

		verify(accounts).updateLastError(eq(ALICE), isNull(), isNull());
	}

	@Test
	void actionNobodySyncsCausesNoCalls() {
		action(ALICE, EActionStatus.OPEN);

		engine.reconcile(ACTION);

		assertThat(target.calls).isEmpty();
		verify(tokens, never()).accessToken(any());
	}

	@Test
	void rowWithoutBaseIsNotPulled() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		row(ALICE, "task-7");

		engine.reconcile(ACTION);

		assertThat(target.fetches).isZero();
	}

	// --- pull ---

	@Test
	void taskTickedOffInGoogleCompletesTheAction() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		withBase(row(ALICE, "task-7"), false, null);
		target.remote = remote(true, null, Instant.now());

		engine.reconcile(ACTION);

		RemoteChange change = appliedChange("Google Tasks");
		assertThat(change.status()).isEqualTo(EActionStatus.DONE);
		assertThat(change.dueChanged()).isFalse();
		assertThat(change.log()).containsExactly("Completed in Google Tasks");
		// then the merged state is pushed and becomes the new base
		assertThat(target.calls).containsExactly("fetch task-7", "update list-ALICE task-7");
		assertThat(savedRow().getSyncedCompleted()).isTrue();
	}

	@Test
	void taskUntickedInGoogleReopensTheAction() {
		account(ALICE, true);
		action(ALICE, EActionStatus.DONE);
		withBase(row(ALICE, "task-7"), true, null);
		target.remote = remote(false, null, Instant.now());

		engine.reconcile(ACTION);

		assertThat(appliedChange("Google Tasks").status()).isEqualTo(EActionStatus.OPEN);
	}

	@Test
	void echoOfOwnPushIsNotAChange() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		action.setDueDate(NOV_20);
		withBase(row(ALICE, "task-7"), false, NOV_20);
		target.remote = remote(false, NOV_20, Instant.now());

		engine.reconcile(ACTION);

		verify(remoteChanges, never()).apply(anyString(), any(), anyString());
	}

	@Test
	void dueDateMovedInGoogleKeepsTheTimeOfDay() {
		LocalDateTime due = LocalDateTime.of(2026, 11, 20, 13, 30);
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		action.setDueDate(due);
		withBase(row(ALICE, "task-7"), false, due);
		target.remote = remote(false, LocalDateTime.of(2026, 11, 25, 0, 0), Instant.now());

		engine.reconcile(ACTION);

		RemoteChange change = appliedChange("Google Tasks");
		assertThat(change.dueChanged()).isTrue();
		assertThat(change.due()).isEqualTo(LocalDateTime.of(2026, 11, 25, 13, 30));
		assertThat(change.log()).containsExactly("Due date set to 2026-11-25 in Google Tasks");
	}

	@Test
	void dueDateRemovedInGoogleIsRemovedHere() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		action.setDueDate(NOV_20);
		withBase(row(ALICE, "task-7"), false, NOV_20);
		target.remote = remote(false, null, Instant.now());

		engine.reconcile(ACTION);

		RemoteChange change = appliedChange("Google Tasks");
		assertThat(change.dueChanged()).isTrue();
		assertThat(change.due()).isNull();
	}

	@Test
	void bothSidesChangedTheDueDateAndMatrosWasLater() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		action.setDueDate(LocalDateTime.of(2026, 11, 22, 0, 0)); // changed here just now
		withBase(row(ALICE, "task-7"), false, NOV_20);
		target.remote = remote(false, LocalDateTime.of(2026, 11, 25, 0, 0), Instant.now().minus(1, ChronoUnit.HOURS));

		engine.reconcile(ACTION);

		verify(remoteChanges, never()).apply(anyString(), any(), anyString());
		assertThat(target.calls).contains("update list-ALICE task-7");
	}

	@Test
	void bothSidesChangedTheDueDateAndGoogleWasLater() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		action.setDueDate(LocalDateTime.of(2026, 11, 22, 0, 0));
		setDateUpdated(action, LocalDateTime.now().minusHours(1));
		withBase(row(ALICE, "task-7"), false, NOV_20);
		target.remote = remote(false, LocalDateTime.of(2026, 11, 25, 0, 0), Instant.now());

		engine.reconcile(ACTION);

		assertThat(appliedChange("Google Tasks").due()).isEqualTo(LocalDateTime.of(2026, 11, 25, 0, 0));
	}

	@Test
	void unrelatedChangeHereDoesNotBlockTakingOverCompletion() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN); // e.g. description edited just now
		withBase(row(ALICE, "task-7"), false, null);
		target.remote = remote(true, null, Instant.now().minus(1, ChronoUnit.HOURS));

		engine.reconcile(ACTION);

		assertThat(appliedChange("Google Tasks").status()).isEqualTo(EActionStatus.DONE);
	}

	@Test
	void taskDeletedInGoogleIsRecreatedAndTheActionKept() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		withBase(row(ALICE, "task-7"), false, null);
		target.remote = new RemoteState("task-7", true, null, null, null);

		engine.reconcile(ACTION);

		verify(remoteChanges, never()).apply(anyString(), any(), anyString());
		assertThat(target.calls).containsExactly("fetch task-7", "create list-ALICE");
		assertThat(savedRow().getExternalId()).isEqualTo("remote-1");
	}

	@Test
	void unreadableCopyIsNeverOverwritten() {
		account(ALICE, true);
		action(ALICE, EActionStatus.OPEN);
		withBase(row(ALICE, "task-7"), false, null);
		target.failFetch = new GoogleApiException(503, "Google API 503", true);

		assertThatThrownBy(() -> engine.reconcile(ACTION)).isInstanceOf(GoogleApiException.class);
		assertThat(target.calls).containsExactly("fetch task-7");
	}

	// --- fixtures ---

	private DBGoogleAccount account(String userUuid, boolean connected) {
		DBGoogleAccount a = new DBGoogleAccount();
		a.setUserUuid(userUuid);
		a.setRefreshToken(connected ? "v1:secret" : null);
		a.setGrantedScopes(GoogleOAuthClient.SCOPE_TASKS);
		a.setTasksEnabled(true);
		a.setTaskListId("list-" + userUuid.replaceAll("0+1$", ""));
		knownAccounts.add(a);
		return a;
	}

	private void action(String assigneeUuid, EActionStatus status) {
		DBUser assignee = new DBUser();
		assignee.setUuid(assigneeUuid);
		action = new DBAction();
		action.setUuid(ACTION);
		action.setName("Pay invoice");
		action.setStatus(status);
		action.setAssignee(assignee);
		action.updateDates(); // dateUpdated = now, as after a save
	}

	private static void setDateUpdated(DBAction a, LocalDateTime when) {
		try {
			var field = net.schwehla.matrosdms.entity.AbstractDBInfoBaseEntity.class.getDeclaredField("dateUpdated");
			field.setAccessible(true);
			field.set(a, when);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private DBActionSync row(String accountUserUuid, String externalId) {
		DBActionSync r = new DBActionSync();
		r.setActionUuid(ACTION);
		r.setTarget(ESyncTarget.GOOGLE_TASKS);
		r.setAccountUserUuid(accountUserUuid);
		r.setContainerId("list-" + accountUserUuid.replaceAll("0+1$", ""));
		r.setExternalId(externalId);
		knownRows.add(r);
		return r;
	}

	private static DBActionSync withBase(DBActionSync row, boolean completed, LocalDateTime due) {
		row.setSyncedCompleted(completed);
		row.setSyncedDue(due);
		return row;
	}

	private static RemoteState remote(boolean completed, LocalDateTime due, Instant updated) {
		return new RemoteState("task-7", false, completed, due, updated);
	}

	private RemoteChange appliedChange(String source) {
		ArgumentCaptor<RemoteChange> captor = ArgumentCaptor.forClass(RemoteChange.class);
		verify(remoteChanges).apply(eq(ACTION), captor.capture(), eq(source));
		return captor.getValue();
	}

	private DBActionSync savedRow() {
		ArgumentCaptor<DBActionSync> captor = ArgumentCaptor.forClass(DBActionSync.class);
		verify(syncs).save(captor.capture());
		return captor.getValue();
	}

	/** Records calls; behaves like the Tasks target (open actions, or already synced ones). */
	private static final class FakeTarget implements ActionSyncTarget {
		final List<String> calls = new ArrayList<>();
		final GoogleTasksTarget dueRules = new GoogleTasksTarget(null);
		RemoteState remote;
		int fetches;
		GoogleApiException failCreate;
		GoogleApiException failCreateOnce;
		GoogleApiException failUpdate;
		GoogleApiException failFetch;

		@Override
		public ESyncTarget type() {
			return ESyncTarget.GOOGLE_TASKS;
		}

		@Override
		public String label() {
			return "Google Tasks";
		}

		@Override
		public boolean isEnabled(DBGoogleAccount account) {
			return account.isTasksEnabled();
		}

		@Override
		public boolean wants(ActionSnapshot action, boolean alreadySynced) {
			return alreadySynced || !action.completed();
		}

		@Override
		public String containerId(DBGoogleAccount account) {
			return account.getTaskListId();
		}

		@Override
		public String createContainer(String accessToken) {
			calls.add("createContainer");
			return "new-list";
		}

		@Override
		public Remote create(String accessToken, String containerId, ActionSnapshot action) {
			calls.add("create " + containerId);
			if (failCreateOnce != null) {
				GoogleApiException e = failCreateOnce;
				failCreateOnce = null;
				throw e;
			}
			if (failCreate != null) {
				throw failCreate;
			}
			return new Remote("remote-1", "etag-1");
		}

		@Override
		public String update(String accessToken, String containerId, String externalId, ActionSnapshot action) {
			calls.add("update " + containerId + " " + externalId);
			if (failUpdate != null) {
				throw failUpdate;
			}
			return "etag-2";
		}

		@Override
		public void delete(String accessToken, String containerId, String externalId) {
			calls.add("delete " + containerId + " " + externalId);
		}

		@Override
		public RemoteState fetch(String accessToken, String containerId, String externalId) {
			fetches++;
			calls.add("fetch " + externalId);
			if (failFetch != null) {
				throw failFetch;
			}
			return remote;
		}

		@Override
		public List<RemoteState> changedSince(String accessToken, String containerId, Instant since) {
			return List.of();
		}

		@Override
		public boolean sameDue(LocalDateTime a, LocalDateTime b) {
			return dueRules.sameDue(a, b);
		}

		@Override
		public LocalDateTime adoptDue(LocalDateTime remoteDue, LocalDateTime currentDue) {
			return dueRules.adoptDue(remoteDue, currentDue);
		}
	}
}
