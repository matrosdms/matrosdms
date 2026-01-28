/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

public class StoreResult {

	String SHA256;
	String cryptSettings;

	public String getSHA256() {
		return SHA256;
	}

	public void setSHA256(String sHA256) {
		SHA256 = sHA256;
	}

	public String getCryptSettings() {
		return cryptSettings;
	}

	public void setCryptSettings(String cryptSettings) {
		this.cryptSettings = cryptSettings;
	}
}
