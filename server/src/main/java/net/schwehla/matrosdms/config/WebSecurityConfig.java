/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import jakarta.servlet.DispatcherType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import net.schwehla.matrosdms.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		log.info("SECURITY: Manual CORS Filter Active.");

		http.headers(headers -> headers.frameOptions(FrameOptionsConfig::disable))
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(
						session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(
						auth -> auth
								// FIX: Allow Async/Error dispatches
								.dispatcherTypeMatchers(
										DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD)
								.permitAll()

								// Allow H2
								.requestMatchers(PathRequest.toH2Console())
								.permitAll()

								// FIX: Allow Actuator (Kubernetes Health Checks) - Solves 403 Error
								.requestMatchers("/actuator/**")
								.permitAll()

								// Allow Swagger
								.requestMatchers(
										"/v3/api-docs/**", "/api/v3/api-docs/**",
										"/swagger-ui/**", "/api/swagger-ui/**",
										"/swagger-ui.html", "/api/swagger-ui.html",
										"/api-docs/**", "/api/api-docs/**")
								.permitAll()

								// Allow Assets
								.requestMatchers("/assets/**", "/index.html", "/")
								.permitAll()

								// --- ALLOW LOGIN & REGISTRATION ---
								.requestMatchers(
										"/api/auth/login",
										"/api/auth/status",
										"/api/auth/register",
										"/auth/login",
										"/auth/status",
										"/auth/register")
								.permitAll()

								// --- ALLOW SYSTEM INFO (Version Check) ---
								.requestMatchers("/api/system/**")
								.permitAll()

								// Allow ERROR dispatcher
								.requestMatchers("/error")
								.permitAll()

								// Secure Everything Else
								.anyRequest()
								.authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
