/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config.database;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.service.management.H2BackupService;

/**
 * The startup backup ({@code app.backup.on-startup}), taken from the database file before H2 opens
 * it, so it holds the state before this start: before H2's repair of an uncleanly closed file and
 * before Flyway's migrations.
 *
 * <p>Why before the open: H2 repairs an uncleanly closed file while opening it, and when that makes
 * it fall back to an older state it rewrites the file within seconds (2026-11-16, see
 * {@link DatabaseGeneration}). A backup made after the open, as {@code BACKUP TO} must be, can only
 * capture the result.
 *
 * <p>Two steps, so the start does not wait for compression: before the open the file is copied next
 * to the database ({@code matros.mv.db.before-open}; 100 MB take well under 0.1 s on an SSD). After a
 * successful start {@link #publish()} zips that copy into {@code db-backup} as
 * {@code matros-db-startup-<time>.zip}, the same format {@code BACKUP TO} writes, and deletes it.
 * When the startup guard refuses to start, {@link #onRefused} turns it into
 * {@code matros-db-incident-<time>.zip} instead: the file as it was, possibly with data newer than
 * the last backup. Both kinds are rotated by {@link H2BackupService}'s date-based retention.
 *
 * <p>A {@link BeanFactoryPostProcessor} because those run before any bean is created, so before
 * anything can open a connection.
 */
@Component
public class StartupSnapshot implements BeanFactoryPostProcessor, EnvironmentAware {

	private static final Logger log = LoggerFactory.getLogger(StartupSnapshot.class);

	public static final String STARTUP = "startup";
	public static final String INCIDENT = "incident";
	static final String COPY_SUFFIX = ".before-open";
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	/** This start's copy, waiting to be published (or kept, if the start is refused). */
	record Copy(Path file, Path backupDir, LocalDateTime takenAt) {
	}

	private Environment environment;
	private volatile Copy pending;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		if (!environment.getProperty("app.backup.on-startup", Boolean.class, true)) {
			return;
		}
		Path database = DatabaseGeneration.databaseFile(environment.getProperty("spring.datasource.url"));
		String repository = environment.getProperty("app.server.repository-path");
		if (database != null && repository != null && !repository.isBlank()) {
			take(database, Path.of(repository, "db-backup").toAbsolutePath());
		}
	}

	static Path copyOf(Path database) {
		return database.resolveSibling(database.getFileName() + COPY_SUFFIX);
	}

	/** Copies {@code database} next to itself. Never throws: a missing backup must not stop the start. */
	void take(Path database, Path backupDir) {
		pending = null;
		if (!Files.isRegularFile(database)) {
			return; // first start: nothing there yet
		}
		Path target = copyOf(database);
		Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
		long started = System.nanoTime();
		try {
			Files.copy(database, tmp, StandardCopyOption.REPLACE_EXISTING);
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			pending = new Copy(target, backupDir, LocalDateTime.now());
			log.info("[Backup] Copied {} ({} MB) before opening it, {} ms", database.getFileName(),
					String.format("%.1f", Files.size(target) / 1_048_576.0), (System.nanoTime() - started) / 1_000_000);
		} catch (IOException e) {
			// e.g. another process holds the file (it will not open for us either)
			log.warn("[Backup] Could not copy {} before opening it: {}", database, e.getMessage());
			deleteQuietly(tmp);
		}
	}

	/** After a successful start: zip this start's copy into the backup folder as the startup backup. */
	public Optional<Path> publish() {
		Copy copy = pending;
		pending = null;
		if (copy == null || !Files.isRegularFile(copy.file())) {
			return Optional.empty();
		}
		try {
			Path zip = zip(copy, STARTUP);
			log.info("[Backup] Success: {}", zip.getFileName());
			return Optional.of(zip);
		} catch (IOException e) {
			log.error("[Backup] Startup backup FAILED: {}", e.getMessage());
			return Optional.empty();
		} finally {
			deleteQuietly(copy.file());
		}
	}

	/**
	 * The startup guard refused to start: keep this start's copy as an incident backup. Only the first
	 * refused start of a series holds the file as it was before H2 rewrote it; later attempts copy the
	 * already rewritten file, so their copy is dropped while an incident backup made since
	 * {@code marker} was last written (the last good start or checkpoint) exists.
	 *
	 * @return the incident backup (this one or the one of the first refused start), or null
	 */
	Path onRefused(Path marker) {
		Copy copy = pending;
		pending = null;
		if (copy == null) {
			return null;
		}
		try {
			LocalDateTime lastGood = marker != null && Files.isRegularFile(marker)
					? LocalDateTime.ofInstant(Files.getLastModifiedTime(marker).toInstant(), ZoneId.systemDefault())
					: LocalDateTime.MIN;
			// Backup names have whole seconds: compare at that resolution
			LocalDateTime since = lastGood.equals(LocalDateTime.MIN) ? lastGood
					: lastGood.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
			Optional<Path> earlier = backups(copy.backupDir(), INCIDENT)
					.filter(p -> !H2BackupService.backupTime(p).isBefore(since))
					.max((a, b) -> H2BackupService.backupTime(a).compareTo(H2BackupService.backupTime(b)));
			if (earlier.isPresent()) {
				return earlier.get();
			}
			Path zip = zip(copy, INCIDENT);
			log.warn("[Backup] Kept the database file as it was before this start: {}", zip);
			return zip;
		} catch (IOException e) {
			log.warn("[Backup] Could not keep {}: {}", copy.file(), e.getMessage());
			return null;
		} finally {
			deleteQuietly(copy.file());
		}
	}

	private static Path zip(Copy copy, String type) throws IOException {
		Files.createDirectories(copy.backupDir());
		String name = String.format("matros-db-%s-%s.zip", type, copy.takenAt().format(STAMP));
		Path target = copy.backupDir().resolve(name);
		Path tmp = copy.backupDir().resolve(name + ".tmp");
		// Same layout as H2's BACKUP TO: a single entry named like the database file, so restoring
		// is the same for every backup
		String entry = copy.file().getFileName().toString().replace(COPY_SUFFIX, "");
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp), 1 << 16);
				ZipOutputStream zip = new ZipOutputStream(out)) {
			zip.setLevel(Deflater.BEST_SPEED);
			zip.putNextEntry(new ZipEntry(entry));
			Files.copy(copy.file(), zip);
			zip.closeEntry();
		} catch (IOException e) {
			deleteQuietly(tmp);
			throw e;
		}
		Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
		return target;
	}

	static Stream<Path> backups(Path backupDir, String type) throws IOException {
		if (backupDir == null || !Files.isDirectory(backupDir)) {
			return Stream.empty();
		}
		String prefix = "matros-db-" + type + "-";
		try (Stream<Path> files = Files.list(backupDir)) {
			return files.filter(p -> p.getFileName().toString().startsWith(prefix))
					.filter(p -> p.getFileName().toString().endsWith(".zip"))
					.toList().stream();
		}
	}

	private static void deleteQuietly(Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException ignored) {
			// best effort
		}
	}
}
