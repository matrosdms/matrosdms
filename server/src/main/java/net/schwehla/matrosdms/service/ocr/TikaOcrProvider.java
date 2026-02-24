/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.ocr;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.service.TikaService;

@Component
public class TikaOcrProvider implements IOcrProvider {

	@Autowired
	TikaService tikaService;

	@Override
	public String getId() {
		return "tika-standard";
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public int getPriority() {
		return 100; // Default priority
	}

	@Override
	public String extractText(Path file, String mimeType) {
		// Tika automatically handles OCR if Tesseract is installed on the OS
		// and handles parsing if it is a standard digital document.
		return tikaService.extractText(file);
	}
}