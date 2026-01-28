/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.ai.OllamaRequest;
import net.schwehla.matrosdms.domain.ai.OllamaResponse;
import net.schwehla.matrosdms.domain.search.MSearchResult;
import net.schwehla.matrosdms.search.SearchCriteria;
import net.schwehla.matrosdms.store.StoreContext;

@Service
public class RAGService {

	private static final Logger log = LoggerFactory.getLogger(RAGService.class);

	@Autowired
	SearchService searchService;
	@Autowired
	AppServerSpringConfig appConfig;

	@Autowired
	@Qualifier("ollamaRestTemplate")
	RestTemplate restTemplate;

	@Value("classpath:ai/prompt-chat.txt")
	private Resource promptResource;

	private String promptTemplate;

	@PostConstruct
	public void init() {
		try {
			promptTemplate = new String(promptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Failed to load AI Chat prompt", e);
			promptTemplate = "Context:\n%s\n\nQuestion: %s";
		}
	}

	public String chat(String message) {
		// Fix: Wrap string in SearchCriteria
		List<MSearchResult> hits = searchService.search(SearchCriteria.forText(message), 0, 3).getContent();

		if (hits.isEmpty()) {
			return "I couldn't find any documents related to your question.";
		}

		StringBuilder contextBuilder = new StringBuilder();
		for (MSearchResult hit : hits) {
			String content = StoreContext.readTextFile(hit.getUuid());
			if (content.length() > 3000)
				content = content.substring(0, 3000) + "...";
			contextBuilder.append("--- DOCUMENT: ").append(hit.getName()).append(" ---\n");
			contextBuilder.append(content).append("\n\n");
		}

		String fullPrompt = promptTemplate.formatted(contextBuilder.toString(), message);
		String url = appConfig.getAi().getChat().getUrl();
		String model = appConfig.getAi().getChat().getModel();

		if (url == null)
			url = appConfig.getAi().getClassification().getOllama().getUrl();
		if (model == null)
			model = appConfig.getAi().getClassification().getOllama().getModel();

		try {
			OllamaRequest req = new OllamaRequest(model, fullPrompt, false);
			ResponseEntity<OllamaResponse> resp = restTemplate.postForEntity(url + "/api/generate", req,
					OllamaResponse.class);
			if (resp.getBody() != null)
				return resp.getBody().getResponse();
		} catch (Exception e) {
			log.error("AI Chat failed", e);
			return "I encountered an error (" + e.getMessage() + ")";
		}
		return "No response from AI.";
	}
}
