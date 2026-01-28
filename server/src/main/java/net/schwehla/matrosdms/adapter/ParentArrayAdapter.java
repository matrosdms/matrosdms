/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.adapter;

import java.util.List;

import com.fasterxml.jackson.databind.util.StdConverter;

import net.schwehla.matrosdms.domain.core.MBaseElement;

public class ParentArrayAdapter extends StdConverter<List<MBaseElement>, MBaseElement> {

	@Override
	public MBaseElement convert(List<MBaseElement> list) {
		if (list.isEmpty()) {
			return null;
		}
		if (list.size() == 1) {
			return list.get(0);
		}
		throw new IllegalStateException("More than one parent");
	}
}
