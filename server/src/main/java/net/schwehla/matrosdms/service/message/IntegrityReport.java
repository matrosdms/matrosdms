/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import java.util.ArrayList;
import java.util.List;

public class IntegrityReport {
	private int totalDbItems;
	private int missingCount;
	private List<MissingItem> missingItems = new ArrayList<>();

	public void addMissingItem(String uuid, String name) {
		missingItems.add(new MissingItem(uuid, name));
		missingCount++;
	}

	public int getTotalDbItems() {
		return totalDbItems;
	}

	public void setTotalDbItems(int totalDbItems) {
		this.totalDbItems = totalDbItems;
	}

	public int getMissingCount() {
		return missingCount;
	}

	public List<MissingItem> getMissingItems() {
		return missingItems;
	}

	public static class MissingItem {
		public String uuid;
		public String name;

		public MissingItem(String uuid, String name) {
			this.uuid = uuid;
			this.name = name;
		}
	}
}
