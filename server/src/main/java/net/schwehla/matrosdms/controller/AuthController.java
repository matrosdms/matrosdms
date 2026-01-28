/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.core.EUserRole;
import net.schwehla.matrosdms.domain.core.MUser;
import net.schwehla.matrosdms.security.JwtTokenProvider;
import net.schwehla.matrosdms.service.domain.UserService;
import net.schwehla.matrosdms.service.message.CreateUserMessage;
import net.schwehla.matrosdms.service.message.LoginMessage;
import net.schwehla.matrosdms.service.message.LoginResponse;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class AuthController {

	@Autowired
	UserService userService;
	@Autowired
	JwtTokenProvider tokenProvider;

	@PostMapping("/auth/login")
	@Operation(summary = "Login user and return JWT token")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginMessage loginMessage) {

		MUser user = userService.login(loginMessage.getUsername(), loginMessage.getPassword());
		String token = tokenProvider.generateToken(user);
		return ResponseEntity.ok(new LoginResponse(token, user));
	}

	// --- WIZARD / INITIALIZATION SUPPORT ---

	@GetMapping("/auth/status")
	@Operation(summary = "Check if the system is initialized (has users)")
	public ResponseEntity<Map<String, Boolean>> getSystemStatus() {
		// If count > 0, system is initialized. Frontend should show Login.
		// If count == 0, system is uninitialized. Frontend should show
		// Registration/Setup.
		boolean initialized = userService.getUserCount() > 0;
		return ResponseEntity.ok(Map.of("initialized", initialized));
	}

	@PostMapping("/auth/register")
	@Operation(summary = "Register the first Admin user (Only allowed if DB is empty)")
	public ResponseEntity<MUser> registerFirstUser(@Valid @RequestBody CreateUserMessage message) {

		// Security Check: This endpoint is public, but we ONLY allow it
		// to be used if the database is completely empty (First Run).
		if (userService.getUserCount() > 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// Force role to ADMIN for the first user
		message.setRole(EUserRole.ADMIN);

		MUser created = userService.createUser(message);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}
