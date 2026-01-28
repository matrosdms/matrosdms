/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum EItemSource {
	UPLOAD, EMAIL, SCAN, API, UNKNOWN
}
