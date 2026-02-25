/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.api.SystemInfoResponse;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/system")
public class SystemController {

	@Value("${app.version:1.0.0-SNAPSHOT}")
	private String appVersion;

	@Value("${app.server.repository-path}")
	private String repositoryPath;

	@GetMapping("/version")
	@Operation(summary = "Get API Version and System Information")
	public ResponseEntity<SystemInfoResponse> getVersion() {
		SystemInfoResponse response = new SystemInfoResponse(
				appVersion,
				"MatrosDMS Server",
				"OK",
				formatTenantName(repositoryPath));
		return ResponseEntity.ok(response);
	}

	/**
	 * Extracts the folder name and formats it as Title Case.
	 * e.g., "d:/cloud/repository/schwehla-invest" -> "Schwehla-Invest"
	 */
	private String formatTenantName(String path) {
		if (path == null || path.isBlank()) {
			return "Default";
		}

		// 1. Extract the last folder name
		String normalizedPath = path.replace("\\", "/");
		if (normalizedPath.endsWith("/")) {
			normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
		}

		int lastSlashIndex = normalizedPath.lastIndexOf('/');
		String rawName = (lastSlashIndex >= 0 && lastSlashIndex < normalizedPath.length() - 1)
				? normalizedPath.substring(lastSlashIndex + 1)
				: normalizedPath;

		// 2. Format to Title Case (split by hyphen)
		String[] parts = rawName.split("-");
		StringBuilder formattedName = new StringBuilder();

		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (!part.isEmpty()) {
				// Capitalize first letter, lowercase the rest
				formattedName.append(Character.toUpperCase(part.charAt(0)))
						.append(part.substring(1).toLowerCase());
			}
			if (i < parts.length - 1) {
				formattedName.append("-");
			}
		}

		return formattedName.toString();
	}
}