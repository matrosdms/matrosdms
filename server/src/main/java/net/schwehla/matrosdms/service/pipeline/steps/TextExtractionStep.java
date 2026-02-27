/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline.steps;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.inbox.EmailMetadata;
import net.schwehla.matrosdms.service.PdfConversionService;
import net.schwehla.matrosdms.service.PdfConversionService.AnalysisResult;
import net.schwehla.matrosdms.service.PdfConversionService.ConversionResult;
import net.schwehla.matrosdms.service.PdfTextExtractor;
import net.schwehla.matrosdms.service.TikaService;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;
import net.schwehla.matrosdms.util.TextLayerBuilder;

@Component
@Order(4)
public class TextExtractionStep implements PipelineStep {

	private static final Logger log = LoggerFactory.getLogger(TextExtractionStep.class);

	@Autowired
	TikaService tikaService;
	@Autowired
	PdfConversionService conversionService;
	@Autowired
	PdfTextExtractor pdfTextExtractor;
	@Autowired
	AppServerSpringConfig appConfig;

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		ctx.log("Extracting Content/Text...");

		String originalFilename = ctx.getCurrentState().getFileInfo().getOriginalFilename();
		if (originalFilename == null || originalFilename.isBlank()) {
			originalFilename = ctx.getOriginalFile().getFileName().toString();
		}

		String extension = ctx.getExtension() != null ? ctx.getExtension().toLowerCase() : "";
		String finalXml = "";

		if (originalFilename.toLowerCase().endsWith(".eml") || ".eml".equals(extension)) {
			finalXml = extractEmailContent(ctx, originalFilename);
			ctx.setMimeType("message/rfc822");
			ctx.setExtension(".eml");
			ctx.setProcessedFile(ctx.getOriginalFile());
		} else {

			String mime = ctx.getMimeType();
			if (mime == null) {
				mime = tikaService.detectMimeType(ctx.getOriginalFile());
			}

			ConversionResult res = conversionService.normalize(ctx.getOriginalFile(), ctx.getWorkingDir(), mime);

			ctx.setProcessedFile(res.path());
			ctx.setExtension(res.extension());
			ctx.setMimeType(res.mimeType());

			String rawText = "";
			boolean isPdf = "application/pdf".equals(res.mimeType());

			// 1. Smart PDF Inspection (Replaces the dumb > 50 chars shortcut)
			if (isPdf && appConfig.getProcessing().isPreferScannerText()) {
				ctx.log("Inspecting PDF text layer density...");
				AnalysisResult analysis = conversionService.inspectPdf(res.path());

				if (!analysis.needsOcr()) {
					rawText = analysis.extractedText();
					ctx.log(String.format("Good text layer found (%d chars, %d pages). Skipping OCR.",
							rawText.length(), analysis.pageCount()));
				} else {
					ctx.log(String.format("Insufficient text layer (%d chars, %d pages). Forcing Tika/OCR.",
							analysis.extractedText().length(), analysis.pageCount()));
				}
			}

			if ("text/plain".equals(res.mimeType())) {
				ctx.log("Reading plain text file directly...");
				try {
					rawText = Files.readString(res.path(), StandardCharsets.UTF_8);
				} catch (Exception e) {
					log.warn("Failed to read text file as UTF-8, falling back to Tika");
				}
			}

			// 2. Fallback to full Tika OCR if the smart inspector flagged needsOcr = true
			if (rawText == null || rawText.isBlank()) {
				ctx.log("Performing full text extraction / OCR...");
				rawText = tikaService.extractText(res.path());
			}

			if (rawText == null || rawText.isBlank()) {
				ctx.addWarning("No text or OCR content could be extracted.");
				rawText = "";
			} else {
				log.info("Extracted {} characters from {}", rawText.length(), originalFilename);
			}

			TextLayerBuilder builder = new TextLayerBuilder("FILE");
			builder.addMeta("filename", originalFilename);
			builder.addMeta("processed_date", LocalDate.now().toString());
			builder.closeMeta();
			builder.addContent(rawText, res.mimeType());
			finalXml = builder.toString();
		}

		ctx.setExtractedText(finalXml);
		Files.writeString(
				ctx.getWorkingDir().resolve("textlayer.txt"), finalXml, StandardCharsets.UTF_8);
	}

	private String extractEmailContent(PipelineContext ctx, String originalFilename) throws Exception {
		TextLayerBuilder xml = new TextLayerBuilder("EMAIL");
		EmailMetadata meta = ctx.getAiResult().getEmailMetadata();

		xml.addMeta("filename", originalFilename);
		xml.addMeta("processed_date", LocalDate.now().toString());
		if (meta != null) {
			xml.addMeta("subject", meta.getSubject());
			xml.addMeta("sender", meta.getSender());
		}
		xml.closeMeta();

		if (meta != null) {
			StringBuilder header = new StringBuilder();
			header.append("Subject: ").append(meta.getSubject()).append("\n");
			header.append("From: ").append(meta.getSender()).append("\n");
			if (meta.getRecipients() != null && !meta.getRecipients().isEmpty()) {
				header.append("To: ").append(String.join(", ", meta.getRecipients())).append("\n");
			}
			header.append("Date: ").append(meta.getSentDate()).append("\n");
			header.append("--------------------------------------------------\n");
			xml.addContent(header.toString(), "text/plain");
		}

		DefaultMessageBuilder builder = new DefaultMessageBuilder();
		try (InputStream is = new FileInputStream(ctx.getOriginalFile().toFile())) {
			Message message = builder.parseMessage(is);
			extractRecursive(message, xml, ctx);
		}
		return xml.toString();
	}

	private void extractRecursive(Entity entity, TextLayerBuilder xml, PipelineContext ctx) {
		try {
			if (entity.getBody() instanceof Multipart) {
				Multipart mp = (Multipart) entity.getBody();
				for (Entity part : mp.getBodyParts())
					extractRecursive(part, xml, ctx);
			} else if (entity.getBody() instanceof TextBody) {
				TextBody tb = (TextBody) entity.getBody();
				String text = new String(tb.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
				if (entity.getMimeType().contains("html")) {
					text = text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
				}
				xml.addContent(text, entity.getMimeType());
			} else if (entity.getBody() instanceof BinaryBody) {
				String fname = entity.getFilename();
				if (fname == null)
					fname = "attachment";

				if (fname.startsWith("_embed_")) {
					return;
				}

				BinaryBody bb = (BinaryBody) entity.getBody();
				try (InputStream stream = bb.getInputStream()) {
					String extracted = tikaService.extractText(stream);
					if (extracted != null && !extracted.isBlank()) {
						xml.addAttachment(fname, extracted);
					}
				} catch (Exception e) {
					ctx.addWarning("Attachment extraction failed: " + fname);
				}
			}
		} catch (Exception e) {
			/* log */
		}
	}
}