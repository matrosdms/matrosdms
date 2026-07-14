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

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.beans.factory.annotation.Value;

import net.schwehla.matrosdms.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	// H2 console is off by default. PathRequest.toH2Console() resolves the
	// H2ConsoleProperties bean at request-match time; when the console is
	// disabled that bean is absent and EVERY request 500s. So only register the
	// matcher when the console is actually enabled.
	@Value("${spring.h2.console.enabled:false}")
	private boolean h2ConsoleEnabled;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		log.info("SECURITY: Manual CORS Filter Active. H2 console enabled: {}", h2ConsoleEnabled);

		// sameOrigin (not disable): enough for the H2 console iframe, still blocks
		// cross-site clickjacking
		http.headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(
						session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> {
					// FIX: Allow Async/Error dispatches
					auth.dispatcherTypeMatchers(
							DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD)
							.permitAll();

					// Allow H2 console only when it is enabled (otherwise the matcher
					// throws NoSuchBeanDefinitionException on every request)
					if (h2ConsoleEnabled) {
						auth.requestMatchers(PathRequest.toH2Console()).permitAll();
					}

					auth
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
								.authenticated();
				})
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
