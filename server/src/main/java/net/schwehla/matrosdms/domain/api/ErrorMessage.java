/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.api;

import java.time.LocalDate;

public class ErrorMessage {

	private int statusCode;
	private LocalDate timestamp;
	private String message;
	private String description;

	public ErrorMessage(int statusCode, LocalDate timestamp, String message, String description) {
		this.statusCode = statusCode;
		this.timestamp = timestamp;
		this.message = message;
		this.description = description;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public LocalDate getTimestamp() {
		return timestamp;
	}

	public String getMessage() {
		return message;
	}

	public String getDescription() {
		return description;
	}
}
