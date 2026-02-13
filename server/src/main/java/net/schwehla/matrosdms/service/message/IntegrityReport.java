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
	private int corruptCount;
    
	private List<MissingItem> missingItems = new ArrayList<>();
	private List<CorruptItem> corruptItems = new ArrayList<>();

	public void addMissingItem(String uuid, String name) {
		missingItems.add(new MissingItem(uuid, name));
		missingCount++;
	}

	public void addCorruptItem(String uuid, String name, String expected, String actual) {
		corruptItems.add(new CorruptItem(uuid, name, expected, actual));
		corruptCount++;
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

	public int getCorruptCount() {
		return corruptCount;
	}

	public List<MissingItem> getMissingItems() {
		return missingItems;
	}

	public List<CorruptItem> getCorruptItems() {
		return corruptItems;
	}

	public static class MissingItem {
		public String uuid;
		public String name;

		public MissingItem(String uuid, String name) {
			this.uuid = uuid;
			this.name = name;
		}
	}

	public static class CorruptItem {
		public String uuid;
		public String name;
		public String expectedHash;
		public String actualHash;

		public CorruptItem(String uuid, String name, String expected, String actual) {
			this.uuid = uuid;
			this.name = name;
			this.expectedHash = expected;
			this.actualHash = actual;
		}
	}
}