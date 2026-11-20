/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import net.schwehla.matrosdms.domain.core.EUserRole;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.repository.UserRepository;
import net.schwehla.matrosdms.security.JwtAuthenticationFilter;
import net.schwehla.matrosdms.security.JwtTokenProvider;

/**
 * The real security chain around stand-in endpoints: what a browser with an expired session gets.
 * The frontend renews the access token only on 401, and the renewal itself must not need one.
 */
@SpringJUnitWebConfig(classes = { WebSecurityConfig.class, JwtAuthenticationFilter.class,
		SessionExpiryTest.Web.class, SessionExpiryTest.Endpoints.class })
class SessionExpiryTest {

	@Configuration
	@EnableWebMvc
	static class Web {
	}

	@RestController
	static class Endpoints {
		@GetMapping("/api/contexts")
		String contexts() {
			return "[]";
		}

		@PostMapping("/api/auth/refresh")
		String refresh() {
			return "{}";
		}

		@PostMapping("/api/auth/logout")
		String logout() {
			return "";
		}
	}

	@MockitoBean
	JwtTokenProvider tokenProvider;

	@MockitoBean
	UserRepository userRepository;

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	void anExpiredTokenIs401SoTheBrowserRenewsIt() throws Exception {
		when(tokenProvider.validateToken("expired")).thenReturn(false);

		mvc.perform(get("/api/contexts").header("Authorization", "Bearer expired"))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/contexts")).andExpect(status().isUnauthorized());
	}

	@Test
	void aValidTokenGetsThrough() throws Exception {
		DBUser user = new DBUser();
		user.setName("martin");
		user.setRole(EUserRole.ADMIN);
		when(tokenProvider.validateToken("valid")).thenReturn(true);
		when(tokenProvider.getUsernameFromJWT("valid")).thenReturn("martin");
		when(userRepository.findByName(anyString())).thenReturn(Optional.of(user));

		mvc.perform(get("/api/contexts").header("Authorization", "Bearer valid")).andExpect(status().isOk());
	}

	@Test
	void renewingAndEndingASessionNeedNoAccessToken() throws Exception {
		when(tokenProvider.validateToken("expired")).thenReturn(false);

		mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer expired")
				.contentType("application/json").content("{\"refreshToken\":\"r\"}"))
				.andExpect(status().isOk());
		mvc.perform(post("/api/auth/logout").contentType("application/json").content("{\"refreshToken\":\"r\"}"))
				.andExpect(status().isOk());
	}
}
