/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.integration.google.GoogleAccountService;
import net.schwehla.matrosdms.integration.google.GoogleHttp;
import net.schwehla.matrosdms.service.message.GoogleConnectResponse;
import net.schwehla.matrosdms.service.message.GoogleIntegrationStatus;
import net.schwehla.matrosdms.service.message.GoogleSyncSettingsMessage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/** The current user's Google Tasks / Calendar connection. */
@RestController
@RequestMapping("/integrations/google")
public class GoogleIntegrationController {

	private static final Logger log = LoggerFactory.getLogger(GoogleIntegrationController.class);

	/** Public (see WebSecurityConfig): Google's redirect carries no MatrosDMS login. */
	public static final String CALLBACK_PATH = "/api/integrations/google/callback";

	private static final Pattern ORIGIN = Pattern.compile("^https?://[^/\\s]+$");

	@Autowired
	GoogleAccountService googleAccounts;
	@Autowired
	AppServerSpringConfig config;

	@GetMapping("/status")
	@Operation(summary = "Google sync state of the current user")
	public ResponseEntity<GoogleIntegrationStatus> status(@Parameter(hidden = true) @AuthenticationPrincipal DBUser user) {
		return ResponseEntity.ok(googleAccounts.status(user.getUuid()));
	}

	@PostMapping("/connect")
	@Operation(summary = "Start connecting a Google account; returns the consent URL to open")
	public ResponseEntity<GoogleConnectResponse> connect(
			@Parameter(description = "Also ask for the calendar permission and switch the calendar on")
			@RequestParam(defaultValue = "false") boolean calendar,
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser user, HttpServletRequest request) {
		String url = googleAccounts.startAuthorization(user.getUuid(), redirectUri(request), calendar);
		return ResponseEntity.ok(new GoogleConnectResponse(url));
	}

	@GetMapping("/callback")
	@Operation(hidden = true)
	public ResponseEntity<Void> callback(
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String error) {
		String outcome;
		if (error != null) {
			// e.g. access_denied when the user cancels the consent screen
			outcome = "google=error&reason=" + GoogleHttp.encode(error);
		} else if (code == null) {
			outcome = "google=error&reason=" + GoogleHttp.encode("missing authorization code");
		} else {
			try {
				googleAccounts.completeAuthorization(state, code);
				outcome = "google=connected";
			} catch (ResponseStatusException e) {
				outcome = "google=error&reason=" + GoogleHttp.encode(String.valueOf(e.getReason()));
			} catch (Exception e) {
				log.warn("Google sign-in failed: {}", e.getMessage());
				outcome = "google=error&reason=" + GoogleHttp.encode(String.valueOf(e.getMessage()));
			}
		}
		// Relative: the browser stays on the origin it started from (and its login)
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, "/?view=profile&" + outcome)
				.build();
	}

	@PutMapping("/settings")
	@Operation(summary = "Switch Google Tasks / Calendar sync on or off for the current user")
	public ResponseEntity<GoogleIntegrationStatus> updateSettings(
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser user,
			@RequestBody GoogleSyncSettingsMessage settings) {
		return ResponseEntity.ok(googleAccounts.updateSettings(
				user.getUuid(), settings.tasksEnabled(), settings.calendarEnabled()));
	}

	@PostMapping("/resync")
	@Operation(summary = "Queue a sync of all of the current user's tasks; returns how many")
	public ResponseEntity<Map<String, Integer>> resync(@Parameter(hidden = true) @AuthenticationPrincipal DBUser user) {
		return ResponseEntity.accepted().body(Map.of("scheduled", googleAccounts.resync(user.getUuid())));
	}

	@DeleteMapping
	@Operation(summary = "Disconnect the current user's Google account (pauses the sync)")
	public ResponseEntity<GoogleIntegrationStatus> disconnect(
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser user) {
		return ResponseEntity.ok(googleAccounts.disconnect(user.getUuid()));
	}

	/**
	 * The configured callback URL, else the browser's own origin. A "Desktop app"
	 * OAuth client accepts any loopback port, so every tenant profile works without
	 * registering anything. Going by the Origin header rather than the Host also
	 * keeps a dev setup behind the Vite proxy on the Vite origin, where the login
	 * lives.
	 */
	private String redirectUri(HttpServletRequest request) {
		String configured = config.getIntegrations().getGoogle().getRedirectUri();
		if (configured != null && !configured.isBlank()) {
			return configured;
		}
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (origin != null && ORIGIN.matcher(origin).matches()) {
			return origin + CALLBACK_PATH;
		}
		URI self = ServletUriComponentsBuilder.fromContextPath(request).replacePath(CALLBACK_PATH).replaceQuery(null)
				.build().toUri();
		return self.toString();
	}
}
