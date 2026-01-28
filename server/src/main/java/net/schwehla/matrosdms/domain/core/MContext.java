/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MContext extends MBaseElement {

	private static final long serialVersionUID = 1L;

	private String name;
	private String description;
	private String icon;
	private EStage stage;
	private LocalDateTime dateRunUntil;
	private boolean visible = true; // UI Helper

	// FIX: Added itemCount field so the frontend receives the calculated count
	private long itemCount;

	// Dictionary for Categories (Who/What/Where/Kind)
	private Map<String, MCategoryList> dictionary = new HashMap<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public EStage getStage() {
		return stage;
	}

	public void setStage(EStage stage) {
		this.stage = stage;
	}

	public LocalDateTime getDateRunUntil() {
		return dateRunUntil;
	}

	public void setDateRunUntil(LocalDateTime dateRunUntil) {
		this.dateRunUntil = dateRunUntil;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public Map<String, MCategoryList> getDictionary() {
		return dictionary;
	}

	public void setDictionary(Map<String, MCategoryList> dictionary) {
		this.dictionary = dictionary;
	}

	public long getItemCount() {
		return itemCount;
	}

	public void setItemCount(long itemCount) {
		this.itemCount = itemCount;
	}
}
