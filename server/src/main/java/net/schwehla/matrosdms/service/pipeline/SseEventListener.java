/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.domain.api.EBroadcastSource;
import net.schwehla.matrosdms.domain.api.EBroadcastType;
import net.schwehla.matrosdms.messagebus.VUEMessageBus;
import net.schwehla.matrosdms.service.message.PipelineStatusMessage;
import net.schwehla.matrosdms.service.message.ProgressMessage;
import net.schwehla.matrosdms.service.pipeline.PipelineEvents.*;

@Component
public class SseEventListener {

	@Autowired
	private VUEMessageBus messageBus;

	@Async("taskExecutor")
	@EventListener
	public void handleFileDetected(FileDetectedEvent event) {
		messageBus.sendMessageToGUI(EBroadcastSource.INBOX, EBroadcastType.FILE_ADDED, event.file());
	}

	@Async("taskExecutor")
	@EventListener
	public void handleProgress(PipelineProgressEvent event) {
		// FIX: Map filename from event to message
		messageBus.sendMessageToGUI(
				EBroadcastSource.PIPELINE,
				EBroadcastType.PROGRESS,
				new ProgressMessage(event.sha256(), event.filename(), event.info(), event.step(), event.totalSteps()));
	}

	@Async("taskExecutor")
	@EventListener
	public void handleStatusUpdate(PipelineStatusEvent event) {
		messageBus.sendMessageToGUI(EBroadcastSource.INBOX, EBroadcastType.STATUS, event.payload());
	}

	@Async("taskExecutor")
	@EventListener
	public void handleResult(PipelineResultEvent event) {
		messageBus.sendMessageToGUI(EBroadcastSource.INBOX, EBroadcastType.STATUS, event.result());
	}

	@Async("taskExecutor")
	@EventListener
	public void handleError(PipelineErrorEvent event) {
		messageBus.sendMessageToGUI(
				EBroadcastSource.INBOX,
				EBroadcastType.ERROR,
				PipelineStatusMessage.error(event.sha256(), event.reason()));
	}
}