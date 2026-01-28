/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.io.IOException;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SpaRedirectFilterConfiguration {
	private final Logger LOGGER = LoggerFactory.getLogger(SpaRedirectFilterConfiguration.class);

	@Bean
	public FilterRegistrationBean<OncePerRequestFilter> spaRedirectFiler() {
		FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(createRedirectFilter());
		registration.addUrlPatterns("/*");
		registration.setName("frontendRedirectFiler");
		registration.setOrder(1);
		return registration;
	}

	private OncePerRequestFilter createRedirectFilter() {
		return new OncePerRequestFilter() {
			// FIX: Added "|/assets" to the exclusion list below
			private final String REGEX = "(?!/actuator|/api|/ws|/_nuxt|/static|/assets|/index\\.html|/200\\.html|/favicon\\.ico|/sw\\.js|/swagger-ui|/v3/api-docs|/api-docs).*$";
			private Pattern pattern = Pattern.compile(REGEX);

			@Override
			protected void doFilterInternal(
					HttpServletRequest req, HttpServletResponse res, FilterChain chain)
					throws ServletException, IOException {
				if (pattern.matcher(req.getRequestURI()).matches() && !req.getRequestURI().equals("/")) {
					// LOGGER.debug("URL {} entered directly into the Browser, redirecting...",
					// req.getRequestURI());
					RequestDispatcher rd = req.getRequestDispatcher("/");
					rd.forward(req, res);
				} else {
					chain.doFilter(req, res);
				}
			}
		};
	}
}
