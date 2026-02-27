/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.inbox;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI Analysis Results")
public class Prediction implements Serializable {

	@Schema(description = "UUID of the suggested Kind")
	private String kind;

	@Schema(description = "UUID of the suggested Context/Folder")
	private String context;

	private LocalDate documentDate;
	private String summary;
	private Double confidence;

	@Schema(description = "Key-Value pairs extracted by AI")
	private Map<String, Object> attributes;

	public Prediction() {
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public LocalDate getDocumentDate() {
		return documentDate;
	}

	public void setDocumentDate(LocalDate documentDate) {
		this.documentDate = documentDate;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Double getConfidence() {
		return confidence;
	}

	public void setConfidence(Double confidence) {
		this.confidence = confidence;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
}
