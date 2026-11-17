/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.time.LocalDateTime;

import net.schwehla.matrosdms.domain.action.EActionPriority;
import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.entity.DBAction;

/**
 * The parts of an action that are pushed to Google, read inside a short transaction
 * so the slow remote calls run without holding a database connection.
 *
 * <p>{@code dueDate} is UTC wall time: the frontend sends {@code toISOString()} of a
 * date-only input, so a plain due date arrives as midnight UTC.
 */
public record ActionSnapshot(
		String uuid,
		String name,
		String description,
		EActionStatus status,
		EActionPriority priority,
		LocalDateTime dueDate,
		String assigneeUuid,
		String itemName,
		String contextName,
		String resolution,
		/** Server-local time of the last change in MatrosDMS, for "later change wins". */
		LocalDateTime dateUpdated) {

	public boolean completed() {
		return status != null && status.isCompleted();
	}

	public static ActionSnapshot of(DBAction a) {
		return new ActionSnapshot(
				a.getUuid(),
				a.getName(),
				a.getDescription(),
				a.getStatus(),
				a.getPriority(),
				a.getDueDate(),
				a.getAssignee() != null ? a.getAssignee().getUuid() : null,
				a.getItem() != null ? a.getItem().getName() : null,
				a.getContext() != null ? a.getContext().getName() : null,
				a.getResolution(),
				a.getDateUpdated());
	}

	/** Body text shared by the task notes and the event description. */
	public String notes() {
		StringBuilder sb = new StringBuilder();
		if (description != null && !description.isBlank()) {
			sb.append(description.strip()).append("\n\n");
		}
		if (completed() && resolution != null && !resolution.isBlank()) {
			sb.append("Resolution: ").append(resolution.strip()).append('\n');
		}
		if (itemName != null) {
			sb.append("Document: ").append(itemName).append('\n');
		}
		if (contextName != null) {
			sb.append("Context: ").append(contextName).append('\n');
		}
		if (priority != null && priority != EActionPriority.NORMAL) {
			sb.append("Priority: ").append(priority).append('\n');
		}
		sb.append("— MatrosDMS");
		return sb.toString();
	}

	static String truncate(String s, int max) {
		if (s == null) {
			return "";
		}
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}
}
