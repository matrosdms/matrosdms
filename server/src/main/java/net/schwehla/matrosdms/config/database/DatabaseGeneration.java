/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config.database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.OptionalLong;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Notices when the H2 database went back in time.
 *
 * <p>H2 recovers from an unclean shutdown by opening the newest state it can read completely. If
 * the newest chunks of the file are damaged, that can be a state months old, and H2 says nothing:
 * the app starts on old data, and its next backups overwrite the good ones. This happened twice
 * (2026-09-13 and 2026-11-16, both times back to 2026-02-28).
 *
 * <p>So the database counts its save points: table {@code db_generation}, raised at every start and
 * with every periodic checkpoint, after which the new value is fsynced and copied into a small file
 * next to the database ({@code matros.generation}). At start the database must be at least at the
 * value in the file. The file is written only after the database has it on disk, so a database
 * that is behind the file has lost data.
 *
 * <p>Only for an embedded H2 file database: a server database (PostgreSQL, H2 over TCP) does its own
 * crash recovery, and an in-memory one has nothing to lose.
 */
@Component
public class DatabaseGeneration {

	private static final Logger log = LoggerFactory.getLogger(DatabaseGeneration.class);

	static final String MARKER_SUFFIX = ".generation";

	private final Path marker;

	public DatabaseGeneration(@Value("${spring.datasource.url:}") String jdbcUrl) {
		this.marker = markerFor(jdbcUrl);
	}

	/** The marker file for an embedded H2 file database, or {@code null} when there is none to guard. */
	static Path markerFor(String jdbcUrl) {
		Path database = databaseFile(jdbcUrl);
		return database == null ? null
				: database.resolveSibling(stripMvDb(database.getFileName().toString()) + MARKER_SUFFIX);
	}

	/** {@code jdbc:h2:D:/x/db/matros;MODE=...} → {@code D:/x/db/matros.mv.db}; null when not an embedded file database. */
	static Path databaseFile(String jdbcUrl) {
		if (jdbcUrl == null || !jdbcUrl.regionMatches(true, 0, "jdbc:h2:", 0, 8)) {
			return null;
		}
		String location = jdbcUrl.substring(8);
		int options = location.indexOf(';');
		if (options >= 0) {
			location = location.substring(0, options);
		}
		String lower = location.toLowerCase(Locale.ROOT);
		if (lower.startsWith("mem:") || lower.startsWith("tcp:") || lower.startsWith("ssl:")
				|| lower.startsWith("zip:") || location.isBlank()) {
			return null;
		}
		if (lower.startsWith("file:")) {
			location = location.substring(5);
		}
		if (location.startsWith("~")) {
			location = System.getProperty("user.home") + location.substring(1);
		}
		return Path.of(location + ".mv.db").toAbsolutePath().normalize();
	}

	private static String stripMvDb(String name) {
		return name.endsWith(".mv.db") ? name.substring(0, name.length() - 6) : name;
	}

	public boolean isEnabled() {
		return marker != null;
	}

	Path marker() {
		return marker;
	}

	Path databaseFile() {
		return marker == null ? null
				: marker.resolveSibling(marker.getFileName().toString().replace(MARKER_SUFFIX, ".mv.db"));
	}

	/**
	 * Refuses a database that is behind the marker file. Runs before Flyway, so the table may not
	 * exist yet (an older database, or one that fell back to before the table was created): that
	 * counts as save point 0.
	 */
	void check(DataSource dataSource, BackupHint backups, StartupSnapshot snapshot) {
		if (!isEnabled()) {
			return;
		}
		OptionalLong expected = readMarker();
		if (expected.isEmpty()) {
			return; // first start with the guard, or a restore the user confirmed by deleting the file
		}
		long current;
		try (Connection c = dataSource.getConnection()) {
			current = readGeneration(c).orElse(0);
		} catch (SQLException e) {
			// Not ours to diagnose: Flyway opens the database next and reports what is wrong with it.
			log.warn("[DB] Could not read the save point: {}", e.getMessage());
			return;
		}
		if (current < expected.getAsLong()) {
			log.error("[DB] Database is at save point {} but {} says {}: it fell back to an older state. "
					+ "Refusing to start.", current, marker, expected.getAsLong());
			Path kept = snapshot != null ? snapshot.onRefused(marker) : null;
			throw DatabaseStateException.rolledBack(current, expected.getAsLong(), marker, backups, kept);
		}
	}

	/**
	 * Raises the save point, makes it durable, then records it in the marker file. In this order, so
	 * the file never claims more than the disk holds. Failures are logged, never thrown: the guard
	 * must not be the reason MatrosDMS cannot run.
	 */
	public void advance(DataSource dataSource) {
		if (!isEnabled()) {
			return;
		}
		try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
			int updated;
			try (PreparedStatement ps = c.prepareStatement(
					"update db_generation set generation = generation + 1, date_updated = ? where id = 1")) {
				ps.setObject(1, LocalDateTime.now());
				updated = ps.executeUpdate();
			}
			if (updated == 0) {
				try (PreparedStatement ps = c.prepareStatement(
						"insert into db_generation (id, generation, date_updated) values (1, 1, ?)")) {
					ps.setObject(1, LocalDateTime.now());
					ps.executeUpdate();
				}
			}
			if (!c.getAutoCommit()) {
				c.commit();
			}
			long generation = readGeneration(c).orElseThrow();
			st.execute("CHECKPOINT SYNC");
			writeMarker(generation);
			log.debug("[DB] Save point {}", generation);
		} catch (Exception e) {
			log.warn("[DB] Could not record the save point: {}", e.getMessage());
		}
	}

	static OptionalLong readGeneration(Connection c) throws SQLException {
		try (Statement st = c.createStatement();
				ResultSet rs = st.executeQuery("select generation from db_generation where id = 1")) {
			return rs.next() ? OptionalLong.of(rs.getLong(1)) : OptionalLong.empty();
		} catch (SQLException e) {
			if (isMissingTable(e)) {
				return OptionalLong.empty();
			}
			throw e;
		}
	}

	private static boolean isMissingTable(SQLException e) {
		// H2: 42S02 / 42S04 (table not found), PostgreSQL: 42P01
		String state = e.getSQLState();
		return state != null && (state.startsWith("42S0") || state.equals("42P01"));
	}

	OptionalLong readMarker() {
		if (marker == null || !Files.isRegularFile(marker)) {
			return OptionalLong.empty();
		}
		try {
			for (String line : Files.readAllLines(marker, StandardCharsets.UTF_8)) {
				String l = line.strip();
				if (l.startsWith("generation=")) {
					return OptionalLong.of(Long.parseLong(l.substring("generation=".length()).strip()));
				}
			}
		} catch (IOException | NumberFormatException e) {
			log.warn("[DB] Ignoring unreadable {}: {}", marker, e.getMessage());
		}
		return OptionalLong.empty();
	}

	void writeMarker(long generation) throws IOException {
		String content = """
				# MatrosDMS: the newest save point of the database next to this file. At every start the
				# database must be at least at this point, otherwise it fell back to an older state.
				# Restored an older backup on purpose? Delete this file and start MatrosDMS again.
				generation=%d
				updated=%s
				""".formatted(generation, LocalDateTime.now().withNano(0));
		Path tmp = marker.resolveSibling(marker.getFileName() + ".tmp");
		Files.writeString(tmp, content, StandardCharsets.UTF_8);
		try {
			Files.move(tmp, marker, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, marker, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
