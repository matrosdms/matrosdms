/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import net.schwehla.matrosdms.domain.api.BroadcastMessage;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.messagebus.VUEMessageBus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/stream")
public class NotificationController {

	@Autowired
	private VUEMessageBus messageBus;

	@GetMapping(path = "/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Subscribe to server events (SSE)")
	@ApiResponse(responseCode = "200", description = "Stream of BroadcastMessages", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(implementation = BroadcastMessage.class)))
	public SseEmitter subscribe(@AuthenticationPrincipal DBUser user) {
		// Timeout: 1 Hour
		SseEmitter emitter = new SseEmitter(3600_000L);
		messageBus.addEmitter(emitter);
		return emitter;
	}
}
