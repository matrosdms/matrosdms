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

import net.schwehla.matrosdms.service.management.StoreConsistencyService;

public class IntegrityReport {
	private int totalDbItems;
	private int missingCount;
	private int corruptCount;
	private int orphanedIndexCount;

	private List<MissingItem> missingItems = new ArrayList<>();
	private List<CorruptItem> corruptItems = new ArrayList<>();
	private List<OrphanedIndexItem> orphanedIndexItems = new ArrayList<>();

	// The other direction: what is in the document store but not in the database
	private int storeDocuments;
	private List<StoreConsistencyService.UnknownDocument> unknownInStore = List.of();
	private List<String> strayFiles = List.of();

	public void setStoreCheck(StoreConsistencyService.Result storeCheck) {
		this.storeDocuments = storeCheck.storeDocuments();
		this.unknownInStore = storeCheck.unknownInStore();
		this.strayFiles = storeCheck.strayFiles();
	}

	public int getStoreDocuments() {
		return storeDocuments;
	}

	public List<StoreConsistencyService.UnknownDocument> getUnknownInStore() {
		return unknownInStore;
	}

	public List<String> getStrayFiles() {
		return strayFiles;
	}

	public boolean hasProblems() {
		return missingCount > 0 || corruptCount > 0 || orphanedIndexCount > 0 || !unknownInStore.isEmpty()
				|| !strayFiles.isEmpty();
	}

	public void addMissingItem(String uuid, String name) {
		missingItems.add(new MissingItem(uuid, name));
		missingCount++;
	}

	public void addCorruptItem(String uuid, String name, String expected, String actual) {
		corruptItems.add(new CorruptItem(uuid, name, expected, actual));
		corruptCount++;
	}

	public void addOrphanedIndexItem(String uuid) {
		orphanedIndexItems.add(new OrphanedIndexItem(uuid));
		orphanedIndexCount++;
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

	public int getOrphanedIndexCount() {
		return orphanedIndexCount;
	}

	public List<MissingItem> getMissingItems() {
		return missingItems;
	}

	public List<CorruptItem> getCorruptItems() {
		return corruptItems;
	}

	public List<OrphanedIndexItem> getOrphanedIndexItems() {
		return orphanedIndexItems;
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

	public static class OrphanedIndexItem {
		public String uuid;

		public OrphanedIndexItem(String uuid) {
			this.uuid = uuid;
		}
	}
}