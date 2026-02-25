/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.service.FileSignatureService;
import net.schwehla.matrosdms.service.TikaService;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;

@Component
@Order(1) // Runs VERY early (Phase 1)
public class SignatureAnalysisStep implements PipelineStep {

	private static final Logger log = LoggerFactory.getLogger(SignatureAnalysisStep.class);

	@Autowired
	FileSignatureService signatureService;

	@Autowired
	TikaService tikaService;

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		ctx.log("Analyzing file signature...");

		// 1. Try Magic Bytes (Instant)
		String mime = signatureService.quickDetect(ctx.getOriginalFile());

		// 2. Fallback to Tika (Slow but accurate)
		if (mime == null) {
			log.debug("Magic bytes ambiguous, falling back to Tika for {}", ctx.getHash());
			mime = tikaService.detectMimeType(ctx.getOriginalFile());
		}

		if (mime == null)
			mime = "application/octet-stream";

		log.info("Detected MIME: {}", mime);
		ctx.setMimeType(mime);
		
		// Extension fixing is now safely handled downstream in PdfConversionService.normalize()
	}
}