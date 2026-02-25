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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
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

	private static final int MAX_STARTUP_BACKUPS = 10;
	private static final int MAX_DAILY_BACKUPS = 7;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Value("${app.server.repository-path}/db-backup")
	private String backupBasePath;

	@Value("${app.backup.on-startup:true}")
	private boolean backupOnStartup;

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
		if (backupOnStartup) {
			log.info("[Backup] Application Started. Creating Snapshot (Async)...");
			if (createBackup("startup")) {
				cleanupOldBackups("startup", MAX_STARTUP_BACKUPS);
			}
		}
	}

	@Scheduled(cron = "0 0 3 * * ?")
	public void performDailyBackup() {
		log.info("[Backup] Performing Scheduled Nightly Backup...");
		if (createBackup("daily")) {
			cleanupOldBackups("daily", MAX_DAILY_BACKUPS);
		}
	}

	public void createBackup() {
		createBackup("manual");
	}

	private boolean createBackup(String type) {
		try {
			Path backupDir = Paths.get(backupBasePath);
			if (!Files.exists(backupDir))
				Files.createDirectories(backupDir);

			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
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

	private void cleanupOldBackups(String type, int maxToKeep) {
		try (Stream<Path> files = Files.list(Paths.get(backupBasePath))) {
			List<Path> backups = files
					.filter(p -> p.getFileName().toString().startsWith("matros-db-" + type))
					.filter(p -> p.toString().endsWith(".zip"))
					.sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
					.toList();

			if (backups.size() > maxToKeep) {
				int toDelete = backups.size() - maxToKeep;
				for (int i = 0; i < toDelete; i++) {
					Path old = backups.get(i);
					log.info("[Backup] Rotating: Deleting old file {}", old.getFileName());
					Files.deleteIfExists(old);
				}
			}
		} catch (IOException e) {
			log.error("[Backup] Cleanup failed", e);
		}
	}
}
