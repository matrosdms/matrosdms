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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.service.PdfConversionService;
import net.schwehla.matrosdms.service.PdfConversionService.AnalysisResult;
import net.schwehla.matrosdms.service.TikaService;
import net.schwehla.matrosdms.service.ocr.OcrService;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;
import net.schwehla.matrosdms.util.TextLayerBuilder;

@Component
@Order(4) 
public class SmartContentExtractionStep implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(SmartContentExtractionStep.class);

    @Autowired
    PdfConversionService pdfService;
    
    @Autowired
    TikaService tikaService;
    
    @Autowired
    OcrService ocrService; // Uses TikaOcrProvider

    @Override
    public void execute(PipelineContext ctx) throws Exception {
        String mime = ctx.getMimeType(); 
        String text = "";
        String extractionMethod = "NONE";

        // === STRATEGY 1: Email ===
        if ("message/rfc822".equals(mime) || ctx.getOriginalFile().toString().endsWith(".eml")) {
            return; 
        }

        // === STRATEGY 2: PDF Smart Inspection ===
        if ("application/pdf".equals(mime)) {
            ctx.log("Inspecting PDF Structure...");
            
            AnalysisResult analysis = pdfService.inspectPdf(ctx.getOriginalFile());
            ctx.setPdfAnalysis(analysis);

            if (!analysis.needsOcr()) {
                // Digital Native PDF
                ctx.log("Using existing text layer");
                text = analysis.extractedText();
                extractionMethod = "NATIVE_PDF";
            } else {
                // Scanned PDF -> Send to OCR Service (Tika)
                ctx.log("Performing OCR/Extraction via Tika...");
                text = ocrService.extractText(ctx.getOriginalFile(), mime);
                extractionMethod = "OCR_TIKA";
            }
        }
        // === STRATEGY 3: Images & Office ===
        else {
            ctx.log("Extracting content...");
            text = ocrService.extractText(ctx.getOriginalFile(), mime);
            extractionMethod = "TIKA_GENERIC";
        }

        // === FINALIZE ===
        if (text == null || text.isBlank()) {
            ctx.addWarning("No text content extracted.");
            text = "";
        } else {
            log.info("Extracted {} chars using {}", text.length(), extractionMethod);
        }

        TextLayerBuilder builder = new TextLayerBuilder(extractionMethod);
        builder.addMeta("filename", ctx.getDisplayFilename());
        builder.addMeta("processed_date", LocalDate.now().toString());
        builder.closeMeta();
        builder.addContent(text, mime);

        String finalXml = builder.toString();
        ctx.setExtractedText(finalXml);

        Files.writeString(
                ctx.getWorkingDir().resolve("textlayer.txt"), finalXml, StandardCharsets.UTF_8);
    }
}