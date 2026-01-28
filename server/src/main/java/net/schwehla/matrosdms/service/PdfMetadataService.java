/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PdfMetadataService {

	private static final Logger log = LoggerFactory.getLogger(PdfMetadataService.class);

	private static final String KEY_UUID = "MatrosUUID";
	private static final String KEY_FILENAME = "MatrosOriginalFilename";

	public void injectMetadata(Path pdfFile, String uuid, String originalFilename) {
		Path tempOutput = pdfFile.getParent().resolve(pdfFile.getFileName().toString() + ".tmp_meta");

		try (PDDocument doc = Loader.loadPDF(pdfFile.toFile())) {

			if (doc.isEncrypted()) {
				log.warn("Skipping metadata injection: PDF is encrypted");
				return;
			}

			PDDocumentInformation info = doc.getDocumentInformation();

			// 1. Critical System ID
			info.setCustomMetadataValue(KEY_UUID, uuid);

			// 2. Original Filename (for recovery if DB is lost)
			info.setCustomMetadataValue(KEY_FILENAME, originalFilename);

			// 3. Update Standard Fields for Search Indexers (Windows/Mac Spotlight)
			info.setTitle(originalFilename);
			info.setProducer("MatrosDMS 2.0");
			info.setModificationDate(
					GregorianCalendar.from(LocalDateTime.now().atZone(ZoneId.systemDefault())));

			// 4. Tags
			String tag = "matros:" + uuid;
			String keywords = info.getKeywords();
			if (keywords == null || keywords.isEmpty()) {
				info.setKeywords(tag);
			} else if (!keywords.contains(tag)) {
				info.setKeywords(keywords + " " + tag);
			}

			doc.save(tempOutput.toFile());
		} catch (IOException e) {
			log.error("Failed to inject metadata", e);
			return;
		}

		try {
			Files.move(tempOutput, pdfFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			log.error("Failed to replace original PDF", e);
		}
	}
}
