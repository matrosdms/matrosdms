/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

@Configuration
public class JacksonConfig {

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

	@Bean
	Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {

		return builder -> builder
				.serializationInclusion(JsonInclude.Include.NON_ABSENT)
				.modules(new JavaTimeModule())
				.visibility(PropertyAccessor.FIELD, Visibility.ANY)
				.visibility(PropertyAccessor.GETTER, Visibility.NONE)
				.build();
	}

	@Bean
	@Primary
	ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {

		ObjectMapper objectMapper = builder.createXmlMapper(false).build();

		objectMapper
				.registerModule(new ParameterNamesModule())
				.registerModule(new Jdk8Module())
				.registerModule(new JavaTimeModule())
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		return objectMapper;
	}
}
