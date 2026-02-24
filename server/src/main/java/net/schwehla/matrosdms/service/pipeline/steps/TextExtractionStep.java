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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.inbox.EmailMetadata;
import net.schwehla.matrosdms.service.PdfConversionService;
import net.schwehla.matrosdms.service.PdfConversionService.ConversionResult;
import net.schwehla.matrosdms.service.PdfTextExtractor;
import net.schwehla.matrosdms.service.TikaService;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;
import net.schwehla.matrosdms.util.TextLayerBuilder;

@Component
@Order(4)
public class TextExtractionStep implements PipelineStep {

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

		String filename = ctx.getOriginalFile().getFileName().toString().toLowerCase();
		String finalXml = "";

		if (filename.endsWith(".eml")) {
			finalXml = extractEmailContent(ctx);
			ctx.setMimeType("message/rfc822");
			ctx.setExtension(".eml");
			// For emails, we store the original EML as the primary file
			ctx.setProcessedFile(ctx.getOriginalFile());
		} else {
			String mime = tikaService.detectMimeType(ctx.getOriginalFile());
			ConversionResult res = conversionService.normalize(ctx.getOriginalFile(), ctx.getWorkingDir(), mime);

			ctx.setProcessedFile(res.path());
			ctx.setExtension(res.extension());
			ctx.setMimeType(res.mimeType());

			String rawText = "";
			boolean isPdf = "application/pdf".equals(res.mimeType());

			if (isPdf && appConfig.getProcessing().isPreferScannerText()) {
				ctx.log("Checking for existing text layer...");
				String scannerText = pdfTextExtractor.quickExtract(res.path());

				if (scannerText.length() > 50) {
					rawText = scannerText;
					ctx.log("Text layer found (" + scannerText.length() + " chars). Skipping OCR.");
				} else {
					ctx.log("Insufficient text layer. Fallback to Tika/OCR.");
				}
			}

			if (rawText.isBlank()) {
				rawText = tikaService.extractText(res.path());
			}

			if (rawText == null || rawText.isBlank()) {
				ctx.addWarning("OCR: No text found.");
				rawText = "";
			}

			TextLayerBuilder builder = new TextLayerBuilder("FILE");
			builder.addMeta("filename", filename);
			builder.addMeta("processed_date", LocalDate.now().toString());
			builder.closeMeta();
			builder.addContent(rawText, res.mimeType());
			finalXml = builder.toString();
		}

		ctx.setExtractedText(finalXml);
		Files.writeString(
				ctx.getWorkingDir().resolve("textlayer.txt"), finalXml, StandardCharsets.UTF_8);
	}

	private String extractEmailContent(PipelineContext ctx) throws Exception {
		TextLayerBuilder xml = new TextLayerBuilder("EMAIL");
		EmailMetadata meta = ctx.getAiResult().getEmailMetadata();

		// 1. Add Structured Meta (for internal use)
		if (meta != null) {
			xml.addMeta("subject", meta.getSubject());
			xml.addMeta("sender", meta.getSender());
		}
		xml.closeMeta();

		// 2. Add Human-Readable Header to Body (for Search Index & Snippets)
		if (meta != null) {
			StringBuilder header = new StringBuilder();
			header.append("Subject: ").append(meta.getSubject()).append("\n");
			header.append("From: ").append(meta.getSender()).append("\n");
			if (meta.getRecipients() != null && !meta.getRecipients().isEmpty()) {
				header.append("To: ").append(String.join(", ", meta.getRecipients())).append("\n");
			}
			header.append("Date: ").append(meta.getSentDate()).append("\n");
			header.append("--------------------------------------------------\n");

			// This ensures "Subject" matches appear in the fulltext highlight
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
					// Simple HTML stripping for index
					text = text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
				}
				xml.addContent(text, entity.getMimeType());
			} else if (entity.getBody() instanceof BinaryBody) {
				String fname = entity.getFilename();
				if (fname == null)
					fname = "attachment";

				// Skip embedded resources (images/css we downloaded earlier)
				if (fname.startsWith("_embed_")) {
					return;
				}

				BinaryBody bb = (BinaryBody) entity.getBody();
				try (InputStream stream = bb.getInputStream()) {
					// Extract text from attachment (PDF, Doc, etc)
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