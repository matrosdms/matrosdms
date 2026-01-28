/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.service.RAGService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI", description = "Artificial Intelligence features (Chat/RAG)")
public class AiQueryController {

	@Autowired
	RAGService ragService;

	@PostMapping("/chat")
	@Operation(summary = "Chat with your documents (RAG)", description = "Sends a question to the LLM, augmented with context from the most relevant documents in"
			+ " the archive.")
	public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
		String answer = ragService.chat(request.message());
		return ResponseEntity.ok(new ChatResponse(answer));
	}

	// --- DTOs ---

	@Schema(description = "Request object for AI Chat")
	public record ChatRequest(
			@Schema(example = "What is my Tax ID?", requiredMode = Schema.RequiredMode.REQUIRED) String message,
			@Schema(example = "uuid-1234", description = "Optional ID to continue a conversation (Future feature)") String conversationId) {
	}

	@Schema(description = "Response from AI")
	public record ChatResponse(
			@Schema(example = "Based on 'Tax_2024.pdf', your ID is 123-45.") String reply) {
	}
}
