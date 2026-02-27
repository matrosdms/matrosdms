/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Semaphore;

import jakarta.annotation.PostConstruct;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.store.util.FileExtensionService;

@Service
@Lazy
public class PdfConversionService {

	private static final Logger log = LoggerFactory.getLogger(PdfConversionService.class);
	private static final String FONT_RESOURCE = "fonts/Roboto-Regular.ttf";

	// MUCH STRICTER Heuristics to prevent skipping OCR on bad scans
	private static final int MIN_TEXT_CHARS = 150;
	private static final double MIN_CHARS_PER_PAGE = 150.0;
	private static final Set<String> OCR_PRODUCERS = Set.of(
			"abbyy", "finereader", "tesseract", "ocrmypdf", "omnipage", "readiris");

	private volatile byte[] fontBytes;
	private final Semaphore conversionSemaphore = new Semaphore(2);

	@Autowired
	private FileExtensionService extensionService;

	// --- Rich Result Record ---
	public record AnalysisResult(
			boolean isDigitalPdf,
			String extractedText,
			boolean needsOcr,
			int pageCount,
			String producer) {
	}

	@PostConstruct
	public void loadFontOnce() {
		try (InputStream is = new ClassPathResource(FONT_RESOURCE).getInputStream()) {
			fontBytes = is.readAllBytes();
		} catch (Exception e) {
			log.warn("Unicode font missing: {}", FONT_RESOURCE);
		}
	}

	/**
	 * "The Inspector": Opens PDF once, checks metadata, and extracts text.
	 * Decisions are made here to avoid re-opening the file later.
	 */
	public AnalysisResult inspectPdf(Path pdfFile) {
		try (PDDocument doc = Loader.loadPDF(pdfFile.toFile())) {
			if (doc.isEncrypted()) {
				return new AnalysisResult(true, "", true, 0, "Encrypted");
			}

			int pages = doc.getNumberOfPages();

			// 1. Check Producer
			String producer = "";
			PDDocumentInformation info = doc.getDocumentInformation();
			if (info != null) {
				producer = (info.getProducer() != null ? info.getProducer() : "") + " " +
						(info.getCreator() != null ? info.getCreator() : "");
			}
			String lcProducer = producer.toLowerCase(Locale.ROOT);
			boolean isKnownOcr = OCR_PRODUCERS.stream().anyMatch(lcProducer::contains);

			// 2. Extract Text (Fast Strip)
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setSortByPosition(true);

			if (pages > 20) {
				stripper.setEndPage(5);
			}

			String text = stripper.getText(doc).trim();

			// 3. Density Check
			int charCount = text.length();
			double density = pages > 0 ? (double) charCount / Math.min(pages, 5) : 0;

			boolean hasGoodTextLayer = (charCount > MIN_TEXT_CHARS && density > MIN_CHARS_PER_PAGE);

			if (isKnownOcr && charCount > 50) {
				hasGoodTextLayer = true;
			}

			if (hasGoodTextLayer && pages > 20) {
				stripper.setEndPage(Integer.MAX_VALUE);
				text = stripper.getText(doc).trim();
			}

			return new AnalysisResult(
					true,
					text,
					!hasGoodTextLayer, // Needs OCR if text layer is sparse
					pages,
					producer);

		} catch (IOException e) {
			log.error("PDF Inspection failed: {}", e.getMessage());
			return new AnalysisResult(false, "", true, 0, "Error");
		}
	}

	public Path convertTextToPdf(Path source, Path destination) throws IOException {
		Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
		return destination;
	}

	public record ConversionResult(Path path, String mimeType, String extension) {
	}

	public ConversionResult normalize(Path source, Path workingDir, String mimeType) {
		String originalExt = extensionService.getExtensionOrDefault(source);
		String finalExt = originalExt;

		if ("application/pdf".equals(mimeType)) {
			finalExt = ".pdf";
		} else {
			String mimeExt = extensionService.getExtensionForMimeType(mimeType);
			if (mimeExt != null && !mimeExt.isEmpty() && !".bin".equals(mimeExt)) {
				if (originalExt.equals(".bin") || originalExt.equals(".tmp") || originalExt.isEmpty()) {
					finalExt = mimeExt;
				}
			}
		}

		Path finalPath = source;

		if (!originalExt.equalsIgnoreCase(finalExt)) {
			String filename = source.getFileName().toString();
			String baseName = extensionService.removeExtension(filename);
			finalPath = workingDir.resolve(baseName + finalExt);

			try {
				if (!Files.exists(finalPath)) {
					Files.copy(source, finalPath, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				log.error("Failed to apply new extension {} to file {}", finalExt, filename, e);
				finalPath = source;
				finalExt = originalExt;
			}
		}

		return new ConversionResult(finalPath, mimeType, finalExt);
	}
}