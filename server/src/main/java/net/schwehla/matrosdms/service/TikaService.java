/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy
public class TikaService {

	private static final Logger log = LoggerFactory.getLogger(TikaService.class);
	private Tika tika;
	private MimeTypes mimeRepository;

	private Tika getTika() {
		if (this.tika == null) {
			this.tika = new Tika();
			this.tika.setMaxStringLength(10 * 1024 * 1024); // 10MB text limit
		}
		return this.tika;
	}

	private MimeTypes getMimeRepository() {
		if (this.mimeRepository == null) {
			this.mimeRepository = TikaConfig.getDefaultConfig().getMimeRepository();
		}
		return this.mimeRepository;
	}

	public String detectMimeType(Path file) {
		try {
			return getTika().detect(file);
		} catch (IOException e) {
			log.warn("Mime detection failed: {}", e.getMessage());
			return "application/octet-stream";
		}
	}

	public String detectMimeType(byte[] data) {
		try {
			return getTika().detect(data);
		} catch (Exception e) {
			log.warn("Mime detection failed: {}", e.getMessage());
			return "application/octet-stream";
		}
	}

	/**
	 * Resolves the preferred file extension for a given MIME type string.
	 * e.g., "application/pdf" -> ".pdf"
	 */
	public String getExtensionForMimeType(String mimeType) {
		if (mimeType == null || mimeType.isBlank())
			return ".bin";
		try {
			MimeType type = getMimeRepository().forName(mimeType);
			String ext = type.getExtension();
			return (ext == null || ext.isEmpty()) ? ".bin" : ext;
		} catch (Exception e) {
			log.warn("Failed to resolve extension for mime: {}", mimeType);
			return ".bin";
		}
	}

	public String extractText(Path file) {
		try (InputStream stream = new BufferedInputStream(Files.newInputStream(file))) {
			// PDF Fallback check - PDFBox is often better for layout preservation
			if (file.toString().toLowerCase().endsWith(".pdf")) {
				String text = extractPdfBox(file);
				if (text != null && !text.isBlank())
					return text;
			}
			return extractText(stream);
		} catch (Exception e) {
			log.warn("Extraction failed for {}: {}", file.getFileName(), e.getMessage());
			return "";
		}
	}

	public String extractText(InputStream stream) {
		try {
			AutoDetectParser parser = new AutoDetectParser();
			BodyContentHandler handler = new BodyContentHandler(-1); // -1 disables the 100k length limit
			Metadata metadata = new Metadata();
			ParseContext context = new ParseContext();

			// 1. Configure PDF OCR Strategy (Crucial for scanned PDFs)
			PDFParserConfig pdfConfig = new PDFParserConfig();
			pdfConfig.setExtractInlineImages(true);
			pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION);
			context.set(PDFParserConfig.class, pdfConfig);

			// 2. Configure Tesseract to use standard languages (German + English)
			TesseractOCRConfig ocrConfig = new TesseractOCRConfig();
			ocrConfig.setLanguage("deu+eng");
			context.set(TesseractOCRConfig.class, ocrConfig);

			parser.parse(stream, handler, metadata, context);
			return handler.toString().trim();
		} catch (Exception e) {
			log.warn("In-memory extraction failed: {}", e.getMessage());
			return "";
		}
	}

	private String extractPdfBox(Path file) {
		try (PDDocument document = Loader.loadPDF(file.toFile())) {
			if (document.isEncrypted())
				return "";
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setSortByPosition(true);
			return stripper.getText(document);
		} catch (IOException e) {
			return "";
		}
	}
}