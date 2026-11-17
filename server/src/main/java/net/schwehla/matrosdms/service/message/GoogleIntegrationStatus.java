/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import java.time.LocalDateTime;

/**
 * The current user's Google sync state for the profile page.
 *
 * @param configured the server has Google OAuth credentials (otherwise nothing can be connected)
 * @param tasksGranted the user granted the Google Tasks scope on the consent screen
 * @param calendarGranted the user granted the calendar scope on the consent screen
 * @param lastPulledAt when Google was last checked for changes made there
 */
public record GoogleIntegrationStatus(
		boolean configured,
		boolean connected,
		String email,
		boolean tasksEnabled,
		boolean calendarEnabled,
		boolean tasksGranted,
		boolean calendarGranted,
		String lastError,
		LocalDateTime lastErrorAt,
		LocalDateTime lastPulledAt,
		long syncedTasks,
		long syncedEvents) {
}
