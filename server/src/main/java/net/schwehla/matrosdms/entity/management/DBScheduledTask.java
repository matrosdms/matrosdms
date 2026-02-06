/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.management;

import java.sql.Types;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "scheduled_tasks", indexes = {
		@Index(name = "idx_scheduled_tasks_execution_time", columnList = "execution_time"),
		@Index(name = "idx_scheduled_tasks_last_heartbeat", columnList = "last_heartbeat"),
		@Index(name = "idx_scheduled_tasks_priority_execution_time", columnList = "priority DESC, execution_time ASC")
})
@NamedQueries({
		@NamedQuery(name = "DBScheduledTask.findAll", query = "SELECT t FROM DBScheduledTask t"),
		@NamedQuery(name = "DBScheduledTask.findByNameAndInstance", query = "SELECT t FROM DBScheduledTask t WHERE t.id.taskName = :taskName AND t.id.taskInstance = :taskInstance"),
		@NamedQuery(name = "DBScheduledTask.findDue", query = "SELECT t FROM DBScheduledTask t WHERE t.executionTime <= :now AND t.picked = false ORDER BY t.priority DESC, t.executionTime ASC")
})
public class DBScheduledTask {

	@EmbeddedId
	private DBScheduledTaskId id;

	@Lob
	@JdbcTypeCode(Types.LONGVARBINARY) // <--- ADD THIS
	@Column(name = "task_data")
	private byte[] taskData;

	@Column(name = "execution_time", nullable = false)
	private OffsetDateTime executionTime;

	@Column(nullable = false)
	private boolean picked;

	@Column(name = "picked_by")
	private String pickedBy;

	@Column(name = "last_success")
	private OffsetDateTime lastSuccess;

	@Column(name = "last_failure")
	private OffsetDateTime lastFailure;

	@Column(name = "consecutive_failures")
	private Integer consecutiveFailures;

	@Column(name = "last_heartbeat")
	private OffsetDateTime lastHeartbeat;

	@Column(nullable = false)
	private long version;

	@Column
	private Short priority;

	// --- GETTERS & SETTERS ---

	public DBScheduledTaskId getId() {
		return id;
	}

	public void setId(DBScheduledTaskId id) {
		this.id = id;
	}

	public byte[] getTaskData() {
		return taskData;
	}

	public void setTaskData(byte[] taskData) {
		this.taskData = taskData;
	}

	public OffsetDateTime getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(OffsetDateTime executionTime) {
		this.executionTime = executionTime;
	}

	public boolean isPicked() {
		return picked;
	}

	public void setPicked(boolean picked) {
		this.picked = picked;
	}

	public String getPickedBy() {
		return pickedBy;
	}

	public void setPickedBy(String pickedBy) {
		this.pickedBy = pickedBy;
	}

	public OffsetDateTime getLastSuccess() {
		return lastSuccess;
	}

	public void setLastSuccess(OffsetDateTime lastSuccess) {
		this.lastSuccess = lastSuccess;
	}

	public OffsetDateTime getLastFailure() {
		return lastFailure;
	}

	public void setLastFailure(OffsetDateTime lastFailure) {
		this.lastFailure = lastFailure;
	}

	public Integer getConsecutiveFailures() {
		return consecutiveFailures;
	}

	public void setConsecutiveFailures(Integer consecutiveFailures) {
		this.consecutiveFailures = consecutiveFailures;
	}

	public OffsetDateTime getLastHeartbeat() {
		return lastHeartbeat;
	}

	public void setLastHeartbeat(OffsetDateTime lastHeartbeat) {
		this.lastHeartbeat = lastHeartbeat;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Short getPriority() {
		return priority;
	}

	public void setPriority(Short priority) {
		this.priority = priority;
	}
}
