/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.inbox;

import java.time.LocalDateTime;

public class StagingMetadata {
	public String originalFilename;
	public String source;
	public LocalDateTime receivedAt;

	public StagingMetadata() {
	}

	public StagingMetadata(String originalFilename, String source) {
		this.originalFilename = originalFilename;
		this.source = source;
		this.receivedAt = LocalDateTime.now();
	}
}
