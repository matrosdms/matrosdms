/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import net.schwehla.matrosdms.domain.action.ActionLog;
import net.schwehla.matrosdms.domain.action.EActionPriority;
import net.schwehla.matrosdms.domain.action.EActionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class BaseActionMessage {

	@NotBlank(message = "Title is required")
	@Size(min = 2, max = 255, message = "Title must be between 2 and 255 characters")
	private String name;

	@Size(max = 4000, message = "Description too long")
	private String description;

	private EActionPriority priority = EActionPriority.NORMAL;

	@Schema(nullable = true, description = "When the action must be completed")
	private LocalDateTime dueDate;

	private String assigneeIdentifier;

	@Size(max = 1000, message = "Resolution too long")
	private String resolution;

	private List<ActionLog> history = new ArrayList<>();

	// Status is usually updated, not created (default is OPEN)
	private EActionStatus status;

	public EActionStatus getStatus() {
		return status;
	}

	public void setStatus(EActionStatus status) {
		this.status = status;
	}

	// --- Getters / Setters ---
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public String getAssigneeIdentifier() {
		return assigneeIdentifier;
	}

	public void setAssigneeIdentifier(String assigneeIdentifier) {
		this.assigneeIdentifier = assigneeIdentifier;
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
}
