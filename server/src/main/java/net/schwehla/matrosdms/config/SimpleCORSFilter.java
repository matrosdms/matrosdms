/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // CRITICAL: Run before Spring Security
public class SimpleCORSFilter implements Filter {

	private final Logger log = LoggerFactory.getLogger(SimpleCORSFilter.class);

	/**
	 * SECURITY: fixed allowlist instead of echoing the caller's Origin.
	 * Reflecting any Origin combined with Allow-Credentials=true lets every
	 * website script this API with the victim's stored credentials.
	 * In production the SPA is served same-origin (no Origin mismatch), so the
	 * defaults only need to cover local Vite dev servers.
	 */
	private final Set<String> allowedOrigins;

	public SimpleCORSFilter(
			@Value("${matros.security.allowed-origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173}") String origins) {
		this.allowedOrigins = Arrays.stream(origins.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toSet());
		log.info("SimpleCORSFilter init, allowed origins: {}", allowedOrigins);
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		HttpServletResponse response = (HttpServletResponse) res;
		HttpServletRequest request = (HttpServletRequest) req;

		String origin = request.getHeader("Origin");

		// Same-origin requests and non-browser clients send no Origin header:
		// nothing to do. Unknown origins get no CORS headers -> browser blocks.
		if (origin != null && allowedOrigins.contains(origin)) {
			response.setHeader("Access-Control-Allow-Origin", origin);
			response.setHeader("Vary", "Origin");
			response.setHeader("Access-Control-Allow-Credentials", "true");
			response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT, PATCH");
			response.setHeader(
					"Access-Control-Allow-Headers",
					"Content-Type, Accept, X-Requested-With, Authorization, X-MATROS-USER, Origin");
			response.setHeader("Access-Control-Max-Age", "3600");

			// Preflight for an allowed origin: answer directly
			if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
				response.setStatus(HttpServletResponse.SC_OK);
				return;
			}
		} else if (origin != null && "OPTIONS".equalsIgnoreCase(request.getMethod())) {
			// Preflight from a non-allowlisted origin: refuse without CORS headers
			log.warn("CORS preflight rejected for origin: {}", origin);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		chain.doFilter(req, res);
	}

	@Override
	public void init(FilterConfig filterConfig) {
	}

	@Override
	public void destroy() {
	}
}
