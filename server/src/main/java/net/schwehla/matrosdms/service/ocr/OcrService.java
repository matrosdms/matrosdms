/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.ocr;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OcrService {

	private static final Logger log = LoggerFactory.getLogger(OcrService.class);
	private final List<IOcrProvider> providers;

	@Autowired
	public OcrService(List<IOcrProvider> providers) {
		this.providers = providers.stream()
				.sorted(Comparator.comparingInt(IOcrProvider::getPriority))
				.toList();

		log.info("OCR: Active providers: {}",
				providers.stream().map(IOcrProvider::getId).toList());
	}

	public boolean isOcrAvailable() {
		return providers.stream().anyMatch(IOcrProvider::isAvailable);
	}

	public String extractText(Path file, String mimeType) {
		for (IOcrProvider provider : providers) {
			if (provider.isAvailable()) {
				try {
					return provider.extractText(file, mimeType);
				} catch (Exception e) {
					log.warn("OCR Provider '{}' failed: {}", provider.getId(), e.getMessage());
				}
			}
		}
		return "";
	}
}