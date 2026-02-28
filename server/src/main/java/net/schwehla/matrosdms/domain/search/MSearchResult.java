/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.search;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single row in the Search Result Table")
public class MSearchResult {
	private String uuid;
	private String name;
	private String description;

	// Context Info
	private String contextName;
	private String contextUuid;

	// Store / Physical Info
	private String storeName;
	private String storeIdentifier; // The UUID of the store
	private String storeItemNumber; // The "Binder Number" (storageItemIdentifier)

	// Metadata
	private List<String> tags;
	private LocalDate issueDate;
	private String stage; // "ACTIVE" or "CLOSED"
	private String filename; // For extension icons

	// Search specific
	private float score;
	private String highlight;

	public MSearchResult(
			String uuid,
			String name,
			String description,
			String contextName,
			String contextUuid,
			String storeName,
			String storeIdentifier,
			String storeItemNumber,
			List<String> tags,
			LocalDate issueDate,
			String stage,
			String filename,
			float score,
			String highlight) {
		this.uuid = uuid;
		this.name = name;
		this.description = description;
		this.contextName = contextName;
		this.contextUuid = contextUuid;
		this.storeName = storeName;
		this.storeIdentifier = storeIdentifier;
		this.storeItemNumber = storeItemNumber;
		this.tags = tags;
		this.issueDate = issueDate;
		this.stage = stage;
		this.filename = filename;
		this.score = score;
		this.highlight = highlight;
	}

	// Getters
	public String getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getContextName() {
		return contextName;
	}

	public String getContextUuid() {
		return contextUuid;
	}

	public String getStoreName() {
		return storeName;
	}

	public String getStoreIdentifier() {
		return storeIdentifier;
	}

	public String getStoreItemNumber() {
		return storeItemNumber;
	}

	public List<String> getTags() {
		return tags;
	}

	public LocalDate getIssueDate() {
		return issueDate;
	}

	public String getStage() {
		return stage;
	}

	public String getFilename() {
		return filename;
	}

	public float getScore() {
		return score;
	}

	public String getHighlight() {
		return highlight;
	}
}