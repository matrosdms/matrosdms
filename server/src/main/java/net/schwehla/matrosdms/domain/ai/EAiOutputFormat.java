/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.ai;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "The desired output format for the AI response")
public enum EAiOutputFormat {
	@Schema(description = "Standard Markdown (headers, bold, lists)")
	MARKDOWN("Use Markdown formatting (Headers #, Lists -, Bold **)."),

	@Schema(description = "Strict JSON structure")
	JSON("Output strictly valid JSON."),

	@Schema(description = "Plain text without special formatting")
	TEXT("Output plain text only.");

	private final String formatInstruction;

	EAiOutputFormat(String formatInstruction) {
		this.formatInstruction = formatInstruction;
	}

	public String getFormatInstruction() {
		return formatInstruction;
	}
}
