/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

/**
 * Result of a document storage operation.
 */
public class StoreResult {

	private String SHA256;
	private String cryptSettings;

	public StoreResult() {
	}

	public StoreResult(String sha256, String cryptSettings) {
		this.SHA256 = sha256;
		this.cryptSettings = cryptSettings;
	}

	public String getSHA256() {
		return SHA256;
	}

	public void setSHA256(String SHA256) {
		this.SHA256 = SHA256;
	}

	public String getCryptSettings() {
		return cryptSettings;
	}

	public void setCryptSettings(String cryptSettings) {
		this.cryptSettings = cryptSettings;
	}

	@Override
	public String toString() {
		return "StoreResult{" +
				"SHA256='" + SHA256 + '\'' +
				", cryptSettings='" + cryptSettings + '\'' +
				'}';
	}
}