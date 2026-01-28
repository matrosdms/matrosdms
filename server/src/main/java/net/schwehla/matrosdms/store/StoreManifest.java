/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import java.time.LocalDateTime;

public class StoreManifest {
	private String originalFilename;
	private String uuid;
	private String extension;
	private String mimeType;
	private LocalDateTime storageDate;
	private long originalSize;

	// Constructors, Getters, Setters
	public StoreManifest() {
	}

	public StoreManifest(String uuid, String originalFilename, String mimeType, long size) {
		this.uuid = uuid;
		this.originalFilename = originalFilename;
		this.mimeType = mimeType;
		this.originalSize = size;
		this.storageDate = LocalDateTime.now();

		int dotIndex = originalFilename.lastIndexOf('.');
		this.extension = (dotIndex > 0) ? originalFilename.substring(dotIndex) : "";
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public LocalDateTime getStorageDate() {
		return storageDate;
	}

	public void setStorageDate(LocalDateTime storageDate) {
		this.storageDate = storageDate;
	}

	public long getOriginalSize() {
		return originalSize;
	}

	public void setOriginalSize(long originalSize) {
		this.originalSize = originalSize;
	}
}
