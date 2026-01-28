/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.inbox;

import java.time.LocalDateTime;

/**
 * Persisted as "source.info" in the staging folder. Ensures original filename
 * is never lost.
 */
public class SourceMetadata {
	public String originalFilename;
	public String sourceFolder; // "email", "scan", "upload"
	public LocalDateTime receivedAt;

	public SourceMetadata() {
	}

	public SourceMetadata(String originalFilename, String sourceFolder) {
		this.originalFilename = originalFilename;
		this.sourceFolder = sourceFolder;
		this.receivedAt = LocalDateTime.now();
	}
}
