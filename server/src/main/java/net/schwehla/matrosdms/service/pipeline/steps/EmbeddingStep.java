/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline.steps;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.domain.inbox.Prediction;
import net.schwehla.matrosdms.service.EmbeddingService;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;

/**
 * Generates semantic embeddings for vector search (pgvector).
 * Disabled by default - enable when vector DB is configured.
 * 
 * Enable via: matros.ai.embedding.enabled=true
 */
@Component
@Order(7)
public class EmbeddingStep implements PipelineStep {

	private static final Logger log = LoggerFactory.getLogger(EmbeddingStep.class);

	@Value("${matros.ai.embedding.enabled:false}")
	private boolean embeddingEnabled;

	@Autowired
	EmbeddingService embeddingService;

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		if (!embeddingEnabled) {
			log.debug("Embedding step skipped (disabled)");
			return;
		}

		StringBuilder prompt = new StringBuilder();

		if (ctx.getAiResult() != null && ctx.getAiResult().getPrediction() != null
				&& ctx.getAiResult().getPrediction().getSummary() != null) {
			prompt.append(ctx.getAiResult().getPrediction().getSummary()).append(" ");
		}

		String text = ctx.getExtractedText();
		if (text != null) {
			prompt.append(text.substring(0, Math.min(text.length(), 2000)));
		}

		if (prompt.length() > 0) {
			ctx.log("Generating Semantic Vector...");
			float[] vector = embeddingService.generateEmbedding(prompt.toString());

			if (ctx.getAiResult() != null && vector != null) {
				Prediction p = ctx.getAiResult().getPrediction();
				if (p.getAttributes() == null) {
					p.setAttributes(new HashMap<>());
				}
				p.getAttributes().put("_vector", vector);
				log.info("Generated embedding with {} dimensions", vector.length);
			}
		}
	}
}
