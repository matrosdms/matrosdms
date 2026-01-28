/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import net.schwehla.matrosdms.domain.core.MBaseElement;

public class CreateCategoryMessage extends MBaseElement {

	private static final long serialVersionUID = 1L;

	boolean dropfieldCategory;
	private boolean object;

	public boolean isDropfieldCategory() {
		return dropfieldCategory;
	}

	public void setDropfieldCategory(boolean dropfieldCategory) {
		this.dropfieldCategory = dropfieldCategory;
	}

	public boolean isObject() {
		return object;
	}

	public void setObject(boolean object) {
		this.object = object;
	}
}
