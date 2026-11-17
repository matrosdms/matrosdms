/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import net.schwehla.matrosdms.service.management.H2BackupService;

/** Where the database snapshots are, so a refusal to start can say what to restore. */
record BackupHint(Path backupDir, Path databaseFile) {

	Optional<Path> newest() {
		if (backupDir == null || !Files.isDirectory(backupDir)) {
			return Optional.empty();
		}
		try (Stream<Path> files = Files.list(backupDir)) {
			// An incident backup is the file a refused start found: not one to restore
			return files.filter(p -> p.getFileName().toString().endsWith(".zip"))
					.filter(p -> !p.getFileName().toString().startsWith("matros-db-" + StartupSnapshot.INCIDENT + "-"))
					.max(Comparator.comparing(H2BackupService::backupTime));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	String restoreAdvice() {
		StringBuilder text = new StringBuilder("To repair it, restore the newest backup");
		newest().ifPresentOrElse(
				zip -> text.append(":\n  ").append(zip),
				() -> text.append(" from\n  ").append(backupDir));
		if (databaseFile != null) {
			String name = databaseFile.getFileName().toString();
			String marker = name.replace(".mv.db", DatabaseGeneration.MARKER_SUFFIX);
			text.append("\nMove ").append(name).append(" and ").append(marker).append(" out of\n  ")
					.append(databaseFile.getParent())
					.append("\n(keep them until everything works again), unzip the backup into that folder and "
							+ "start MatrosDMS again.");
		}
		text.append(" Step by step: doc/usage.md, \"Restoring a backup\".");
		return text.toString();
	}
}
