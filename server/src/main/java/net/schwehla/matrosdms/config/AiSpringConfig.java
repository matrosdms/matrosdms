/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiSpringConfig {

	/**
	 * Specialized RestTemplate for AI operations. AI models (LLMs) are slow, so we
	 * need significantly
	 * higher read timeouts (5 minutes) compared to standard microservice calls.
	 */
	@Bean(name = "ollamaRestTemplate")
	public RestTemplate ollamaRestTemplate(RestTemplateBuilder builder) {
		return builder
				.connectTimeout(Duration.ofSeconds(10)) // Handshake timeout
				.readTimeout(Duration.ofMinutes(10)) // Wait time for generation
				.build();
	}
}
