/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.schwehla.matrosdms.domain.inbox.EmailMetadata;
import net.schwehla.matrosdms.domain.inbox.Prediction;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DigestResultMessage {

	private String sha256;
	private String mimeType;

	private Prediction prediction = new Prediction();
	private EmailMetadata emailMetadata;

	public DigestResultMessage() {
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public Prediction getPrediction() {
		return prediction;
	}

	public void setPrediction(Prediction prediction) {
		this.prediction = prediction;
	}

	public EmailMetadata getEmailMetadata() {
		return emailMetadata;
	}

	public void setEmailMetadata(EmailMetadata emailMetadata) {
		this.emailMetadata = emailMetadata;
	}
}
