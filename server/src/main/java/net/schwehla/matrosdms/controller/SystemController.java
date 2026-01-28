/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/system")
public class SystemController {

	// Injected from build properties or defaults
	@Value("${app.version:2.0.0-SNAPSHOT}")
	private String appVersion;

	@GetMapping("/version")
	@Operation(summary = "Get API Version information")
	public ResponseEntity<Map<String, String>> getVersion() {
		Map<String, String> info = new HashMap<>();
		info.put("version", appVersion);
		info.put("name", "MatrosDMS Server");
		info.put("status", "OK");
		return ResponseEntity.ok(info);
	}
}
