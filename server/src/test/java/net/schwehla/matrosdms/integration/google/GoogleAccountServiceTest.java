/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.entity.DBGoogleAccount;
import net.schwehla.matrosdms.integration.google.GoogleOAuthClient.TokenResponse;
import net.schwehla.matrosdms.repository.ActionSyncRepository;
import net.schwehla.matrosdms.repository.GoogleAccountRepository;

/** Which permissions a sign-in asks for, and which switches it turns on. */
class GoogleAccountServiceTest {

	private static final String USER = "U1";
	private static final String TASKS_ONLY = "openid email " + GoogleOAuthClient.SCOPE_TASKS;
	private static final String TASKS_AND_CALENDAR = TASKS_ONLY + " " + GoogleOAuthClient.SCOPE_CALENDAR;

	private GoogleOAuthClient oauth;
	private GoogleAccountService service;
	private final AtomicReference<DBGoogleAccount> stored = new AtomicReference<>();

	@BeforeEach
	void setUp() {
		AppServerSpringConfig config = new AppServerSpringConfig();
		config.getIntegrations().getGoogle().setClientId("id");
		config.getIntegrations().getGoogle().setClientSecret("secret");

		oauth = mock(GoogleOAuthClient.class);
		when(oauth.authorizationUrl(anyString(), anyString(), anyString(), anyBoolean())).thenReturn("https://consent");
		when(oauth.emailFromIdToken(any())).thenReturn("me@example.com");

		GoogleTokenCipher cipher = mock(GoogleTokenCipher.class);
		when(cipher.encrypt(any())).thenReturn("v1:encrypted");

		GoogleAccountRepository accounts = mock(GoogleAccountRepository.class);
		when(accounts.findByUserUuid(USER)).thenAnswer(i -> Optional.ofNullable(stored.get()));
		when(accounts.save(any())).thenAnswer(i -> {
			DBGoogleAccount a = i.getArgument(0);
			if (a.getId() == null) {
				ReflectionTestUtils.setField(a, "id", 1L);
			}
			stored.set(a);
			return a;
		});

		PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
		when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));

		service = new GoogleAccountService(config, oauth, cipher, mock(GoogleTokenService.class), accounts,
				mock(ActionSyncRepository.class), mock(ActionSyncDispatcher.class), tx);
	}

	/** Starts a sign-in and lets Google answer with {@code grantedScopes}; returns whether the calendar was asked for. */
	private boolean signIn(boolean calendar, String grantedScopes) {
		service.startAuthorization(USER, "http://localhost:9099/api/integrations/google/callback", calendar);
		ArgumentCaptor<String> state = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Boolean> askedCalendar = ArgumentCaptor.forClass(Boolean.class);
		verify(oauth, org.mockito.Mockito.atLeastOnce())
				.authorizationUrl(anyString(), state.capture(), anyString(), askedCalendar.capture());
		when(oauth.exchangeCode(eq("code"), anyString(), anyString()))
				.thenReturn(new TokenResponse("access", 3600, "refresh", grantedScopes, "id-token"));
		service.completeAuthorization(state.getValue(), "code");
		return askedCalendar.getValue();
	}

	@Test
	void connectingAsksForTasksOnlyAndLeavesTheCalendarOff() {
		boolean askedCalendar = signIn(false, TASKS_ONLY);

		assertThat(askedCalendar).isFalse();
		assertThat(stored.get().isTasksEnabled()).isTrue();
		assertThat(stored.get().isCalendarEnabled()).isFalse();
	}

	@Test
	void calendarGrantedEarlierStaysOffUntilTheUserAsks() {
		// include_granted_scopes: Google may hand back a calendar grant from an earlier sign-in
		signIn(false, TASKS_AND_CALENDAR);

		assertThat(stored.get().isCalendarEnabled()).isFalse();
	}

	@Test
	void switchingTheCalendarOnAsksGoogleAndTurnsItOn() {
		signIn(false, TASKS_ONLY);

		boolean askedCalendar = signIn(true, TASKS_AND_CALENDAR);

		assertThat(askedCalendar).isTrue();
		assertThat(stored.get().isTasksEnabled()).isTrue();
		assertThat(stored.get().isCalendarEnabled()).isTrue();
	}

	@Test
	void calendarStaysOffWhenTheUserDeclinesItOnGooglesPage() {
		signIn(false, TASKS_ONLY);

		signIn(true, TASKS_ONLY);

		assertThat(stored.get().isCalendarEnabled()).isFalse();
	}

	@Test
	void reconnectAsksForTheCalendarAgainWhenItWasOn() {
		signIn(true, TASKS_AND_CALENDAR);
		stored.get().setRefreshToken(null); // disconnected

		boolean askedCalendar = signIn(false, TASKS_AND_CALENDAR);

		assertThat(askedCalendar).isTrue();
		assertThat(stored.get().isCalendarEnabled()).isTrue();
	}

	@Test
	void reconnectKeepsTasksSwitchedOffWhenTheUserTurnedThemOff() {
		signIn(false, TASKS_ONLY);
		stored.get().setTasksEnabled(false);

		signIn(false, TASKS_ONLY);

		assertThat(stored.get().isTasksEnabled()).isFalse();
	}

	@Test
	void scopeStringsAskForTheCalendarOnlyWhenWanted() {
		assertThat(GoogleOAuthClient.scopes(false)).isEqualTo(TASKS_ONLY);
		assertThat(GoogleOAuthClient.scopes(true)).isEqualTo(TASKS_AND_CALENDAR);
	}
}
