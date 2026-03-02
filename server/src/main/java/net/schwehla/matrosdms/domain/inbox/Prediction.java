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

	@Schema(description = "UUID of the suggested Kind (document category)")
	private String kind;

	@Schema(description = "UUID of the suggested Context/Folder")
	private String context;

	private LocalDate documentDate;
	private String summary;

	@Schema(description = "Overall confidence score 0.0-1.0 for the entire prediction")
	private Double confidence;

	@Schema(description = "Per-field confidence scores: keys are field names (context, kind, documentDate, summary), values 0.0-1.0")
	private Map<String, Double> fieldConfidences;

	@Schema(description = "ID of the strategy that produced this prediction: 'ollama', 'heuristic'")
	private String strategyId;

	@Schema(description = "True when the user manually set at least one field, overriding the AI suggestion")
	private Boolean manuallyAssigned;

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

	public Map<String, Double> getFieldConfidences() {
		return fieldConfidences;
	}

	public void setFieldConfidences(Map<String, Double> fieldConfidences) {
		this.fieldConfidences = fieldConfidences;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public Boolean getManuallyAssigned() {
		return manuallyAssigned;
	}

	public void setManuallyAssigned(Boolean manuallyAssigned) {
		this.manuallyAssigned = manuallyAssigned;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
}
