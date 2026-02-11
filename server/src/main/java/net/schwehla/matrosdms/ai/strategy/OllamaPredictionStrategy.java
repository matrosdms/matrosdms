/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.ai.strategy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.ai.IPredictionStrategy;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.ai.AiClassificationResult;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates.Candidate;
import net.schwehla.matrosdms.domain.ai.OllamaRequest;
import net.schwehla.matrosdms.domain.ai.OllamaResponse;
import net.schwehla.matrosdms.domain.inbox.Prediction;
import net.schwehla.matrosdms.service.message.DigestResultMessage;

@Component
public class OllamaPredictionStrategy implements IPredictionStrategy {

	private static final Logger log = LoggerFactory.getLogger(OllamaPredictionStrategy.class);

	private Semaphore gpuLock;
	private final RestTemplate restTemplate;
	private final ObjectMapper jsonMapper;

	@Autowired
	AppServerSpringConfig appConfig;

	@Value("classpath:ai/prompt-classification.txt")
	private Resource promptResource;

	private String promptTemplate;

	public OllamaPredictionStrategy(
			@Qualifier("ollamaRestTemplate") RestTemplate restTemplate, ObjectMapper mapper) {
		this.restTemplate = restTemplate;
		this.jsonMapper = mapper;
	}

	@PostConstruct
	public void init() {
        // OPTIMIZATION: Configurable Concurrency
        int permits = appConfig.getAi().getConcurrency();
        if (permits < 1) permits = 1;
        this.gpuLock = new Semaphore(permits);
        log.info("AI: Ollama Concurrency Level: {}", permits);

		try {
			promptTemplate = new String(promptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Failed to load AI prompt", e);
		}
	}

	@Override
	public String getId() {
		return "ollama";
	}

	@Override
	public void analyze(
			String fullText,
			String filename,
			ClassificationCandidates candidates,
			DigestResultMessage result) {
		try {
			log.debug("AI: Waiting for slot...");
			gpuLock.acquire();

			String url = appConfig.getAi().getClassification().getOllama().getUrl();
			String model = appConfig.getAi().getClassification().getOllama().getModel();

			log.info("AI: Processing {} with model {} at {}", filename, model, url);

			String prompt = buildPrompt(fullText, filename, candidates);
			OllamaResponse response = callOllama(url, model, prompt);

			if (response != null && response.getResponse() != null) {
				parseJsonResult(response.getResponse(), result);
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.error("AI: Ollama failure", e);
		} finally {
			gpuLock.release();
		}
	}

	private OllamaResponse callOllama(String url, String model, String prompt) {
		OllamaRequest req = new OllamaRequest(model, prompt, false);
		try {
			ResponseEntity<OllamaResponse> resp = restTemplate.postForEntity(url + "/api/generate", req,
					OllamaResponse.class);
			return resp.getBody();
		} catch (Exception e) {
			log.error("Failed to connect to Ollama at {}: {}", url, e.getMessage());
			return null;
		}
	}

	private void parseJsonResult(String json, DigestResultMessage result) {
		try {
			int start = json.indexOf("{");
			int end = json.lastIndexOf("}");
			if (start >= 0 && end > start) {
				String cleanJson = json.substring(start, end + 1);
				AiClassificationResult dto = jsonMapper.readValue(cleanJson, AiClassificationResult.class);

				Prediction p = result.getPrediction();
				p.setContext(dto.getContextUuid());
				p.setCategory(dto.getCategoryUuid());
				p.setSummary(dto.getSummary());

				if (dto.getCustomAttributes() != null) {
					p.setAttributes(dto.getCustomAttributes());
				}

				if (dto.getDate() != null) {
					try {
						p.setDocumentDate(java.time.LocalDate.parse(dto.getDate()));
					} catch (Exception ignored) {
					}
				}
			}
		} catch (JsonProcessingException e) {
			log.warn("AI: Failed to parse JSON response. Raw: {}", json);
		}
	}

	private String buildPrompt(String text, String filename, ClassificationCandidates candidates) {
		String safeText = text.length() > 6000 ? text.substring(0, 6000) : text;
		String contextList = formatList(candidates.contexts());
		String categoryList = formatList(candidates.categories());
		return promptTemplate.formatted(contextList, categoryList, filename, safeText);
	}

	private String formatList(java.util.List<Candidate> list) {
		return list.stream()
				.map(c -> String.format("- %s (ID: %s) : %s", c.name(), c.uuid(), c.description()))
				.collect(Collectors.joining("\n"));
	}
}