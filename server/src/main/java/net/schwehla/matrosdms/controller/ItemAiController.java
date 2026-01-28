/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.ai.EAiInstruction;
import net.schwehla.matrosdms.domain.ai.EAiOutputFormat;
import net.schwehla.matrosdms.domain.ai.OllamaRequest;
import net.schwehla.matrosdms.domain.ai.OllamaResponse;
import net.schwehla.matrosdms.store.MatrosObjectStoreService;
import net.schwehla.matrosdms.util.TextLayerUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/items")
public class ItemAiController {

	@Autowired
	MatrosObjectStoreService storeService;
	@Autowired
	AppServerSpringConfig appConfig;

	@Autowired
	@Qualifier("ollamaRestTemplate")
	RestTemplate restTemplate;

	@GetMapping("/{uuid}/text")
	@Operation(summary = "Get the raw text content (cleaned)", description = "Returns the text layer stripped of XML tags")
	public ResponseEntity<String> getRawText(@PathVariable String uuid) {
		String rawXml = storeService.loadTextLayer(uuid);
		if (rawXml == null || rawXml.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(TextLayerUtils.extractCleanText(rawXml));
	}

	@PostMapping("/{uuid}/ai/transform")
	@Operation(summary = "Transform content using AI", description = "Generates summaries, tables, or extraction based on the instruction.")
	@Cacheable(value = "ai_markdown", key = "#uuid + '-' + #instruction.name() + '-' + #format.name()")
	public ResponseEntity<String> generateContent(
			@PathVariable String uuid,
			@Parameter(description = "What to do with the document") @RequestParam(defaultValue = "SUMMARY") EAiInstruction instruction,
			@Parameter(description = "Format of the result") @RequestParam(defaultValue = "MARKDOWN") EAiOutputFormat format) {

		// 1. Load Data
		String rawXml = storeService.loadTextLayer(uuid);
		if (rawXml == null || rawXml.isEmpty()) {
			return ResponseEntity.status(404).body("No text layer found for this item.");
		}

		String cleanText = TextLayerUtils.extractCleanText(rawXml);

		// Truncate to avoid context window overflow (approx 12k chars ~ 3k tokens)
		if (cleanText.length() > 12000)
			cleanText = cleanText.substring(0, 12000) + "\n...[truncated]";

		// 2. Build Structured Prompt
		String prompt = buildPrompt(instruction, format, cleanText);

		// 3. Call Ollama
		String url = appConfig.getAi().getChat().getUrl();
		String model = appConfig.getAi().getChat().getModel();

		if (url == null)
			url = appConfig.getAi().getClassification().getOllama().getUrl();
		if (model == null)
			model = appConfig.getAi().getClassification().getOllama().getModel();

		try {
			OllamaRequest req = new OllamaRequest(model, prompt, false);
			OllamaResponse resp = restTemplate.postForObject(url + "/api/generate", req, OllamaResponse.class);

			if (resp != null && resp.getResponse() != null) {
				return ResponseEntity.ok(resp.getResponse());
			}
		} catch (Exception e) {
			return ResponseEntity.status(503).body("AI Service Unavailable: " + e.getMessage());
		}

		return ResponseEntity.status(500).body("AI failed to generate response.");
	}

	private String buildPrompt(EAiInstruction instruction, EAiOutputFormat format, String content) {
		return """
				INSTRUCTION: %s
				FORMAT: %s

				CONTEXT:
				%s
				"""
				.formatted(instruction.getBasePrompt(), format.getFormatInstruction(), content);
	}
}
