/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import java.util.ArrayList;
import java.util.List;

import net.schwehla.matrosdms.domain.api.EPipelineStatus;
import net.schwehla.matrosdms.domain.inbox.InboxFile;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Final Result Payload from the Pipeline")
public class PipelineStatusMessage {

	@Schema(description = "The Database ShortUUID (null if not yet persisted)")
	private String uuid;

	@Schema(description = "The SHA-256 Hash of the file")
	private String sha256;

	private EPipelineStatus status;
	private InboxFile fileState;
	private List<String> warnings = new ArrayList<>();

	public PipelineStatusMessage() {
	}

	// Factory: Error
	public static PipelineStatusMessage error(String hash, String reason) {
		PipelineStatusMessage m = new PipelineStatusMessage();
		m.sha256 = hash;
		m.status = EPipelineStatus.ERROR;
		m.fileState = new InboxFile();
		m.fileState.setSha256(hash);
		m.fileState.setStatus(EPipelineStatus.ERROR);
		m.warnings.add(reason);
		return m;
	}

	// Factory: Duplicate
	public static PipelineStatusMessage duplicate(String hash, String filename) {
		PipelineStatusMessage m = new PipelineStatusMessage();
		m.sha256 = hash;
		m.status = EPipelineStatus.DUPLICATE;
		m.fileState = new InboxFile();
		m.fileState.setSha256(hash);
		m.fileState.getFileInfo().setOriginalFilename(filename);
		m.fileState.setStatus(EPipelineStatus.DUPLICATE);
		return m;
	}

	// Factory: Success
	public static PipelineStatusMessage success(
			String hash, InboxFile finalState, List<String> warnings) {
		PipelineStatusMessage m = new PipelineStatusMessage();
		m.sha256 = hash;
		m.uuid = null;
		m.status = EPipelineStatus.READY;
		m.fileState = finalState;
		m.fileState.setStatus(EPipelineStatus.READY);
		m.warnings = warnings;
		return m;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
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

	public InboxFile getFileState() {
		return fileState;
	}

	public void setFileState(InboxFile fileState) {
		this.fileState = fileState;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}
}
