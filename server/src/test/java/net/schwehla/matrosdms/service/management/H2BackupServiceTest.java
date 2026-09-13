/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class H2BackupServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 13, 21, 0);

	private H2BackupService service;

	@BeforeEach
	void setUp() {
		service = new H2BackupService();
		ReflectionTestUtils.setField(service, "retentionDays", 90);
		ReflectionTestUtils.setField(service, "keepAllDays", 3);
		ReflectionTestUtils.setField(service, "minKeep", 3);
	}

	/** Newest first, the order cleanup sorts into. */
	private static List<Path> backups(String... stamps) {
		return Arrays.stream(stamps).map(s -> Path.of("matros-db-startup-" + s + ".zip")).toList();
	}

	private static List<String> names(List<Path> paths) {
		return paths.stream().map(p -> p.getFileName().toString()).toList();
	}

	@Test
	void aBurstOfRestartsCostsOneSlotPerDayNotOnePerRestart() {
		// The real failure: six starts in one evening used to push weeks of history off the end.
		List<Path> doomed = service.selectForDeletion(backups(
				"2026-09-13_20-41-39", "2026-09-13_20-39-59", "2026-09-13_19-49-27", "2026-09-13_18-45-16",
				"2026-09-10_10-00-00", "2026-09-10_09-00-00",
				"2026-08-04_13-33-00"), NOW);

		// Only the second snapshot of 10 September goes; August survives the burst.
		assertThat(names(doomed)).containsExactly("matros-db-startup-2026-09-10_09-00-00.zip");
	}

	@Test
	void everySnapshotInsideTheKeepAllWindowSurvives() {
		List<Path> doomed = service.selectForDeletion(backups(
				"2026-09-13_20-41-39", "2026-09-13_20-39-59", "2026-09-13_19-49-27",
				"2026-09-13_18-45-16", "2026-09-13_17-10-35", "2026-09-12_08-00-00"), NOW);

		assertThat(doomed).isEmpty();
	}

	@Test
	void olderThanRetentionIsDeletedEvenAsTheOnlyOneOfItsDay() {
		List<Path> doomed = service.selectForDeletion(backups(
				"2026-09-13_20-41-39", "2026-09-13_20-39-59", "2026-09-13_19-49-27",
				"2026-07-01_04-00-00", "2026-01-05_04-00-00"), NOW);

		// 90 days back from 2026-09-13 is 2026-06-15: July stays, January goes.
		assertThat(names(doomed)).containsExactly("matros-db-startup-2026-01-05_04-00-00.zip");
	}

	@Test
	void minKeepWinsOverEveryDateRule() {
		// A machine started twice a year must not end up with nothing to restore from.
		List<Path> doomed = service.selectForDeletion(backups(
				"2024-05-01_10-00-00", "2023-05-01_10-00-00", "2022-05-01_10-00-00"), NOW);

		assertThat(doomed).isEmpty();
	}

	@Test
	void readsTheTimestampFromTheNameNotTheFileTime() {
		// A cloud client that re-downloads a backup rewrites mtime; retention must not be fooled.
		assertThat(H2BackupService.backupTime(Path.of("matros-db-startup-2026-08-04_13-33-00.zip")))
				.isEqualTo(LocalDateTime.of(2026, 8, 4, 13, 33, 0));
	}

	@Test
	void fallsBackToTheFileTimeForAnUnparsableName(@TempDir Path dir) throws Exception {
		Path renamed = Files.createFile(dir.resolve("matros-db-startup-kopie.zip"));
		Files.setLastModifiedTime(renamed, java.nio.file.attribute.FileTime.fromMillis(1_600_000_000_000L));

		assertThat(H2BackupService.backupTime(renamed))
				.isEqualTo(LocalDateTime.ofInstant(
						java.time.Instant.ofEpochMilli(1_600_000_000_000L), java.time.ZoneId.systemDefault()));
	}
}
