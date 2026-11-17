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
import java.util.List;

import net.schwehla.matrosdms.domain.action.ESyncTarget;
import net.schwehla.matrosdms.entity.DBGoogleAccount;

/**
 * One remote system that actions are pushed to. The engine decides what has to
 * happen; a target only knows how to talk to its API. Every call may throw
 * {@link GoogleApiException}.
 */
public interface ActionSyncTarget {

	record Remote(String id, String etag) {
	}

	/**
	 * What a remote copy looks like now.
	 *
	 * @param gone deleted in Google (deletions are never pulled into MatrosDMS)
	 * @param completed ticked off; null when the target has no such notion (calendar)
	 * @param due UTC wall time, like the action's due date; null = none
	 * @param updated last modification in Google, for "later change wins"
	 */
	record RemoteState(String id, boolean gone, Boolean completed, LocalDateTime due, Instant updated) {
	}

	ESyncTarget type();

	/** For history entries and messages, e.g. "Google Tasks". */
	String label();

	/** Current state of one remote copy; {@code gone} when it was deleted or is missing. */
	RemoteState fetch(String accessToken, String containerId, String externalId);

	/** Every copy in the container modified after {@code since}, deleted ones included. */
	List<RemoteState> changedSince(String accessToken, String containerId, Instant since);

	/** Whether two due values are the same for this target (Google Tasks only keeps the date). */
	boolean sameDue(LocalDateTime a, LocalDateTime b);

	/**
	 * The due date to store in MatrosDMS for a due date set in Google. Google Tasks only
	 * has a date, so the time of day of the current due date is kept.
	 */
	LocalDateTime adoptDue(LocalDateTime remoteDue, LocalDateTime currentDue);

	/** The user switched this target on and granted the scope for it. */
	boolean isEnabled(DBGoogleAccount account);

	/**
	 * Whether the action belongs in this target at all.
	 *
	 * @param alreadySynced the action already has a remote copy in this account
	 */
	boolean wants(ActionSnapshot action, boolean alreadySynced);

	/** The stored id of the MatrosDMS task list / calendar, or null. */
	String containerId(DBGoogleAccount account);

	/** Finds or creates the MatrosDMS task list / calendar and returns its id. */
	String createContainer(String accessToken);

	Remote create(String accessToken, String containerId, ActionSnapshot action);

	/** @return the new etag; throws a not-found {@link GoogleApiException} when the copy is gone */
	String update(String accessToken, String containerId, String externalId, ActionSnapshot action);

	/** Removes the remote copy; already gone counts as success. */
	void delete(String accessToken, String containerId, String externalId);
}
