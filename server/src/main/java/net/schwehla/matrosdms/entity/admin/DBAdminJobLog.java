/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.admin;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import net.schwehla.matrosdms.entity.DBBaseEntity;

@Entity
@Table(name = "AdminJobLog")
public class DBAdminJobLog extends DBBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private LocalDateTime timestamp = LocalDateTime.now();
	private String severity; // INFO, WARN, ERROR
	private String message;

	@ManyToOne
	// NEW: Explicit Name
	@JoinColumn(name = "JOB_ID", foreignKey = @ForeignKey(name = "FK_JOB_LOG_JOB"))
	private DBAdminJob job;

	public DBAdminJobLog() {
	}

	public DBAdminJobLog(DBAdminJob job, String severity, String message) {
		this.job = job;
		this.severity = severity;
		this.message = message;
	}

	// Getters/Setters
	public Long getId() {
		return id;
	}

	public String getSeverity() {
		return severity;
	}

	public String getMessage() {
		return message;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setJob(DBAdminJob job) {
		this.job = job;
	}
}
