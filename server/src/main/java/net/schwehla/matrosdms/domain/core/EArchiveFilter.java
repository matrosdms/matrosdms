/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Filter mode for archival status")
public enum EArchiveFilter {
	@Schema(description = "Show everything (Active + Archived)")
	ALL,

	@Schema(description = "Show only active working documents (Default)")
	ACTIVE_ONLY,

	@Schema(description = "Show only the archive bin")
	ARCHIVED_ONLY;

	public class Names {
		public static final String ACTIVE_ONLY = "ACTIVE_ONLY";
		public static final String ARCHIVED_ONLY = "ARCHIVED_ONLY";
		public static final String ALL = "ALL";
	}
}