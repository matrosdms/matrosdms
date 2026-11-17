/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.OptionalLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

/**
 * Runs the real migrations on a real H2 file database, the way the desktop app starts: startup
 * backup copied before the open, guard, Flyway, startup backup published. Checks the three refusals
 * (database fell back, history differs, app too old) and what is kept when a start is refused.
 */
class DatabaseStartupGuardTest {

	@TempDir
	Path dir;

	private String url;
	private DataSource dataSource;
	private DatabaseGeneration generation;
	private BackupHint backups;
	private Path backupDir;
	private StartupSnapshot snapshot;

	@BeforeEach
	void setUp() throws Exception {
		Path dbDir = Files.createDirectories(dir.resolve("workspace/db"));
		url = "jdbc:h2:" + dbDir.resolve("matros").toString().replace('\\', '/')
				+ ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL(url);
		ds.setUser("admin");
		ds.setPassword("admin");
		dataSource = ds;
		generation = new DatabaseGeneration(url);

		backupDir = Files.createDirectories(dir.resolve("repository/db-backup"));
		Files.writeString(backupDir.resolve("matros-db-daily-2026-11-16_08-08-22.zip"), "x");
		backups = new BackupHint(backupDir, generation.databaseFile());
		snapshot = new StartupSnapshot();
	}

	private Flyway flyway() {
		return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
	}

	/** What a start does: copy the file, let the guard and Flyway open it, publish the startup backup. */
	private void start() {
		snapshot.take(generation.databaseFile(), backupDir);
		DatabaseStartupGuard.migrate(flyway(), generation, backups, "1.4.0", snapshot);
		snapshot.publish();
	}

	private List<Path> backupsOf(String type) throws Exception {
		return StartupSnapshot.backups(backupDir, type).toList();
	}

	private void sql(String statement) throws SQLException {
		try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
			st.execute(statement);
		}
	}

	private long dbGeneration() throws SQLException {
		try (Connection c = dataSource.getConnection()) {
			return DatabaseGeneration.readGeneration(c).orElseThrow();
		}
	}

	@Test
	void firstStartMigratesAndWritesTheMarker() throws Exception {
		start();

		assertThat(generation.marker()).exists();
		assertThat(generation.readMarker()).hasValue(1);
		assertThat(dbGeneration()).isEqualTo(1);
	}

	@Test
	void everyStartAndCheckpointRaisesTheSavePoint() throws Exception {
		start();
		generation.advance(dataSource); // periodic checkpoint
		start();

		assertThat(dbGeneration()).isEqualTo(3);
		assertThat(generation.readMarker()).hasValue(3);
	}

	@Test
	void startupBackupIsTheFileFromBeforeTheOpen() throws Exception {
		start();
		assertThat(backupsOf(StartupSnapshot.STARTUP)).isEmpty(); // first start: no file to copy yet

		long sizeBeforeOpen = Files.size(generation.databaseFile());
		start();

		List<Path> startup = backupsOf(StartupSnapshot.STARTUP);
		assertThat(startup).hasSize(1);
		try (ZipFile zip = new ZipFile(startup.get(0).toFile())) {
			ZipEntry entry = zip.getEntry("matros.mv.db"); // same layout as BACKUP TO
			assertThat(entry).isNotNull();
			assertThat(entry.getSize()).isEqualTo(sizeBeforeOpen);
		}
		// the local copy is gone once it is zipped
		assertThat(StartupSnapshot.copyOf(generation.databaseFile())).doesNotExist();
	}

	@Test
	void databaseThatFellBackIsRefused() throws Exception {
		start();
		generation.advance(dataSource);
		sql("update db_generation set generation = 1"); // what H2's fallback to an old state looks like

		assertThatThrownBy(this::start)
				.isInstanceOf(DatabaseStateException.class)
				.hasMessageContaining("older than the last time MatrosDMS ran")
				.hasMessageContaining("save point 1")
				.hasMessageContaining("save point 2")
				.hasMessageContaining("matros.generation");
		// Refused before anything ran: the database is not touched
		assertThat(dbGeneration()).isEqualTo(1);
	}

	@Test
	void refusedStartKeepsTheFileAsItWasOnceForTheWholeSeries() throws Exception {
		start();
		start(); // a successful start with a startup backup
		generation.advance(dataSource);
		sql("update db_generation set generation = 1");

		assertThatThrownBy(this::start)
				.isInstanceOf(DatabaseStateException.class)
				.hasMessageContaining("is kept as")
				.hasMessageContaining("matros-db-incident-")
				// restore advice points at a real backup, never at the incident
				.hasMessageContaining("restore the newest backup:\n  " + backupsOf(StartupSnapshot.STARTUP).get(0));
		assertThat(backupsOf(StartupSnapshot.INCIDENT)).hasSize(1);

		// Trying again copies the already rewritten file: it must not replace the evidence or pile up
		assertThatThrownBy(this::start).isInstanceOf(DatabaseStateException.class);
		assertThat(backupsOf(StartupSnapshot.INCIDENT)).hasSize(1);
		assertThat(backupsOf(StartupSnapshot.STARTUP)).hasSize(1);
		assertThat(StartupSnapshot.copyOf(generation.databaseFile())).doesNotExist();
	}

	@Test
	void startupBackupFollowsTheSetting() throws Exception {
		start();
		Path copy = StartupSnapshot.copyOf(generation.databaseFile());

		StartupSnapshot off = new StartupSnapshot();
		off.setEnvironment(new MockEnvironment().withProperty("spring.datasource.url", url)
				.withProperty("app.server.repository-path", dir.resolve("repository").toString())
				.withProperty("app.backup.on-startup", "false"));
		off.postProcessBeanFactory(null);
		assertThat(copy).doesNotExist();

		StartupSnapshot on = new StartupSnapshot();
		on.setEnvironment(new MockEnvironment().withProperty("spring.datasource.url", url)
				.withProperty("app.server.repository-path", dir.resolve("repository").toString())); // default: on
		on.postProcessBeanFactory(null);
		assertThat(copy).exists();
		assertThat(on.publish()).isPresent();
	}

	@Test
	void fallbackToBeforeTheGuardExistedIsRefused() throws Exception {
		start();
		sql("drop table db_generation"); // fell back to a state from before V70

		assertThatThrownBy(this::start)
				.isInstanceOf(DatabaseStateException.class)
				.hasMessageContaining("save point 0");
	}

	@Test
	void deletingTheMarkerAcceptsARestoredBackup() throws Exception {
		start();
		generation.advance(dataSource);
		sql("update db_generation set generation = 1");
		Files.delete(generation.marker());

		assertThatCode(this::start).doesNotThrowAnyException();
		assertThat(generation.readMarker()).hasValue(2);
	}

	@Test
	void changedMigrationHistoryGetsAnActionableMessage() throws Exception {
		start();
		sql("update \"flyway_schema_history\" set \"checksum\" = -747657783 where \"version\" = '10'");

		assertThatThrownBy(this::start)
				.isInstanceOf(DatabaseStateException.class)
				.hasMessageContaining("does not match this MatrosDMS version")
				.hasMessageContaining("Migration 10")
				.hasMessageContaining("restore the newest backup")
				.hasMessageContaining("matros-db-incident-");
	}

	@Test
	void databaseMigratedByANewerVersionSaysTheAppIsTooOld() throws Exception {
		start();
		sql("""
				insert into "flyway_schema_history" ("installed_rank", "version", "description", "type", "script",
				  "checksum", "installed_by", "execution_time", "success")
				values (999, '999', 'from the future', 'SQL', 'V999__from_the_future.sql', 1, 'admin', 1, true)
				""");

		assertThatThrownBy(this::start)
				.isInstanceOf(DatabaseStateException.class)
				.hasMessageContaining("MatrosDMS 1.4.0 is too old for this database")
				.hasMessageContaining("database version 999")
				.hasMessageContaining("only knows up to 70");
	}

	@Test
	void guardIsOffForServerAndInMemoryDatabases() {
		assertThat(DatabaseGeneration.markerFor("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")).isNull();
		assertThat(DatabaseGeneration.markerFor("jdbc:h2:tcp://localhost/~/matros")).isNull();
		assertThat(DatabaseGeneration.markerFor("jdbc:postgresql://db:5432/matros")).isNull();
		assertThat(DatabaseGeneration.markerFor("")).isNull();
		assertThat(DatabaseGeneration.markerFor(null)).isNull();

		DatabaseGeneration off = new DatabaseGeneration("jdbc:h2:mem:x");
		assertThat(off.isEnabled()).isFalse();
		assertThat(off.readMarker()).isEqualTo(OptionalLong.empty());
	}

	@Test
	void markerSitsNextToTheDatabaseFile() {
		String base = dir.resolve("household/workspace/db/matros").toString().replace('\\', '/');
		Path marker = DatabaseGeneration.markerFor("jdbc:h2:file:" + base + ";MODE=PostgreSQL;AUTO_SERVER=FALSE");
		assertThat(marker).isEqualTo(dir.resolve("household/workspace/db/matros.generation"));
		assertThat(DatabaseGeneration.databaseFile("jdbc:h2:" + base))
				.isEqualTo(dir.resolve("household/workspace/db/matros.mv.db"));
	}
}
