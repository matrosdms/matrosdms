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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.schwehla.matrosdms.domain.action.ESyncTarget;
import net.schwehla.matrosdms.entity.DBGoogleAccount;

/** Pushes actions into a "MatrosDMS" list in Google Tasks. */
@Component
@Order(1)
public class GoogleTasksTarget implements ActionSyncTarget {

	static final String BASE = "https://tasks.googleapis.com/tasks/v1";
	static final String LIST_TITLE = "MatrosDMS";

	private final GoogleHttp http;

	public GoogleTasksTarget(GoogleHttp http) {
		this.http = http;
	}

	@Override
	public ESyncTarget type() {
		return ESyncTarget.GOOGLE_TASKS;
	}

	@Override
	public boolean isEnabled(DBGoogleAccount account) {
		return account.isTasksEnabled() && account.hasScope(GoogleOAuthClient.SCOPE_TASKS);
	}

	/**
	 * Open actions get a task. A completed one keeps its task (ticked off) but is not
	 * pushed fresh, so connecting an account does not flood the list with history.
	 */
	@Override
	public boolean wants(ActionSnapshot action, boolean alreadySynced) {
		return alreadySynced || !action.completed();
	}

	@Override
	public String containerId(DBGoogleAccount account) {
		return account.getTaskListId();
	}

	/** Reuses a list called "MatrosDMS" (e.g. left over from an earlier install) before creating one. */
	@Override
	public String createContainer(String accessToken) {
		JsonNode lists = http.get(BASE + "/users/@me/lists?maxResults=100", accessToken);
		for (JsonNode list : lists.path("items")) {
			if (LIST_TITLE.equals(list.path("title").asText())) {
				return list.path("id").asText();
			}
		}
		ObjectNode body = http.object().put("title", LIST_TITLE);
		return http.send("POST", BASE + "/users/@me/lists", accessToken, body).path("id").asText();
	}

	@Override
	public Remote create(String accessToken, String listId, ActionSnapshot action) {
		JsonNode task = http.send("POST", BASE + "/lists/" + encode(listId) + "/tasks", accessToken, body(action));
		return new Remote(task.path("id").asText(), task.path("etag").asText(null));
	}

	@Override
	public String update(String accessToken, String listId, String taskId, ActionSnapshot action) {
		String url = BASE + "/lists/" + encode(listId) + "/tasks/" + encode(taskId);
		return http.send("PATCH", url, accessToken, body(action)).path("etag").asText(null);
	}

	@Override
	public void delete(String accessToken, String listId, String taskId) {
		http.delete(BASE + "/lists/" + encode(listId) + "/tasks/" + encode(taskId), accessToken);
	}

	@Override
	public String label() {
		return "Google Tasks";
	}

	@Override
	public RemoteState fetch(String accessToken, String listId, String taskId) {
		try {
			return parse(http.get(BASE + "/lists/" + encode(listId) + "/tasks/" + encode(taskId), accessToken));
		} catch (GoogleApiException e) {
			if (e.isNotFound()) {
				return new RemoteState(taskId, true, null, null, null);
			}
			throw e;
		}
	}

	/**
	 * showHidden is required: tasks ticked off in Google's own apps are "hidden" and
	 * would otherwise never show up - exactly the change we are looking for.
	 */
	@Override
	public List<RemoteState> changedSince(String accessToken, String listId, Instant since) {
		List<RemoteState> changed = new ArrayList<>();
		String pageToken = null;
		do {
			String url = BASE + "/lists/" + encode(listId) + "/tasks?showCompleted=true&showHidden=true&showDeleted=true"
					+ "&maxResults=100&updatedMin=" + encode(since.truncatedTo(ChronoUnit.SECONDS).toString())
					+ (pageToken != null ? "&pageToken=" + encode(pageToken) : "");
			JsonNode page = http.get(url, accessToken);
			for (JsonNode task : page.path("items")) {
				changed.add(parse(task));
			}
			pageToken = page.path("nextPageToken").asText(null);
		} while (pageToken != null);
		return changed;
	}

	@Override
	public boolean sameDue(LocalDateTime a, LocalDateTime b) {
		return a == null || b == null ? a == b : a.toLocalDate().equals(b.toLocalDate());
	}

	@Override
	public LocalDateTime adoptDue(LocalDateTime remoteDue, LocalDateTime currentDue) {
		if (remoteDue == null) {
			return null;
		}
		return currentDue != null ? remoteDue.toLocalDate().atTime(currentDue.toLocalTime()) : remoteDue;
	}

	static RemoteState parse(JsonNode task) {
		String due = task.path("due").asText(null);
		String updated = task.path("updated").asText(null);
		return new RemoteState(
				task.path("id").asText(),
				task.path("deleted").asBoolean(false),
				"completed".equals(task.path("status").asText()),
				due == null || due.isBlank() ? null : OffsetDateTime.parse(due).toLocalDate().atStartOfDay(),
				updated == null || updated.isBlank() ? null : OffsetDateTime.parse(updated).toInstant());
	}

	/**
	 * Every field is always sent, nulls included, so a PATCH also clears a removed due
	 * date and un-ticks a reopened action.
	 */
	ObjectNode body(ActionSnapshot a) {
		ObjectNode task = http.object();
		task.put("title", ActionSnapshot.truncate(a.name(), 1024));
		task.put("notes", ActionSnapshot.truncate(a.notes(), 8192));
		if (a.dueDate() != null) {
			// Google Tasks keeps only the date part of "due"
			task.put("due", a.dueDate().toLocalDate() + "T00:00:00.000Z");
		} else {
			task.putNull("due");
		}
		if (a.completed()) {
			task.put("status", "completed");
		} else {
			task.put("status", "needsAction");
			task.putNull("completed");
		}
		return task;
	}
}
