/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline;

import net.schwehla.matrosdms.domain.inbox.InboxFile;
import net.schwehla.matrosdms.service.message.PipelineStatusMessage;

public class PipelineEvents {
	// 1. Initial detection (Watcher -> UI)
	public record FileDetectedEvent(InboxFile file) {
	}

	// 2. Progress Tick (Step 1/3, 2/3...) - ADDED FILENAME
	public record PipelineProgressEvent(String sha256, String filename, String info, int step, int totalSteps) {
	}

	// 3. Metadata Enriched (Subject/From found)
	public record PipelineStatusEvent(InboxFile payload) {
	}

	// 4. Success
	public record PipelineResultEvent(PipelineStatusMessage result) {
	}

	// 5. Failure
	public record PipelineErrorEvent(String sha256, String reason) {
	}
}