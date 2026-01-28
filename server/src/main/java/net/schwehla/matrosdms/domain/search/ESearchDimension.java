/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.search;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum ESearchDimension {
	WHO("kindList"), WHAT("kindList"), WHERE("kindList"), KIND("kindList"), CONTEXT("infoContext"), STORE(
			"store"), ISSUE_DATE(
					"issueDate"), CREATED("dateCreated"), FULLTEXT("fulltext"), ATTRIBUTE("attr"), SOURCE("source"),

	// NEW: Search Filter "Has Text Layer" -> Yes/No
	HAS_TEXT("textParsed");

	private final String luceneField;

	ESearchDimension(String f) {
		this.luceneField = f;
	}

	public String getLuceneField() {
		return luceneField;
	}
}
