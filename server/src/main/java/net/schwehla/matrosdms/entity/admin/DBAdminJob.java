/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import net.schwehla.matrosdms.domain.admin.EJobType;
import net.schwehla.matrosdms.entity.DBBaseEntity;

@Entity
@Table(name = "AdminJob")
public class DBAdminJob extends DBBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Enumerated(EnumType.STRING)
	private EJobType type;

	@Enumerated(EnumType.STRING)
	private JobStatus status;

	private LocalDateTime startTime;
	private LocalDateTime endTime;

	// Generic field to store "deepCheck=true" or "target=/tmp/export"
	private String configuration;

	// Generic field for "Scanned 500/1000 items"
	private String progressInfo;

	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<DBAdminJobLog> logs = new ArrayList<>();

	public enum JobStatus {
		QUEUED, RUNNING, COMPLETED, FAILED
	}

	// --- Helpers ---
	public void addLog(String severity, String message) {
		logs.add(new DBAdminJobLog(this, severity, message));
	}

	// --- Getters/Setters ---
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public EJobType getType() {
		return type;
	}

	public void setType(EJobType type) {
		this.type = type;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public String getConfiguration() {
		return configuration;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

	public String getProgressInfo() {
		return progressInfo;
	}

	public void setProgressInfo(String progressInfo) {
		this.progressInfo = progressInfo;
	}

	public List<DBAdminJobLog> getLogs() {
		return logs;
	}

	public void setLogs(List<DBAdminJobLog> logs) {
		this.logs = logs;
	}
}
