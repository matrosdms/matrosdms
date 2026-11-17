package net.schwehla.matrosdms.integration.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.action.ESyncTarget;
import net.schwehla.matrosdms.entity.DBActionSync;
import net.schwehla.matrosdms.entity.DBGoogleAccount;
import net.schwehla.matrosdms.integration.google.ActionSyncTarget.RemoteState;
import net.schwehla.matrosdms.repository.ActionSyncRepository;
import net.schwehla.matrosdms.repository.GoogleAccountRepository;

class GooglePullServiceTest {

	private static final String ALICE = "ALICE0000000001";

	private GoogleAccountRepository accounts;
	private ActionSyncRepository syncs;
	private GoogleTasksTarget tasks;
	private ActionSyncEngine engine;
	private GooglePullService pull;
	private DBGoogleAccount alice;

	@BeforeEach
	void setUp() {
		accounts = mock(GoogleAccountRepository.class);
		syncs = mock(ActionSyncRepository.class);
		tasks = mock(GoogleTasksTarget.class);
		engine = mock(ActionSyncEngine.class);
		GoogleTokenService tokens = mock(GoogleTokenService.class);
		when(tokens.accessToken(any())).thenReturn("token");

		when(tasks.type()).thenReturn(ESyncTarget.GOOGLE_TASKS);
		when(tasks.isEnabled(any())).thenReturn(true);
		when(tasks.containerId(any())).thenReturn("list-1");
		when(tasks.sameDue(any(), any())).thenAnswer(inv -> java.util.Objects.equals(inv.getArgument(0), inv.getArgument(1)));

		alice = new DBGoogleAccount();
		alice.setUserUuid(ALICE);
		alice.setRefreshToken("v1:x");
		alice.setLastPulledAt(LocalDateTime.of(2026, 11, 17, 8, 0));

		AppServerSpringConfig config = new AppServerSpringConfig();
		config.getIntegrations().getGoogle().setClientId("id");
		config.getIntegrations().getGoogle().setClientSecret("secret");

		pull = new GooglePullService(config, accounts, syncs, List.of(tasks), tokens, engine);
	}

	private static DBActionSync row(String actionUuid, String externalId, boolean completed) {
		DBActionSync r = new DBActionSync();
		r.setActionUuid(actionUuid);
		r.setTarget(ESyncTarget.GOOGLE_TASKS);
		r.setAccountUserUuid(ALICE);
		r.setExternalId(externalId);
		r.setSyncedCompleted(completed);
		return r;
	}

	@Test
	void onlyActionsChangedInGoogleAreReconciled() {
		when(syncs.findByAccountUserUuidAndTarget(ALICE, ESyncTarget.GOOGLE_TASKS)).thenReturn(List.of(
				row("TICKED", "t1", false),
				row("ECHO", "t2", false)));
		when(tasks.changedSince(eq("token"), eq("list-1"), any())).thenReturn(List.of(
				new RemoteState("t1", false, true, null, Instant.now()), // ticked off on the phone
				new RemoteState("t2", false, false, null, Instant.now()), // our own push
				new RemoteState("t9", false, true, null, Instant.now()))); // a task the user added by hand

		int changed = pull.poll(alice);

		assertThat(changed).isEqualTo(1);
		verify(engine).reconcile("TICKED");
		verify(engine, never()).reconcile("ECHO");
		verify(accounts).updateLastPulledAt(eq(ALICE), any(LocalDateTime.class));
	}

	@Test
	void failedTakeOverKeepsTheWindowOpenForTheNextPoll() {
		when(syncs.findByAccountUserUuidAndTarget(ALICE, ESyncTarget.GOOGLE_TASKS))
				.thenReturn(List.of(row("TICKED", "t1", false)));
		when(tasks.changedSince(anyString(), anyString(), any()))
				.thenReturn(List.of(new RemoteState("t1", false, true, null, Instant.now())));
		doThrow(new GoogleApiException(0, "offline", true)).when(engine).reconcile("TICKED");

		pull.poll(alice);

		verify(accounts, never()).updateLastPulledAt(anyString(), any());
	}

	@Test
	void pollWindowStartsAtTheLastPollWithOverlap() {
		Instant since = GooglePullService.since(alice, Instant.now());
		Instant lastPoll = alice.getLastPulledAt().atZone(ZoneId.systemDefault()).toInstant();
		assertThat(since).isEqualTo(lastPoll.minus(GooglePullService.OVERLAP));
	}

	@Test
	void nothingHappensWhileGoogleIsNotConfigured() {
		GooglePullService unconfigured = new GooglePullService(new AppServerSpringConfig(), accounts, syncs,
				List.of(tasks), mock(GoogleTokenService.class), engine);

		unconfigured.pollAll();

		verify(accounts, never()).findByRefreshTokenIsNotNull();
	}
}
