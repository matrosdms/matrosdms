/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

/** Central strategy for mapping internal UUIDs to physical storage paths. */
@Component
public class StoragePathStrategy {

	public Path getPhysicalPath(Path rootFolder, String uuid, String suffix) {
		if (uuid == null || uuid.length() < 3) {
			throw new IllegalArgumentException("Invalid UUID for path resolution: " + uuid);
		}
		// Centralized logic: First 3 chars of UUID define the subfolder
		return rootFolder.resolve(uuid.substring(0, 3)).resolve(uuid + suffix);
	}
}
