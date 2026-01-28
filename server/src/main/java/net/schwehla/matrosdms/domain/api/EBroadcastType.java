/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "The specific event that occurred")
public enum EBroadcastType {
	FILE_ADDED, STATUS, PROGRESS, // <--- NEW: For live updates ("OCR 50%", "AI Thinking...")
	COMPLETE, ERROR
}
