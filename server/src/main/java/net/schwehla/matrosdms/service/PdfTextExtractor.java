/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractor {

	private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

	/**
	 * Tries to extract text from the PDF without rendering images (No OCR).
	 * Returns empty string if no text layer exists or file is encrypted.
	 */
	public String quickExtract(Path pdfFile) {
		try (PDDocument doc = Loader.loadPDF(pdfFile.toFile())) {
			if (doc.isEncrypted())
				return "";

			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setSortByPosition(true);

			// Limit to first 5 pages to speed up decision making?
			// For now, read all to ensure we don't miss content.
			// ScanSnap usually puts text on all pages.
			return stripper.getText(doc).trim();
		} catch (IOException e) {
			log.warn("Fast text extraction failed for {}: {}", pdfFile.getFileName(), e.getMessage());
			return "";
		}
	}
}