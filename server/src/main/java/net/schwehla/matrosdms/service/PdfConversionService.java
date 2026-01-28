/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;

@Service
@Lazy
public class PdfConversionService {

	private static final Logger log = LoggerFactory.getLogger(PdfConversionService.class);

	// Limit concurrent conversions
	private final Semaphore conversionSemaphore = new Semaphore(2);

	@Autowired
	AppServerSpringConfig appConfig;

	public record ConversionResult(Path path, boolean isPdf, String extension, String mimeType) {
	}

	public ConversionResult normalize(Path source, Path tempDir, String originalMimeType)
			throws IOException {

		log.debug("Normalizing file: {} (Mime: {})", source.getFileName(), originalMimeType);

		// 1. Text Files -> PDF (if enabled)
		if (originalMimeType.startsWith("text/") && appConfig.getProcessing().isConvertTextToPdf()) {
			try {
				conversionSemaphore.acquire();
				Path target = tempDir.resolve("processed.pdf");
				convertTextToPdf(source, target);
				return new ConversionResult(target, true, ".pdf", "application/pdf");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Conversion interrupted", e);
			} finally {
				conversionSemaphore.release();
			}
		}

		// 2. Determine Extension (Fallback to Tika if no extension in filename)
		String ext;
		if (originalMimeType.equals("application/pdf")) {
			ext = ".pdf";
		} else {
			ext = getExtension(source, originalMimeType);
		}

		// 3. Passthrough for Binary Files (Images, Office, PDF, etc.)
		// This ensures JPG, PNG, DOCX, XLSX are kept as-is and stored safely.
		Path target = tempDir.resolve("processed" + ext);
		Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

		boolean isPdf = originalMimeType.equals("application/pdf");
		return new ConversionResult(target, isPdf, ext, originalMimeType);
	}

	private String getExtension(Path path, String mimeType) {
		String name = path.getFileName().toString();
		int i = name.lastIndexOf('.');

		// If extension exists in filename, use it
		if (i > 0 && i < name.length() - 1) {
			return name.substring(i);
		}

		// Fallback: Guess extension from MimeType using Tika logic
		try {
			MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
			MimeType type = allTypes.forName(mimeType);
			if (type != null && !type.getExtension().isEmpty()) {
				return type.getExtension();
			}
		} catch (Exception ignored) {
			// Ignore Tika errors
		}

		return ".bin";
	}

	private void convertTextToPdf(Path source, Path destination) throws IOException {
		Files.deleteIfExists(destination);
		try (PDDocument doc = new PDDocument()) {
			PDPage page = new PDPage();
			doc.addPage(page);
			try (PDPageContentStream contentStream = new PDPageContentStream(doc, page);
					BufferedReader reader = Files.newBufferedReader(source)) {
				contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				contentStream.beginText();
				contentStream.newLineAtOffset(50, 700);
				contentStream.setLeading(14.5f);
				String line;
				while ((line = reader.readLine()) != null) {
					// Filter control characters to prevent PDFBox crash
					contentStream.showText(line.replaceAll("[\\p{Cc}&&[^\r\n\t]]", ""));
					contentStream.newLine();
				}
				contentStream.endText();
			}
			doc.save(destination.toFile());
		}
	}
}
