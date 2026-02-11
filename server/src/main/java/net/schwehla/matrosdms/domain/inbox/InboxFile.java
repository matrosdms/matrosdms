/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.inbox;

import java.io.Serializable;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.schwehla.matrosdms.domain.api.EPipelineStatus;
import net.schwehla.matrosdms.domain.core.EItemSource;

import io.swagger.v3.oas.annotations.media.Schema;

public class InboxFile implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "SHA-256 Content Hash (used as ID in Staging)")
	private String sha256;

	private EPipelineStatus status = EPipelineStatus.PROCESSING;
	private EItemSource source;
	private String progressMessage;

	private FileMetadata fileInfo = new FileMetadata();

	@Schema(nullable = true)
	private EmailMetadata emailInfo;

	@Schema(nullable = true)
	private Prediction prediction;

	@Schema(description = "UUID of existing item if this is a duplicate", nullable = true)
	private String doublette;

	@JsonIgnore
	private transient Path path;

	public InboxFile() {
	}

	@Schema(description = "Primary display title")
	@JsonProperty
	public String getDisplayName() {
		if (emailInfo != null && emailInfo.getSubject() != null) {
			return emailInfo.getSubject();
		}
		return fileInfo != null ? fileInfo.getOriginalFilename() : "Unknown";
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public EPipelineStatus getStatus() {
		return status;
	}

	public void setStatus(EPipelineStatus status) {
		this.status = status;
	}

	public EItemSource getSource() {
		return source;
	}

	public void setSource(EItemSource source) {
		this.source = source;
	}

	public String getProgressMessage() {
		return progressMessage;
	}

	public void setProgressMessage(String progressMessage) {
		this.progressMessage = progressMessage;
	}

	public FileMetadata getFileInfo() {
		return fileInfo;
	}

	public void setFileInfo(FileMetadata fileInfo) {
		this.fileInfo = fileInfo;
	}

	public EmailMetadata getEmailInfo() {
		return emailInfo;
	}

	public void setEmailInfo(EmailMetadata emailInfo) {
		this.emailInfo = emailInfo;
	}

	public Prediction getPrediction() {
		return prediction;
	}

	public void setPrediction(Prediction prediction) {
		this.prediction = prediction;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public String getDoublette() {
		return doublette;
	}

	public void setDoublette(String doublette) {
		this.doublette = doublette;
	}
}
