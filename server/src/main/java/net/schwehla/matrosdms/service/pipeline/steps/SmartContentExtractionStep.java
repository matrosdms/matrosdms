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

import net.schwehla.matrosdms.service.PdfConversionService;
import net.schwehla.matrosdms.service.PdfConversionService.AnalysisResult;
import net.schwehla.matrosdms.service.TikaService;
import net.schwehla.matrosdms.service.ocr.OcrService;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;
import net.schwehla.matrosdms.util.TextLayerBuilder;

/**
 * DISABLED: This logic has been merged into TextExtractionStep to prevent 
 * redundant processing and overwriting of the text layer.
 * 
 * Removed @Component and @Order annotations.
 */
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
		// No-op. Logic handled by TextExtractionStep.
	}
}