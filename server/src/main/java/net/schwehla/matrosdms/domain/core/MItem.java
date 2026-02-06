/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.schwehla.matrosdms.domain.attribute.MAttribute;

import io.swagger.v3.oas.annotations.media.Schema;

public class MItem extends MBaseElement {
	private static final long serialVersionUID = 1L;

	private MFileMetadata metadata;
	private MCategoryList kindList;
	private List<MAttribute> attributeList = Collections.synchronizedList(new ArrayList<>());
	private MContext context;
	private String storeIdentifier;
	private String storeItemNumber;

	// Lifecycle Dates
	private LocalDateTime issueDate;
	private LocalDateTime dateExpire;
	private LocalDateTime dateArchived;

	private EStage stage = EStage.ACTIVE;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "True if the item is archived")
	public boolean isArchived() {
		return dateArchived != null;
	}

	// Flag only
	private boolean textParsed;

	public MFileMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(MFileMetadata metadata) {
		this.metadata = metadata;
	}

	public EStage getStage() {
		return stage;
	}

	public void setStage(EStage stage) {
		this.stage = stage;
	}

	public List<MAttribute> getAttributeList() {
		return attributeList;
	}

	public void setAttributeList(List<MAttribute> attributeList) {
		this.attributeList = attributeList;
	}

	public String getStoreItemNumber() {
		return storeItemNumber;
	}

	public LocalDateTime getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(LocalDateTime issueDate) {
		this.issueDate = issueDate;
	}

	public void setStoreItemNumber(String identifier) {
		this.storeItemNumber = identifier;
	}

	public String getStoreIdentifier() {
		return storeIdentifier;
	}

	public void setStoreIdentifier(String storeIdentifier) {
		this.storeIdentifier = storeIdentifier;
	}

	public LocalDateTime getDateExpire() {
		return dateExpire;
	}

	public void setDateExpire(LocalDateTime dateExpire) {
		this.dateExpire = dateExpire;
	}

	public LocalDateTime getDateArchived() {
		return dateArchived;
	}

	public void setDateArchived(LocalDateTime dateArchived) {
		this.dateArchived = dateArchived;
	}

	public int getCount() {
		return kindList.size();
	}

	public MContext getContext() {
		return context;
	}

	public void setContext(MContext context) {
		this.context = context;
	}

	public MCategoryList getKindList() {
		return kindList;
	}

	public void setKindList(MCategoryList kindList) {
		this.kindList = kindList;
	}

	public boolean isTextParsed() {
		return textParsed;
	}

	public void setTextParsed(boolean textParsed) {
		this.textParsed = textParsed;
	}
}