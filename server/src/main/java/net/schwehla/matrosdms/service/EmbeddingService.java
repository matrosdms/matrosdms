/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;

@Service
public class EmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

	@Autowired
	AppServerSpringConfig appConfig;

	@Autowired
	@Qualifier("ollamaRestTemplate")
	RestTemplate restTemplate;

	public float[] generateEmbedding(String text) {
		if (text == null || text.isBlank())
			return null;

		String url = appConfig.getAi().getEmbedding().getUrl();
		String model = appConfig.getAi().getEmbedding().getModel();

		if (url == null || model == null) {
			// Fallback if config is missing to prevent crash
			return null;
		}

		// Simple JSON Map for Request
		Map<String, Object> request = Map.of(
				"model", model,
				"prompt", text);

		try {
			// Ollama response format: { "embedding": [0.1, 0.2, ...] }
			Map response = restTemplate.postForObject(url + "/api/embeddings", request, Map.class);

			if (response != null && response.containsKey("embedding")) {
				List<Double> vectorList = (List<Double>) response.get("embedding");

				if (vectorList != null) {
					float[] vector = new float[vectorList.size()];
					for (int i = 0; i < vectorList.size(); i++) {
						vector[i] = vectorList.get(i).floatValue();
					}
					return vector;
				}
			}
		} catch (Exception e) {
			log.error("Failed to generate embedding: {}", e.getMessage());
		}
		return null;
	}
}
