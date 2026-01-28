/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.admin.EConfigKey;
import net.schwehla.matrosdms.service.domain.ConfigService;
import net.schwehla.matrosdms.service.message.ConfigMessage;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/config") // Prefixed with /api via WebConfig
public class ConfigController {

	@Autowired
	ConfigService configService;

	@GetMapping("/{key}")
	@Operation(summary = "Get a specific configuration value")
	public ResponseEntity<ConfigMessage> getConfig(@PathVariable EConfigKey key) {
		return configService
				.getValue(key)
				.map(val -> ResponseEntity.ok(new ConfigMessage(key, val)))
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping
	@Operation(summary = "Get all system configurations")
	public ResponseEntity<Map<EConfigKey, String>> getAllConfigs() {
		Map<EConfigKey, String> result = java.util.Arrays.stream(EConfigKey.values())
				.collect(Collectors.toMap(k -> k, k -> configService.getValue(k).orElse(null)));
		return ResponseEntity.ok(result);
	}

	@PostMapping
	@Operation(summary = "Set a configuration value")
	public ResponseEntity<Void> setConfig(@RequestBody ConfigMessage message) {
		configService.setValue(message.getKey(), message.getValue());
		return ResponseEntity.ok().build();
	}
}
