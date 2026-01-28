/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.api;

import net.schwehla.matrosdms.domain.inbox.InboxFile;
import net.schwehla.matrosdms.service.message.PipelineStatusMessage;
import net.schwehla.matrosdms.service.message.ProgressMessage;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Server-Sent Event Payload")
public class BroadcastMessage {

	private EBroadcastSource process;
	private EBroadcastType type;

	// --- FIX: Replaced String.class with ProgressMessage.class ---
	@Schema(description = "Dynamic payload depending on event type", oneOf = {
			PipelineStatusMessage.class,
			InboxFile.class,
			ProgressMessage.class // <--- NEW: Structured Progress
	})
	private Object message;

	@Schema(description = "Helper for deserialization", example = "PipelineStatusMessage")
	private String messageType;

	public BroadcastMessage() {
	}

	public BroadcastMessage(Object payload) {
		this.message = payload;
		if (payload != null) {
			this.messageType = payload.getClass().getSimpleName();
		}
	}

	public EBroadcastSource getProcess() {
		return process;
	}

	public void setProcess(EBroadcastSource process) {
		this.process = process;
	}

	public EBroadcastType getType() {
		return type;
	}

	public void setType(EBroadcastType type) {
		this.type = type;
	}

	public Object getMessage() {
		return message;
	}

	public void setMessage(Object message) {
		this.message = message;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
}
