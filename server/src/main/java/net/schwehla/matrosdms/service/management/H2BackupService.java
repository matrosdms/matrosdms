/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.management;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Lazy(false)
public class H2BackupService {

	private static final Logger log = LoggerFactory.getLogger(H2BackupService.class);

	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Value("${app.server.repository-path}/db-backup")
	private String backupBasePath;

	@Value("${app.backup.on-startup:true}")
	private boolean backupOnStartup;

	/**
	 * Retention is driven by dates, not by a count of files. A count is the wrong unit here: every
	 * start writes a snapshot, so an evening of restarts (a profile switch is a restart too) used to
	 * evict weeks of history in minutes — exactly when you are most likely to need yesterday's copy.
	 */
	@Value("${app.backup.retention-days:90}")
	private int retentionDays;

	/** Inside this window every snapshot survives, however often the app was restarted. */
	@Value("${app.backup.keep-all-days:3}")
	private int keepAllDays;

	/** A floor for the young installation and the rarely-started one: dates never take us below it. */
	@Value("${app.backup.min-keep:10}")
	private int minKeep;

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
		if (backupOnStartup) {
			log.info("[Backup] Application Started. Creating Snapshot (Async)...");
			if (createBackup("startup")) {
				cleanupOldBackups("startup");
			}
		}
	}

	// Users tend to just close the console window (hard kill, no graceful
	// shutdown) - fsync committed work every interval so nothing older is lost
	@Scheduled(
			fixedDelayString = "${app.database.checkpoint-interval-ms:300000}",
			initialDelayString = "${app.database.checkpoint-interval-ms:300000}")
	public void checkpointDatabase() {
		try {
			jdbcTemplate.execute("CHECKPOINT SYNC");
			log.debug("[DB] CHECKPOINT SYNC completed");
		} catch (Exception e) {
			log.warn("[DB] CHECKPOINT SYNC failed: {}", e.getMessage());
		}
	}

	@Scheduled(cron = "0 0 3 * * ?")
	public void performDailyBackup() {
		log.info("[Backup] Performing Scheduled Nightly Backup...");
		if (createBackup("daily")) {
			cleanupOldBackups("daily");
		}
	}

	public boolean createBackup() {
		return createBackup("manual");
	}

	private boolean createBackup(String type) {
		try {
			Path backupDir = Paths.get(backupBasePath);
			if (!Files.exists(backupDir))
				Files.createDirectories(backupDir);

			String timestamp = LocalDateTime.now().format(STAMP);
			String filename = String.format("matros-db-%s-%s.zip", type, timestamp);
			Path targetFile = backupDir.resolve(filename);

			String sql = String.format(
					"BACKUP TO '%s'", targetFile.toAbsolutePath().toString().replace("\\", "/"));
			jdbcTemplate.execute(sql);

			log.info("[Backup] Success: {}", filename);
			return true;

		} catch (Exception e) {
			log.error("[Backup] FAILED! Database might be corrupt or disk full.", e);
			return false;
		}
	}

	private void cleanupOldBackups(String type) {
		try (Stream<Path> files = Files.list(Paths.get(backupBasePath))) {
			List<Path> backups = files
					.filter(p -> p.getFileName().toString().startsWith("matros-db-" + type))
					.filter(p -> p.toString().endsWith(".zip"))
					// Newest first: the keep-rules below are all expressed from the newest backwards.
					.sorted(Comparator.comparing(H2BackupService::backupTime).reversed())
					.toList();

			for (Path old : selectForDeletion(backups, LocalDateTime.now())) {
				log.info("[Backup] Rotating: Deleting old file {}", old.getFileName());
				Files.deleteIfExists(old);
			}
		} catch (IOException e) {
			log.error("[Backup] Cleanup failed", e);
		}
	}

	/**
	 * Decides which of {@code backups} (newest first) may go. A backup survives when any of these
	 * holds: it is among the newest {@link #minKeep}; it is younger than {@link #keepAllDays}; or it
	 * is the newest one of its calendar day and still within {@link #retentionDays}. So a day of
	 * restarts costs one slot per day rather than one per restart, and history thins out with age
	 * instead of being pushed off the end.
	 */
	List<Path> selectForDeletion(List<Path> backups, LocalDateTime now) {
		List<Path> doomed = new ArrayList<>();
		Set<LocalDate> daysKept = new HashSet<>();

		for (int i = 0; i < backups.size(); i++) {
			Path backup = backups.get(i);
			LocalDateTime taken = backupTime(backup);
			LocalDate day = taken.toLocalDate();

			if (i < minKeep || taken.isAfter(now.minusDays(keepAllDays))) {
				daysKept.add(day);
				continue;
			}
			if (taken.isBefore(now.minusDays(retentionDays))) {
				doomed.add(backup);
				continue;
			}
			// Within the retention window: one survivor per day, and the list is newest-first, so the
			// first one seen for a day is the one that day keeps.
			if (daysKept.add(day)) {
				continue;
			}
			doomed.add(backup);
		}
		return doomed;
	}

	/**
	 * When the backup was taken, read from the name the app itself wrote
	 * ({@code matros-db-<type>-yyyy-MM-dd_HH-mm-ss.zip}). The file's mtime is only the fallback: a
	 * cloud client that re-downloads a backup rewrites mtime, and retention must not treat a
	 * re-synced copy as if it were made today.
	 */
	static LocalDateTime backupTime(Path backup) {
		String name = backup.getFileName().toString();
		if (name.endsWith(".zip") && name.length() > 23) {
			String stamp = name.substring(name.length() - 23, name.length() - 4);
			try {
				return LocalDateTime.parse(stamp, STAMP);
			} catch (DateTimeParseException ignored) {
				// Not one of ours, or renamed by hand — fall through to the file time.
			}
		}
		return LocalDateTime.ofInstant(
				java.time.Instant.ofEpochMilli(backup.toFile().lastModified()), ZoneId.systemDefault());
	}
}
