/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.ocr;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

/**
 * Disabled/Placeholder.
 * OCR is currently handled entirely by TikaOcrProvider.
 * Delete this file if you wish to clean up the project.
 */
@Component
public class LocalTesseractProvider implements IOcrProvider {

	@Override
	public String getId() {
		return "tesseract-disabled";
	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public int getPriority() {
		return 999;
	}

	@Override
	public String extractText(Path file, String mimeType) {
		return "";
	}
}