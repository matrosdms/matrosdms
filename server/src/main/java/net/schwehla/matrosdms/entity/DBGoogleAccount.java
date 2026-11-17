/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * A MatrosDMS user's linked Google account. No {@code @Version}: sync jobs write the
 * error and container columns with targeted update queries while the profile page
 * edits the toggles, and neither should fail on the other.
 */
@Entity
@Table(name = "google_account")
public class DBGoogleAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "google_account_seq")
	@SequenceGenerator(name = "google_account_seq", sequenceName = "google_account_seq", allocationSize = 50)
	private Long id;

	@Column(name = "user_uuid", nullable = false, unique = true, length = 16)
	private String userUuid;

	@Column(name = "google_email", length = 320)
	private String googleEmail;

	/** Encrypted (see GoogleTokenCipher). Null = disconnected. */
	@Column(name = "refresh_token", length = 4000)
	private String refreshToken;

	@Column(name = "granted_scopes", length = 1000)
	private String grantedScopes;

	@Column(name = "tasks_enabled", nullable = false)
	private boolean tasksEnabled;

	@Column(name = "calendar_enabled", nullable = false)
	private boolean calendarEnabled;

	@Column(name = "task_list_id")
	private String taskListId;

	@Column(name = "calendar_id")
	private String calendarId;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	@Column(name = "last_error_at")
	private LocalDateTime lastErrorAt;

	@Column(name = "date_connected")
	private LocalDateTime dateConnected;

	/** Start of the last successful poll for changes made in Google. */
	@Column(name = "last_pulled_at")
	private LocalDateTime lastPulledAt;

	public LocalDateTime getLastPulledAt() {
		return lastPulledAt;
	}

	public void setLastPulledAt(LocalDateTime lastPulledAt) {
		this.lastPulledAt = lastPulledAt;
	}

	public boolean isConnected() {
		return refreshToken != null;
	}

	public boolean hasScope(String scope) {
		if (grantedScopes == null) {
			return false;
		}
		for (String s : grantedScopes.split(" ")) {
			if (s.equals(scope)) {
				return true;
			}
		}
		return false;
	}

	public Long getId() {
		return id;
	}

	public String getUserUuid() {
		return userUuid;
	}

	public void setUserUuid(String userUuid) {
		this.userUuid = userUuid;
	}

	public String getGoogleEmail() {
		return googleEmail;
	}

	public void setGoogleEmail(String googleEmail) {
		this.googleEmail = googleEmail;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getGrantedScopes() {
		return grantedScopes;
	}

	public void setGrantedScopes(String grantedScopes) {
		this.grantedScopes = grantedScopes;
	}

	public boolean isTasksEnabled() {
		return tasksEnabled;
	}

	public void setTasksEnabled(boolean tasksEnabled) {
		this.tasksEnabled = tasksEnabled;
	}

	public boolean isCalendarEnabled() {
		return calendarEnabled;
	}

	public void setCalendarEnabled(boolean calendarEnabled) {
		this.calendarEnabled = calendarEnabled;
	}

	public String getTaskListId() {
		return taskListId;
	}

	public void setTaskListId(String taskListId) {
		this.taskListId = taskListId;
	}

	public String getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(String calendarId) {
		this.calendarId = calendarId;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public LocalDateTime getLastErrorAt() {
		return lastErrorAt;
	}

	public void setLastErrorAt(LocalDateTime lastErrorAt) {
		this.lastErrorAt = lastErrorAt;
	}

	public LocalDateTime getDateConnected() {
		return dateConnected;
	}

	public void setDateConnected(LocalDateTime dateConnected) {
		this.dateConnected = dateConnected;
	}
}
