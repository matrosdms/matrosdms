/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.ocr;

import java.nio.file.Path;

public interface IOcrProvider {
	String getId();

	boolean isAvailable();

	int getPriority();

	String extractText(Path file, String mimeType);
}