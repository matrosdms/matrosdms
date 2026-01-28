/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

/** DTO to expose Root Category configuration (Scope, Label) to the Frontend. */
public class RootCategoryMeta {

	private String key;
	private String label;
	private ECategoryScope scope;

	public static RootCategoryMeta fromEnum(ERootCategory e) {
		RootCategoryMeta meta = new RootCategoryMeta();
		meta.key = e.getName(); // Returns the JSON value (e.g., "WHO")
		meta.label = e.getDescription();
		meta.scope = e.getScope();
		return meta;
	}

	// Getters and Setters
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public ECategoryScope getScope() {
		return scope;
	}

	public void setScope(ECategoryScope scope) {
		this.scope = scope;
	}
}
