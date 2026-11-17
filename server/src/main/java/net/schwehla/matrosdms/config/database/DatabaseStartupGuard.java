/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config.database;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs Flyway, but first makes sure the database is one this MatrosDMS may run on, and says so in
 * words a user can act on when it is not:
 * <ul>
 * <li>a newer MatrosDMS already migrated it: "this MatrosDMS is too old" (Flyway itself would ignore
 * the unknown migrations and let the old code run on the newer schema);</li>
 * <li>it fell back to an older state since the last start ({@link DatabaseGeneration});</li>
 * <li>its migration history does not match this build (Flyway's checksum mismatch).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class DatabaseStartupGuard {

	private static final Logger log = LoggerFactory.getLogger(DatabaseStartupGuard.class);

	private static final Pattern MISMATCH = Pattern.compile("checksum mismatch for migration version (\\S+)",
			Pattern.CASE_INSENSITIVE);

	@Bean
	FlywayMigrationStrategy guardedFlywayMigration(DatabaseGeneration generation, StartupSnapshot snapshot,
			@Value("${app.server.repository-path:}") String repositoryPath, ObjectProvider<BuildProperties> build) {
		Path backupDir = repositoryPath.isBlank() ? null : Path.of(repositoryPath, "db-backup").toAbsolutePath();
		BuildProperties props = build.getIfAvailable();
		String appVersion = props != null ? props.getVersion() : null;
		return flyway -> migrate(flyway, generation, new BackupHint(backupDir, generation.databaseFile()), appVersion,
				snapshot);
	}

	static void migrate(Flyway flyway, DatabaseGeneration generation, BackupHint backups, String appVersion,
			StartupSnapshot snapshot) {
		refuseNewerDatabase(flyway.info(), appVersion);

		DataSource dataSource = flyway.getConfiguration().getDataSource();
		generation.check(dataSource, backups, snapshot);

		try {
			flyway.migrate();
		} catch (FlywayValidateException e) {
			Matcher m = MISMATCH.matcher(String.valueOf(e.getMessage()));
			if (!m.find()) {
				throw e;
			}
			log.error("[DB] Flyway validation failed, refusing to start", e);
			Path kept = snapshot != null ? snapshot.onRefused(generation.marker()) : null;
			throw DatabaseStateException.historyMismatch(m.group(1), backups, kept);
		}

		generation.advance(dataSource);
	}

	/** Applied migrations this build does not know are a newer MatrosDMS's: stop instead of running old code on them. */
	static void refuseNewerDatabase(MigrationInfoService info, String appVersion) {
		MigrationVersion newest = Arrays.stream(info.applied())
				.filter(m -> m.getState() == MigrationState.FUTURE_SUCCESS
						|| m.getState() == MigrationState.FUTURE_FAILED)
				.map(MigrationInfo::getVersion)
				.filter(Objects::nonNull)
				.max(Comparator.naturalOrder())
				.orElse(null);
		if (newest == null) {
			return;
		}
		String known = Arrays.stream(info.all())
				.filter(m -> m.getState().isResolved())
				.map(MigrationInfo::getVersion)
				.filter(Objects::nonNull)
				.max(Comparator.naturalOrder())
				.map(MigrationVersion::toString)
				.orElse("none");
		log.error("[DB] Database is at migration {} but this build only knows up to {}. Refusing to start.",
				newest, known);
		throw DatabaseStateException.appTooOld(newest.toString(), known, appVersion);
	}
}
