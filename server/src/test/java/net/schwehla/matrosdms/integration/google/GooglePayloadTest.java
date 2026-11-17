package net.schwehla.matrosdms.integration.google;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.schwehla.matrosdms.integration.google.ActionSyncTarget.RemoteState;

import net.schwehla.matrosdms.domain.action.EActionPriority;
import net.schwehla.matrosdms.domain.action.EActionStatus;

class GooglePayloadTest {

	private final GoogleHttp http = new GoogleHttp();
	private final GoogleTasksTarget tasks = new GoogleTasksTarget(http);
	private final GoogleCalendarTarget calendar = new GoogleCalendarTarget(http);

	private static ActionSnapshot action(EActionStatus status, LocalDateTime due) {
		return new ActionSnapshot("ACTION000000001", "Pay invoice", "Bank transfer", status, EActionPriority.HIGH,
				due, "ALICE0000000001", "Invoice 4711.pdf", "Insurance", "Paid on time", null);
	}

	private JsonNode json(String s) throws Exception {
		return http.mapper().readTree(s);
	}

	@Test
	void taskTickedOffOnThePhoneIsReadAsCompleted() throws Exception {
		RemoteState s = GoogleTasksTarget.parse(json("""
				{"id":"t1","status":"completed","hidden":true,"due":"2026-11-20T00:00:00.000Z",
				 "updated":"2026-11-17T08:15:30.123Z","completed":"2026-11-17T08:15:30.000Z"}
				"""));
		assertThat(s.completed()).isTrue();
		assertThat(s.gone()).isFalse();
		assertThat(s.due()).isEqualTo(LocalDateTime.of(2026, 11, 20, 0, 0));
		assertThat(s.updated()).isEqualTo(Instant.parse("2026-11-17T08:15:30.123Z"));
	}

	@Test
	void deletedTaskIsGoneAndOpenTaskWithoutDueHasNone() throws Exception {
		assertThat(GoogleTasksTarget.parse(json("{\"id\":\"t1\",\"deleted\":true,\"status\":\"needsAction\"}")).gone())
				.isTrue();
		RemoteState open = GoogleTasksTarget.parse(json("{\"id\":\"t2\",\"status\":\"needsAction\"}"));
		assertThat(open.completed()).isFalse();
		assertThat(open.due()).isNull();
	}

	@Test
	void tasksCompareDatesOnlyAndKeepTheTimeOfDay() {
		LocalDateTime afternoon = LocalDateTime.of(2026, 11, 20, 13, 30);
		assertThat(tasks.sameDue(afternoon, LocalDateTime.of(2026, 11, 20, 0, 0))).isTrue();
		assertThat(tasks.sameDue(afternoon, null)).isFalse();
		assertThat(tasks.adoptDue(LocalDateTime.of(2026, 11, 25, 0, 0), afternoon))
				.isEqualTo(LocalDateTime.of(2026, 11, 25, 13, 30));
		assertThat(tasks.adoptDue(null, afternoon)).isNull();
	}

	@Test
	void calendarEventMovedReadsTheNewDate() throws Exception {
		RemoteState allDay = GoogleCalendarTarget.parse(json("""
				{"id":"e1","status":"confirmed","start":{"date":"2026-11-25"},"end":{"date":"2026-11-26"},
				 "updated":"2026-11-17T08:00:00.000Z"}
				"""));
		assertThat(allDay.due()).isEqualTo(LocalDateTime.of(2026, 11, 25, 0, 0));
		assertThat(allDay.completed()).isNull();

		RemoteState timed = GoogleCalendarTarget.parse(json("""
				{"id":"e2","status":"confirmed","start":{"dateTime":"2026-11-25T15:00:00+01:00"}}
				"""));
		assertThat(timed.due()).isEqualTo(LocalDateTime.of(2026, 11, 25, 14, 0));

		assertThat(GoogleCalendarTarget.parse(json("{\"id\":\"e3\",\"status\":\"cancelled\"}")).gone()).isTrue();
	}

	@Test
	void openTaskCarriesDateOnlyDueAndClearsCompletion() {
		ObjectNode body = tasks.body(action(EActionStatus.OPEN, LocalDateTime.of(2026, 11, 20, 0, 0)));

		assertThat(body.path("title").asText()).isEqualTo("Pay invoice");
		assertThat(body.path("due").asText()).isEqualTo("2026-11-20T00:00:00.000Z");
		assertThat(body.path("status").asText()).isEqualTo("needsAction");
		assertThat(body.has("completed")).isTrue();
		assertThat(body.path("completed").isNull()).isTrue();
		assertThat(body.path("notes").asText())
				.contains("Bank transfer", "Document: Invoice 4711.pdf", "Context: Insurance", "Priority: HIGH")
				.doesNotContain("Resolution");
	}

	@Test
	void removedDueDateIsSentAsNullSoPatchClearsIt() {
		ObjectNode body = tasks.body(action(EActionStatus.OPEN, null));
		assertThat(body.has("due")).isTrue();
		assertThat(body.path("due").isNull()).isTrue();
	}

	@Test
	void doneActionTicksTheTaskAndShowsResolution() {
		ObjectNode body = tasks.body(action(EActionStatus.DONE, null));
		assertThat(body.path("status").asText()).isEqualTo("completed");
		assertThat(body.path("notes").asText()).contains("Resolution: Paid on time");
	}

	@Test
	void tasksKeepAlreadySyncedFinishedActionsButDoNotPushOldOnes() {
		assertThat(tasks.wants(action(EActionStatus.OPEN, null), false)).isTrue();
		assertThat(tasks.wants(action(EActionStatus.DONE, null), false)).isFalse();
		assertThat(tasks.wants(action(EActionStatus.DONE, null), true)).isTrue();
	}

	@Test
	void calendarOnlyWantsOpenActionsWithDueDate() {
		LocalDateTime due = LocalDateTime.of(2026, 11, 20, 0, 0);
		assertThat(calendar.wants(action(EActionStatus.OPEN, due), false)).isTrue();
		assertThat(calendar.wants(action(EActionStatus.OPEN, null), false)).isFalse();
		assertThat(calendar.wants(action(EActionStatus.DONE, due), true)).isFalse();
		assertThat(calendar.wants(action(EActionStatus.REJECTED, due), true)).isFalse();
	}

	@Test
	void midnightDueDateBecomesAllDayEvent() {
		ObjectNode event = calendar.body(action(EActionStatus.OPEN, LocalDateTime.of(2026, 12, 31, 0, 0)));

		assertThat(event.path("start").path("date").asText()).isEqualTo("2026-12-31");
		assertThat(event.path("end").path("date").asText()).isEqualTo("2027-01-01");
		assertThat(event.path("start").has("dateTime")).isFalse();
		assertThat(event.path("status").asText()).isEqualTo("confirmed");
		assertThat(event.path("transparency").asText()).isEqualTo("transparent");
		assertThat(event.path("extendedProperties").path("private").path("matrosActionUuid").asText())
				.isEqualTo("ACTION000000001");
	}

	@Test
	void dueDateWithTimeBecomesOneHourUtcEvent() {
		ObjectNode event = calendar.body(action(EActionStatus.OPEN, LocalDateTime.of(2026, 11, 20, 13, 30)));

		assertThat(event.path("start").path("dateTime").asText()).isEqualTo("2026-11-20T13:30:00Z");
		assertThat(event.path("end").path("dateTime").asText()).isEqualTo("2026-11-20T14:30:00Z");
	}

	@Test
	void longTextsAreTruncatedToGoogleLimits() {
		ActionSnapshot a = new ActionSnapshot("ACTION000000001", "x".repeat(2000), "y".repeat(10_000),
				EActionStatus.OPEN, EActionPriority.NORMAL, null, null, null, null, null, null);
		ObjectNode body = tasks.body(a);
		assertThat(body.path("title").asText()).hasSize(1024);
		assertThat(body.path("notes").asText()).hasSize(8192);
	}

	@Test
	void retryDelayDoublesFromThirtySecondsUpToOneHour() {
		assertThat(ActionSyncTaskConfig.retryDelay(1)).isEqualTo(Duration.ofSeconds(30));
		assertThat(ActionSyncTaskConfig.retryDelay(2)).isEqualTo(Duration.ofMinutes(1));
		assertThat(ActionSyncTaskConfig.retryDelay(5)).isEqualTo(Duration.ofMinutes(8));
		assertThat(ActionSyncTaskConfig.retryDelay(8)).isEqualTo(Duration.ofHours(1));
		assertThat(ActionSyncTaskConfig.retryDelay(59)).isEqualTo(Duration.ofHours(1));
	}
}
