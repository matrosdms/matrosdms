/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.ai;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "The type of analysis or transformation to perform")
public enum EAiInstruction {
	@Schema(description = "Create a concise summary of the content")
	SUMMARY("Summarize the document content."),

	@Schema(description = "Extract structured data into a table format")
	TABLE_EXTRACTION("Extract all relevant data points into a structured table."),

	@Schema(description = "Identify required actions, deadlines, or todos")
	ACTION_ITEMS("Identify and list all action items, to-dos, and deadlines."),

	@Schema(description = "Clean up OCR errors and fix formatting")
	PROOFREAD("Correct OCR errors and improve readability while keeping the original meaning."),

	@Schema(description = "Extract specific key-value pairs (Dates, Amounts, Names)")
	KEY_FACTS("Extract key facts (Dates, Persons, Amounts, IDs) as a list.");

	private final String basePrompt;

	EAiInstruction(String basePrompt) {
		this.basePrompt = basePrompt;
	}

	public String getBasePrompt() {
		return basePrompt;
	}
}
