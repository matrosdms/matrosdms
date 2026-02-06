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
import net.schwehla.matrosdms.service.PdfConversionService;
import net.schwehla.matrosdms.service.PdfConversionService.ConversionResult;
import net.schwehla.matrosdms.service.PdfTextExtractor;
import net.schwehla.matrosdms.service.TikaService;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;
import net.schwehla.matrosdms.util.TextLayerBuilder;

@Component
@Order(3)
public class TextExtractionStep implements PipelineStep {

	@Autowired
	TikaService tikaService;
	@Autowired
	PdfConversionService conversionService;
	@Autowired
	PdfTextExtractor pdfTextExtractor; // NEW
	@Autowired
	AppServerSpringConfig appConfig; // NEW

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		ctx.log("Extracting Content/Text...");

		String filename = ctx.getOriginalFile().getFileName().toString().toLowerCase();
		String finalXml = "";

		if (filename.endsWith(".eml")) {
			finalXml = extractEmailContent(ctx);
			ctx.setMimeType("message/rfc822");
			ctx.setExtension(".eml");
			ctx.setProcessedFile(ctx.getOriginalFile());
		} else {
			String mime = tikaService.detectMimeType(ctx.getOriginalFile());
			ConversionResult res = conversionService.normalize(ctx.getOriginalFile(), ctx.getWorkingDir(), mime);

			ctx.setProcessedFile(res.path());
			ctx.setExtension(res.extension());
			ctx.setMimeType(res.mimeType());

			String rawText = "";
			boolean isPdf = "application/pdf".equals(res.mimeType());

			// --- SMART OCR LOGIC START ---
			if (isPdf && appConfig.getProcessing().isPreferScannerText()) {
				ctx.log("Checking for existing text layer...");
				String scannerText = pdfTextExtractor.quickExtract(res.path());

				// Threshold: If we found > 50 characters, assume scanner OCR is good
				if (scannerText.length() > 50) {
					rawText = scannerText;
					ctx.log("Text layer found (" + scannerText.length() + " chars). Skipping OCR.");
				} else {
					ctx.log("Insufficient text layer. Fallback to Tika/OCR.");
				}
			}
			// --- SMART OCR LOGIC END ---

			// Fallback to Tika if Quick Extract failed or file is image
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
		if (ctx.getAiResult() != null && ctx.getAiResult().getEmailMetadata() != null) {
			xml.addMeta("subject", ctx.getAiResult().getEmailMetadata().getSubject());
			xml.addMeta("sender", ctx.getAiResult().getEmailMetadata().getSender());
		}
		xml.closeMeta();

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

				BinaryBody bb = (BinaryBody) entity.getBody();
				try (InputStream stream = bb.getInputStream()) {
					String extracted = tikaService.extractText(stream);
					if (!extracted.isBlank())
						xml.addAttachment(fname, extracted);
				} catch (Exception e) {
					ctx.addWarning("Attachment extraction failed: " + fname);
				}
			}
		} catch (Exception e) {
			/* log */
		}
	}
}