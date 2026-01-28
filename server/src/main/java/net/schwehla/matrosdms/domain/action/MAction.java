/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.action;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import net.schwehla.matrosdms.domain.core.MBaseElement;
import net.schwehla.matrosdms.domain.core.MUser;

public class MAction extends MBaseElement {

	private static final long serialVersionUID = 1L;

	private EActionStatus status;
	private EActionPriority priority;
	private LocalDateTime dueDate;
	private LocalDateTime completedDate;

	private MUser assignee;
	private MUser creator;

	private String resolution;

	private EExternalActionTracker externalActionTracker;
	private String externalId;

	private String itemIdentifier;
	private String contextIdentifier;

	private List<ActionLog> history = new ArrayList<>();

	// Getters / Setters
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

	public MUser getAssignee() {
		return assignee;
	}

	public void setAssignee(MUser assignee) {
		this.assignee = assignee;
	}

	public MUser getCreator() {
		return creator;
	}

	public void setCreator(MUser creator) {
		this.creator = creator;
	}

	public String getItemIdentifier() {
		return itemIdentifier;
	}

	public void setItemIdentifier(String itemIdentifier) {
		this.itemIdentifier = itemIdentifier;
	}

	public String getContextIdentifier() {
		return contextIdentifier;
	}

	public void setContextIdentifier(String contextIdentifier) {
		this.contextIdentifier = contextIdentifier;
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

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public EExternalActionTracker getExternalActionTracker() {
		return externalActionTracker;
	}

	public void setExternalActionTracker(EExternalActionTracker externalActionTracker) {
		this.externalActionTracker = externalActionTracker;
	}
}
