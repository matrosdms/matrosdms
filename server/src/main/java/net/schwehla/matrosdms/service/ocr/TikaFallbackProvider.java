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
public class TikaFallbackProvider implements IOcrProvider {

    @Autowired
    TikaService tikaService;

    @Override
    public String getId() {
        return "tika-fallback";
    }

    @Override
    public boolean isAvailable() {
        return true; // Tika is always available as a library
    }

    @Override
    public int getPriority() {
        return 100; // Lowest priority (Use only if others fail)
    }

    @Override
    public String extractText(Path file, String mimeType) {
        // Tika handles everything, but quality for pure images might be lower than Tesseract
        return tikaService.extractText(file);
    }
}