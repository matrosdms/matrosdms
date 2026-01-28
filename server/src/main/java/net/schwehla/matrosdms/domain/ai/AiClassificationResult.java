/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.ai;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiClassificationResult {
	private String contextUuid;
	private String categoryUuid;
	private String date;
	private String summary;
	private Map<String, Object> customAttributes;

	public String getContextUuid() {
		return contextUuid;
	}

	public void setContextUuid(String contextUuid) {
		this.contextUuid = contextUuid;
	}

	public String getCategoryUuid() {
		return categoryUuid;
	}

	public void setCategoryUuid(String categoryUuid) {
		this.categoryUuid = categoryUuid;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	public void setCustomAttributes(Map<String, Object> customAttributes) {
		this.customAttributes = customAttributes;
	}
}
