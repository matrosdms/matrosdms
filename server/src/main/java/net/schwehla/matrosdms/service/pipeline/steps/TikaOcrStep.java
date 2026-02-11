/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline.steps;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.service.PdfConversionService;
import net.schwehla.matrosdms.service.PdfConversionService.ConversionResult;
import net.schwehla.matrosdms.service.TikaService;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;
import net.schwehla.matrosdms.util.TextLayerBuilder;

@Component
@Order(5) // Fallback: Runs after TextExtractionStep (Order 4) for files without text
public class TikaOcrStep implements PipelineStep {

	@Autowired
	TikaService tikaService;
	@Autowired
	PdfConversionService conversionService;

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		if (ctx.getExtractedText() != null && !ctx.getExtractedText().isEmpty()) {
			return;
		}

		ctx.log("Detecting File Type...");
		String mime = tikaService.detectMimeType(ctx.getOriginalFile());

		ctx.log("Normalizing (PDF Conversion)...");
		ConversionResult res = conversionService.normalize(ctx.getOriginalFile(), ctx.getWorkingDir(), mime);

		ctx.setProcessedFile(res.path());
		ctx.setExtension(res.extension());
		ctx.setMimeType(res.mimeType());

		ctx.log("Extracting Text (OCR)...");
		String rawText = tikaService.extractText(res.path());

		if (rawText == null || rawText.isBlank()) {
			ctx.addWarning("OCR: No text found.");
			rawText = "";
		}

		TextLayerBuilder builder = new TextLayerBuilder(guessSource(res.path().getFileName().toString()));
		builder.addMeta("filename", ctx.getOriginalFile().getFileName().toString());
		builder.addMeta("processed_date", LocalDate.now().toString());
		builder.closeMeta();

		builder.addContent(rawText, res.mimeType());

		String finalXml = builder.toString();
		ctx.setExtractedText(finalXml);

		Files.writeString(
				ctx.getWorkingDir().resolve("textlayer.txt"), finalXml, StandardCharsets.UTF_8);
	}

	private String guessSource(String filename) {
		if (filename.toLowerCase().startsWith("scan"))
			return "SCAN";
		return "UPLOAD";
	}
}
