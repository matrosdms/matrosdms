/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import static net.schwehla.matrosdms.integration.google.GoogleHttp.encode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.schwehla.matrosdms.domain.action.ESyncTarget;
import net.schwehla.matrosdms.entity.DBGoogleAccount;

/**
 * Pushes actions with a due date as events into a dedicated "MatrosDMS" calendar.
 * A calendar of its own can be hidden, recoloured or deleted in one go, and the
 * {@code calendar.app.created} scope gives MatrosDMS no access to any other calendar.
 */
@Component
@Order(2)
public class GoogleCalendarTarget implements ActionSyncTarget {

	static final String BASE = "https://www.googleapis.com/calendar/v3";
	static final String CALENDAR_NAME = "MatrosDMS";

	private final GoogleHttp http;

	public GoogleCalendarTarget(GoogleHttp http) {
		this.http = http;
	}

	@Override
	public ESyncTarget type() {
		return ESyncTarget.GOOGLE_CALENDAR;
	}

	@Override
	public boolean isEnabled(DBGoogleAccount account) {
		return account.isCalendarEnabled() && account.hasScope(GoogleOAuthClient.SCOPE_CALENDAR);
	}

	/** Same rule as the iCal feed: open actions with a due date. A finished one leaves the calendar. */
	@Override
	public boolean wants(ActionSnapshot action, boolean alreadySynced) {
		return action.dueDate() != null && !action.completed();
	}

	@Override
	public String containerId(DBGoogleAccount account) {
		return account.getCalendarId();
	}

	@Override
	public String createContainer(String accessToken) {
		ObjectNode body = http.object()
				.put("summary", CALENDAR_NAME)
				.put("description", "Due dates of MatrosDMS tasks. Managed by MatrosDMS - changes here are overwritten.");
		return http.send("POST", BASE + "/calendars", accessToken, body).path("id").asText();
	}

	@Override
	public Remote create(String accessToken, String calendarId, ActionSnapshot action) {
		JsonNode event = http.send("POST", BASE + "/calendars/" + encode(calendarId) + "/events", accessToken,
				body(action));
		return new Remote(event.path("id").asText(), event.path("etag").asText(null));
	}

	@Override
	public String update(String accessToken, String calendarId, String eventId, ActionSnapshot action) {
		String url = BASE + "/calendars/" + encode(calendarId) + "/events/" + encode(eventId);
		return http.send("PATCH", url, accessToken, body(action)).path("etag").asText(null);
	}

	@Override
	public void delete(String accessToken, String calendarId, String eventId) {
		http.delete(BASE + "/calendars/" + encode(calendarId) + "/events/" + encode(eventId), accessToken);
	}

	@Override
	public String label() {
		return "Google Calendar";
	}

	@Override
	public RemoteState fetch(String accessToken, String calendarId, String eventId) {
		try {
			return parse(http.get(BASE + "/calendars/" + encode(calendarId) + "/events/" + encode(eventId), accessToken));
		} catch (GoogleApiException e) {
			if (e.isNotFound()) {
				return new RemoteState(eventId, true, null, null, null);
			}
			throw e;
		}
	}

	@Override
	public List<RemoteState> changedSince(String accessToken, String calendarId, Instant since) {
		List<RemoteState> changed = new ArrayList<>();
		String pageToken = null;
		do {
			String url = BASE + "/calendars/" + encode(calendarId) + "/events?showDeleted=true&maxResults=250"
					+ "&updatedMin=" + encode(since.truncatedTo(ChronoUnit.SECONDS).toString())
					+ (pageToken != null ? "&pageToken=" + encode(pageToken) : "");
			JsonNode page = http.get(url, accessToken);
			for (JsonNode event : page.path("items")) {
				changed.add(parse(event));
			}
			pageToken = page.path("nextPageToken").asText(null);
		} while (pageToken != null);
		return changed;
	}

	@Override
	public boolean sameDue(LocalDateTime a, LocalDateTime b) {
		return Objects.equals(a, b);
	}

	@Override
	public LocalDateTime adoptDue(LocalDateTime remoteDue, LocalDateTime currentDue) {
		return remoteDue;
	}

	/** An all-day event maps to midnight UTC, a timed one to its start in UTC. */
	static RemoteState parse(JsonNode event) {
		JsonNode start = event.path("start");
		LocalDateTime due = null;
		if (start.hasNonNull("date")) {
			due = LocalDate.parse(start.path("date").asText()).atStartOfDay();
		} else if (start.hasNonNull("dateTime")) {
			due = OffsetDateTime.parse(start.path("dateTime").asText())
					.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
		}
		String updated = event.path("updated").asText(null);
		return new RemoteState(
				event.path("id").asText(),
				"cancelled".equals(event.path("status").asText()),
				null,
				due,
				updated == null || updated.isBlank() ? null : OffsetDateTime.parse(updated).toInstant());
	}

	/**
	 * A due date at midnight (what the date-only input produces) becomes an all-day
	 * event; any other time a one-hour event at that UTC time.
	 */
	ObjectNode body(ActionSnapshot a) {
		ObjectNode event = http.object();
		event.put("summary", ActionSnapshot.truncate(a.name(), 1024));
		event.put("description", ActionSnapshot.truncate(a.notes(), 8192));

		LocalDateTime due = a.dueDate();
		ObjectNode start = event.putObject("start");
		ObjectNode end = event.putObject("end");
		if (due.toLocalTime().equals(LocalTime.MIDNIGHT)) {
			start.put("date", due.toLocalDate().toString());
			end.put("date", due.toLocalDate().plusDays(1).toString());
		} else {
			start.put("dateTime", due.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			end.put("dateTime", due.plusHours(1).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		}

		// "confirmed" also restores an event that was deleted in Google meanwhile
		event.put("status", "confirmed");
		// a deadline marker, not a meeting: do not block free/busy time
		event.put("transparency", "transparent");
		event.putObject("extendedProperties").putObject("private").put("matrosActionUuid", a.uuid());
		return event;
	}
}
