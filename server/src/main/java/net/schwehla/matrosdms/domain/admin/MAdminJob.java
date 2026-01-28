/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MAdminJob {

	private Long id;
	private EJobType type;
	private EJobStatus status;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String configuration;
	private String progressInfo;

	private List<MAdminJobLog> logs = new ArrayList<>();

	// Getters / Setters
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

	public EJobStatus getStatus() {
		return status;
	}

	public void setStatus(EJobStatus status) {
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

	public List<MAdminJobLog> getLogs() {
		return logs;
	}

	public void setLogs(List<MAdminJobLog> logs) {
		this.logs = logs;
	}
}
