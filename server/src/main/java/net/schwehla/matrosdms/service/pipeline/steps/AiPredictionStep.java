/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline.steps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.service.PredictionService;
import net.schwehla.matrosdms.service.message.DigestResultMessage;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;

@Component
@Order(6)
public class AiPredictionStep implements PipelineStep {

	@Autowired
	PredictionService predictionService;

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		ctx.log("AI Classification...");

		DigestResultMessage result = ctx.getAiResult();

		result.setSha256(ctx.getHash());
		result.setMimeType(ctx.getMimeType());

		String text = ctx.getExtractedText();
		String filename = ctx.getDisplayFilename();

		if (text != null && !text.isBlank()) {
			try {
				predictionService.predictMetadata(text, filename, result);

				if (result.getPrediction() != null) {
					ctx.getCurrentState().setPrediction(result.getPrediction());
				}

			} catch (Exception e) {
				ctx.addWarning("AI Error: " + e.getMessage());
			}
		} else {
			ctx.addWarning("AI Skipped: No text.");
		}
	}
}
