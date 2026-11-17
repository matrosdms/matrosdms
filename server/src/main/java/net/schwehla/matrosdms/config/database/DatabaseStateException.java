/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config.database;

import java.nio.file.Path;

/**
 * The database is in a state this MatrosDMS must not run on. The message is written for the person
 * who started the app: the desktop build shows it as is in its "could not start" window
 * ({@code DesktopSplash} shows the innermost cause, so this exception deliberately has none; the
 * technical details are logged where it is thrown).
 */
public class DatabaseStateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private DatabaseStateException(String message) {
		super(message, null, false, false);
	}

	/** A newer MatrosDMS already migrated this database. */
	static DatabaseStateException appTooOld(String databaseVersion, String appKnowsUpTo, String appVersion) {
		String app = appVersion == null || appVersion.isBlank() ? "This MatrosDMS" : "MatrosDMS " + appVersion;
		return new DatabaseStateException(app + " is too old for this database.\n\n"
				+ "A newer MatrosDMS has already updated the database (database version " + databaseVersion
				+ "; this version only knows up to " + appKnowsUpTo + "). Install the current MatrosDMS from "
				+ "https://github.com/matrosdms/matrosdms/releases and start it again.\n\n"
				+ "Nothing was changed.");
	}

	/** The database went back in time since the last start: H2 fell back to an old state, or an old file was copied over it. */
	static DatabaseStateException rolledBack(long databaseGeneration, long expectedGeneration, Path marker,
			BackupHint backups, Path keptCopy) {
		return new DatabaseStateException("The database is older than the last time MatrosDMS ran.\n\n"
				+ "It is at save point " + databaseGeneration + ", but MatrosDMS already saw save point "
				+ expectedGeneration + ". The database file was set back to an older state, for example after "
				+ "an unclean shutdown or because an old copy was put in its place. MatrosDMS stopped so that "
				+ "nothing is saved on top of the old state.\n\n"
				+ backups.restoreAdvice()
				+ keptCopyNote(keptCopy) + "\n\n"
				+ "Did you restore an older backup on purpose? Then delete\n  " + marker
				+ "\nand start MatrosDMS again.");
	}

	/** Flyway's history differs from this build's migrations: a set-back database or one used by a development build. */
	static DatabaseStateException historyMismatch(String migration, BackupHint backups, Path keptCopy) {
		return new DatabaseStateException("The database does not match this MatrosDMS version.\n\n"
				+ "Migration " + migration + " is recorded differently in the database than in this version. "
				+ "That happens when the database file was set back to an old copy, or when a development "
				+ "build used it. Nothing was changed.\n\n"
				+ backups.restoreAdvice()
				+ keptCopyNote(keptCopy));
	}

	private static String keptCopyNote(Path keptCopy) {
		return keptCopy == null ? ""
				: "\n\nThe database file as it was before this start is kept as\n  " + keptCopy
						+ "\nIt may still hold data newer than the backup: do not restore it, but keep it.";
	}
}
