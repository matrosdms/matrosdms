/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import jakarta.annotation.Nonnull;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import net.schwehla.matrosdms.domain.core.ERootCategory;

@Configuration
// @EnableWebMvc disabled else HATEOS
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		// This tells Spring: "If a class has @RestController, put /api in front of its
		// URLs"
		configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
				.addResourceHandler("/webjars/**")
				.addResourceLocations("/webjars/")
				.resourceChain(false);
	}

	@Override
	public void addFormatters(@Nonnull FormatterRegistry registry) {
		// Bridges Spring MVC (URL Params) to our O(1) Enum Lookup
		registry.addConverter(new StringToERootCategoryConverter());
	}

	public static class StringToERootCategoryConverter implements Converter<String, ERootCategory> {
		@Override
		public ERootCategory convert(@Nonnull String source) {
			return ERootCategory.fromString(source);
		}
	}
}
