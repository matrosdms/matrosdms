/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.action;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ActionLog implements Serializable {

	private static final long serialVersionUID = 1L;

	private String user;
	private String message;
	private LocalDateTime timestamp;

	public ActionLog() {
	}

	public ActionLog(String user, String message) {
		this.user = user;
		this.message = message;
		this.timestamp = LocalDateTime.now();
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
}
