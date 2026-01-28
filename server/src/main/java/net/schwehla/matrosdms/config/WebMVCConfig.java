/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMVCConfig implements WebMvcConfigurer {

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {

		// Attention, order is important
		converters.add(new org.springframework.http.converter.ByteArrayHttpMessageConverter());
		converters.add(new org.springframework.http.converter.ResourceHttpMessageConverter());
		converters.add(new org.springframework.http.converter.FormHttpMessageConverter());
		converters.add(new MappingJackson2HttpMessageConverter());
		WebMvcConfigurer.super.configureMessageConverters(converters);
	}
}
