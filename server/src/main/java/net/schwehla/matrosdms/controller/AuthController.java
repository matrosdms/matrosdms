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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import net.schwehla.matrosdms.domain.core.EUserRole;
import net.schwehla.matrosdms.domain.core.MUser;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.security.JwtTokenProvider;
import net.schwehla.matrosdms.service.domain.UserService;
import net.schwehla.matrosdms.service.message.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
public class AuthController {

	@Autowired
	UserService userService;
	@Autowired
	JwtTokenProvider tokenProvider;

	@PostMapping("/auth/login")
	@Operation(summary = "Login user and return JWT + Refresh Token + User Object")
	public ResponseEntity<TokenRefreshResponse> login(@Valid @RequestBody LoginMessage loginMessage) {
		MUser user = userService.login(loginMessage.getUsername(), loginMessage.getPassword());
		String accessToken = tokenProvider.generateToken(user);
		String refreshToken = userService.createRefreshToken(user.getUuid());
		return ResponseEntity.ok(new TokenRefreshResponse(accessToken, refreshToken, user));
	}

	@PostMapping("/auth/refresh")
	@Operation(summary = "Get new Access Token using Refresh Token")
	public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		String requestToken = request.getRefreshToken();
		MUser user = userService.verifyRefreshTokenUser(requestToken);
		String newAccess = tokenProvider.generateToken(user);
		String newRefresh = userService.rotateRefreshToken(requestToken);
		return ResponseEntity.ok(new TokenRefreshResponse(newAccess, newRefresh, user));
	}

	// NEW: Logout Endpoint
	@PostMapping("/auth/logout")
	@Operation(summary = "Logout user (Revoke Refresh Token)")
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		userService.logout(request.getRefreshToken());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/auth/change-password")
	@Operation(summary = "Change current user's password and revoke all sessions")
	public ResponseEntity<Void> changePassword(
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser currentUser,
			@Valid @RequestBody ChangePasswordRequest request) {

		userService.changePassword(currentUser.getUuid(), request.getOldPassword(), request.getNewPassword());
		return ResponseEntity.ok().build();
	}

	@GetMapping("/auth/status")
	public ResponseEntity<Map<String, Boolean>> getSystemStatus() {
		boolean initialized = userService.getUserCount() > 0;
		return ResponseEntity.ok(Map.of("initialized", initialized));
	}

	@PostMapping("/auth/register")
	public ResponseEntity<MUser> registerFirstUser(@Valid @RequestBody CreateUserMessage message) {
		if (userService.getUserCount() > 0) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		message.setRole(EUserRole.ADMIN);
		MUser created = userService.createUser(message);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}