/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Fixed system root categories")
public enum ERootCategory {
	@Schema(description = "Persons or Groups")
	WHO("Persons / Groups", ECategoryScope.CONTEXT),

	@Schema(description = "Topics or Content")
	WHAT("Topics / Content", ECategoryScope.CONTEXT),

	@Schema(description = "Locations")
	WHERE("Locations", ECategoryScope.CONTEXT),

	// --- NEW: Ownership Example ---
	// @Schema(description = "Legal Owner / Mandator")
	// OWNER("Mandator", ECategoryScope.CONTEXT),
	// Needs SQL-Update for insert ROOT_

	@Schema(description = "Document Types")
	KIND("Document Type", ECategoryScope.DOCUMENT);

	private final String description;
	private final ECategoryScope scope; // <--- The Logic Field

	// Cache lookup for performance
	private static final Map<String, ERootCategory> LOOKUP_MAP = Arrays.stream(values())
			.collect(Collectors.toMap(e -> e.name().toUpperCase(), Function.identity()));

	/**
	 * Constructor
	 *
	 * @param description
	 *            Human readable label
	 * @param scope
	 *            Where this category is used (Context vs Document)
	 */
	ERootCategory(String description, ECategoryScope scope) {
		this.description = description;
		this.scope = scope;
	}

	/**
	 * Internal: Used for Database UUID (e.g. "ROOT_WHO") This avoids SQL keywords
	 * because the UUID
	 * string "ROOT_WHERE" is safe.
	 */
	public String getUuid() {
		return "ROOT_" + this.name();
	}

	public String getDescription() {
		return description;
	}

	/** Used by Frontend to decide where to show this category filter. */
	public ECategoryScope getScope() {
		return scope;
	}

	/** External: Used for JSON/API output (e.g. "WHO") */
	@JsonValue
	public String getName() {
		return this.name();
	}

	/**
	 * Converter: Strict Lookup Accepts "WHO", "WHAT", "WHERE", "KIND", "OWNER"
	 * (Case insensitive).
	 */
	@JsonCreator
	public static ERootCategory fromString(String value) {
		if (value == null || value.isBlank())
			return null;

		ERootCategory result = LOOKUP_MAP.get(value.toUpperCase());

		if (result == null) {
			throw new IllegalArgumentException("Unknown RootCategory: " + value);
		}

		return result;
	}
}
