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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import net.schwehla.matrosdms.domain.core.EStage;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class BaseItemMessage {

	@NotBlank(message = "Name is required")
	@Size(min = 2, max = 150)
	private String name;

	@Size(max = 255)
	private String description;

	private String icon;

	@Schema(nullable = true)
	private LocalDateTime issueDate;

	@Schema(nullable = true)
	private LocalDateTime dateExpire;

	private String storeItemNumber;
	private String storeIdentifier;
	private EStage stage = EStage.ACTIVE;

	@NotNull
	private List<String> kindList = new ArrayList<>();

	@NotNull
	private Map<String, Object> attributes = new HashMap<>();

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

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public LocalDateTime getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(LocalDateTime issueDate) {
		this.issueDate = issueDate;
	}

	public LocalDateTime getDateExpire() {
		return dateExpire;
	}

	public void setDateExpire(LocalDateTime dateExpire) {
		this.dateExpire = dateExpire;
	}

	public String getStoreItemNumber() {
		return storeItemNumber;
	}

	public void setStoreItemNumber(String storeItemNumber) {
		this.storeItemNumber = storeItemNumber;
	}

	public String getStoreIdentifier() {
		return storeIdentifier;
	}

	public void setStoreIdentifier(String storeIdentifier) {
		this.storeIdentifier = storeIdentifier;
	}

	public EStage getStage() {
		return stage;
	}

	public void setStage(EStage stage) {
		this.stage = stage;
	}

	public List<String> getKindList() {
		return kindList;
	}

	public void setKindList(List<String> kindList) {
		this.kindList = kindList;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
}
