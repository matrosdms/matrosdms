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
	private String contextName;
	private String storeName;
	private List<String> tags;
	private LocalDate issueDate;
	private float score;
	private String highlight;

	public MSearchResult(
			String uuid,
			String name,
			String contextName,
			String storeName,
			List<String> tags,
			LocalDate issueDate,
			float score,
			String highlight) {
		this.uuid = uuid;
		this.name = name;
		this.contextName = contextName;
		this.storeName = storeName;
		this.tags = tags;
		this.issueDate = issueDate;
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

	public String getContextName() {
		return contextName;
	}

	public String getStoreName() {
		return storeName;
	}

	public List<String> getTags() {
		return tags;
	}

	public LocalDate getIssueDate() {
		return issueDate;
	}

	public float getScore() {
		return score;
	}

	public String getHighlight() {
		return highlight;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
