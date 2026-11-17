/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

/**
 * SSE payload: a task was changed by something other than the UI.
 *
 * @param source where the change came from, e.g. "Google Tasks"
 * @param summary what changed, for a toast ("completed", "due 2026-11-25")
 */
public record ActionChangedMessage(String actionUuid, String name, String source, String summary) {
}
