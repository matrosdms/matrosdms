/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import java.io.Serializable;

public class SavedSearchMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private String query; // The MQL string

	public SavedSearchMessage() {
	}

	public SavedSearchMessage(String name, String query) {
		this.name = name;
		this.query = query;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SavedSearchMessage that = (SavedSearchMessage) o;
		// Identification by Name (Case Insensitive) allows upserts
		return name != null ? name.equalsIgnoreCase(that.name) : that.name == null;
	}

	@Override
	public int hashCode() {
		return name != null ? name.toLowerCase().hashCode() : 0;
	}
}
