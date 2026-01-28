/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.io.IOException;

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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // CRITICAL: Run before Spring Security
public class SimpleCORSFilter implements Filter {

	private final Logger log = LoggerFactory.getLogger(SimpleCORSFilter.class);

	public SimpleCORSFilter() {
		log.info("SimpleCORSFilter init (Highest Priority Mode)");
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		HttpServletResponse response = (HttpServletResponse) res;
		HttpServletRequest request = (HttpServletRequest) req;

		// 1. ALLOW ORIGIN: Dynamic echo allows localhost:5173, 5174, etc.
		String origin = request.getHeader("Origin");
		response.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "*");

		// 2. ALLOW CREDENTIALS (for JWT/Cookies)
		response.setHeader("Access-Control-Allow-Credentials", "true");

		// 3. ALLOW METHODS
		response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT, PATCH");

		// 4. ALLOW HEADERS (Include Authorization!)
		response.setHeader(
				"Access-Control-Allow-Headers",
				"Content-Type, Accept, X-Requested-With, Authorization, X-MATROS-USER, Origin");

		// 5. CACHE PREFLIGHT
		response.setHeader("Access-Control-Max-Age", "3600");

		// 6. HANDLE PREFLIGHT (OPTIONS)
		// Bypass the rest of the chain (Security) entirely for OPTIONS requests.
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
			return; // Stop here, do not continue to Security Filter Chain
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
