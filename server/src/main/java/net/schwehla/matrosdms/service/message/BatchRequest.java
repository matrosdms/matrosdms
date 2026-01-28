/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public class BatchRequest {

	@NotEmpty
	private List<String> itemUuids;

	// For Move
	private String targetContextUuid;

	// For Tagging
	private List<String> addTags;
	private List<String> removeTags;

	public List<String> getItemUuids() {
		return itemUuids;
	}

	public void setItemUuids(List<String> itemUuids) {
		this.itemUuids = itemUuids;
	}

	public String getTargetContextUuid() {
		return targetContextUuid;
	}

	public void setTargetContextUuid(String targetContextUuid) {
		this.targetContextUuid = targetContextUuid;
	}

	public List<String> getAddTags() {
		return addTags;
	}

	public void setAddTags(List<String> addTags) {
		this.addTags = addTags;
	}

	public List<String> getRemoveTags() {
		return removeTags;
	}

	public void setRemoveTags(List<String> removeTags) {
		this.removeTags = removeTags;
	}
}
