/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Stable error codes for frontend logic")
public enum EErrorCode {

	// Generic
	INTERNAL_SERVER_ERROR("GEN_500"), BAD_REQUEST("GEN_400"),

	// Request Parsing
	REQ_JSON_MALFORMED("REQ_100"),

	// Entities
	ENTITY_NOT_FOUND("ENT_404"), ENTITY_CONFLICT("ENT_409"),

	// Database & Constraints
	DB_DUPLICATE_ENTRY("DB_100"), DB_LOCK_CONCURRENT("DB_101"), // Optimistic Locking

	// Validation
	VALIDATION_FAILED("VAL_100"),

	// Business Logic
	PROCESSING_ERROR("PROC_100"), DUPLICATE_FILE("PROC_101"), FILE_LOCKED("PROC_102");

	private final String code;

	EErrorCode(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
