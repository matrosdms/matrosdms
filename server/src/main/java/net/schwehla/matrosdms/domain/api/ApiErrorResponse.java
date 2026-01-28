/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponse") // Explicit Name
public class ApiErrorResponse {

	private LocalDateTime timestamp;
	private int status;
	private String errorCode;
	private String error;
	private String message;
	private String path;

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<ValidationError> validationErrors = new ArrayList<>();

	public ApiErrorResponse(int status, EErrorCode code, String error, String message, String path) {
		this.timestamp = LocalDateTime.now();
		this.status = status;
		this.errorCode = code != null ? code.getCode() : EErrorCode.INTERNAL_SERVER_ERROR.getCode();
		this.error = error;
		this.message = message;
		this.path = path;
	}

	public void addValidationError(String field, String message) {
		this.validationErrors.add(new ValidationError(field, message));
	}

	// --- Nested Class for Field Errors ---
	@Schema(name = "ValidationError") // Explicit Name
	public static class ValidationError {
		private String field;
		private String message;

		public ValidationError(String field, String message) {
			this.field = field;
			this.message = message;
		}

		public String getField() {
			return field;
		}

		public String getMessage() {
			return message;
		}
	}

	// --- Getters ---
	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public int getStatus() {
		return status;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getError() {
		return error;
	}

	public String getMessage() {
		return message;
	}

	public String getPath() {
		return path;
	}

	public List<ValidationError> getValidationErrors() {
		return validationErrors;
	}
}
