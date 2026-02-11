/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline;

/**
 * Thrown when a duplicate file is detected during pipeline processing.
 * Contains the hash of the incoming file and the UUID of the existing item.
 */
public class DuplicateException extends Exception {

	private static final long serialVersionUID = 1L;

	private final String hash;
	private final String existingUuid;

	public DuplicateException(String hash, String existingUuid) {
		super("Duplicate file detected: " + hash + " matches existing item " + existingUuid);
		this.hash = hash;
		this.existingUuid = existingUuid;
	}

	public String getHash() {
		return hash;
	}

	public String getExistingUuid() {
		return existingUuid;
	}
}
