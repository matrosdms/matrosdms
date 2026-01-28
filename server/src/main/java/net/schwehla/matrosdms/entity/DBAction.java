/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import net.schwehla.matrosdms.domain.action.ActionLog;
import net.schwehla.matrosdms.domain.action.EActionPriority;
import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.domain.action.EExternalActionTracker;
import net.schwehla.matrosdms.entity.converter.ActionLogListConverter;
import net.schwehla.matrosdms.entity.management.DBUser;

@Entity
@Table(name = "Action", indexes = {
		@Index(name = "idx_action_uuid", columnList = "uuid"),
		@Index(name = "idx_action_assignee", columnList = "ASSIGNEE_ID"),
		@Index(name = "idx_action_status", columnList = "status"),
		@Index(name = "idx_action_duedate", columnList = "dueDate")
})
@NamedQueries({
		@NamedQuery(name = "DBAction.findByUuid", query = "SELECT a FROM DBAction a WHERE a.uuid = :uuid"),
})
public class DBAction extends AbstractDBInfoBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ACTION_ID")
	private Long id;

	@Override
	public Long getPK() {
		return id;
	}

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EActionStatus status = EActionStatus.OPEN;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EActionPriority priority = EActionPriority.NORMAL;

	private LocalDateTime dueDate;
	private LocalDateTime completedDate;

	// NEW FIELD: The closing statement / final result
	@Column(length = 1000)
	private String resolution;

	private String externalId;

	// The version hash from the other system (Required for Google API updates)
	private String externalEtag;

	// The "Switch" to know which system handles this
	@Enumerated(EnumType.STRING)
	@Column(length = 50)
	private EExternalActionTracker externalActionTracker = EExternalActionTracker.NONE;

	@Column(columnDefinition = "json")
	@Convert(converter = ActionLogListConverter.class)
	private List<ActionLog> history = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ASSIGNEE_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_ACTION_ASSIGNEE"))
	private DBUser assignee;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "CREATOR_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_ACTION_CREATOR"))
	private DBUser creator;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ITEM_ID", foreignKey = @ForeignKey(name = "FK_ACTION_ITEM"))
	private DBItem item;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CONTEXT_ID", foreignKey = @ForeignKey(name = "FK_ACTION_CONTEXT"))
	private DBContext context;

	// Getters / Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public EActionStatus getStatus() {
		return status;
	}

	public void setStatus(EActionStatus status) {
		this.status = status;
	}

	public EActionPriority getPriority() {
		return priority;
	}

	public void setPriority(EActionPriority priority) {
		this.priority = priority;
	}

	public LocalDateTime getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDateTime dueDate) {
		this.dueDate = dueDate;
	}

	public LocalDateTime getCompletedDate() {
		return completedDate;
	}

	public void setCompletedDate(LocalDateTime completedDate) {
		this.completedDate = completedDate;
	}

	public DBUser getAssignee() {
		return assignee;
	}

	public void setAssignee(DBUser assignee) {
		this.assignee = assignee;
	}

	public DBUser getCreator() {
		return creator;
	}

	public void setCreator(DBUser creator) {
		this.creator = creator;
	}

	public DBItem getItem() {
		return item;
	}

	public void setItem(DBItem item) {
		this.item = item;
	}

	public DBContext getContext() {
		return context;
	}

	public void setContext(DBContext context) {
		this.context = context;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public List<ActionLog> getHistory() {
		return history;
	}

	public void setHistory(List<ActionLog> history) {
		this.history = history;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getExternalEtag() {
		return externalEtag;
	}

	public void setExternalEtag(String externalEtag) {
		this.externalEtag = externalEtag;
	}

	public EExternalActionTracker getExternalActionTracker() {
		return externalActionTracker;
	}

	public void setExternalActionTracker(EExternalActionTracker externalActionTracker) {
		this.externalActionTracker = externalActionTracker;
	}
}
