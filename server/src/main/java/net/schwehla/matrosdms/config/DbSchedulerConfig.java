/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;

import net.schwehla.matrosdms.service.SearchService;
import net.schwehla.matrosdms.service.domain.AdminService;

@Configuration
public class DbSchedulerConfig {

	private static final Logger log = LoggerFactory.getLogger(DbSchedulerConfig.class);

	// REMOVED: ingestTask (primeFileJob) - Ingestion is now handled via @Async

	public static final String TASK_REINDEX_ALL = "reindex-all";
	public static final String TASK_INDEX_ITEM = "index-item";
	public static final String TASK_INTEGRITY = "integrity-check";
	public static final String TASK_EXPORT = "export-archive";

	// --- BEAN DEFINITIONS ---

	@Bean
	public Task<Long> indexItemTask(SearchService searchService) {
		return Tasks.oneTime(TASK_INDEX_ITEM, Long.class)
				.execute(
						(inst, ctx) -> {
							Long itemId = inst.getData();
							log.debug("JOB [Index]: Indexing Item ID: {}", itemId);
							searchService.indexSingleItem(itemId);
						});
	}

	@Bean
	public Task<Void> reindexTask(SearchService searchService) {
		return Tasks.oneTime(TASK_REINDEX_ALL, Void.class)
				.execute(
						(inst, ctx) -> {
							log.info("JOB [Reindex All]: Starting...");
							searchService.reindexAll();
							log.info("JOB [Reindex All]: Completed.");
						});
	}

	@Bean
	public Task<Void> integrityTask(AdminService adminService) {
		return Tasks.oneTime(TASK_INTEGRITY, Void.class)
				.execute(
						(inst, ctx) -> {
							log.info("JOB [Integrity]: Starting...");
							adminService.runIntegrityCheck();
							log.info("JOB [Integrity]: Completed.");
						});
	}

	@Bean
	public Task<Void> exportTask(AdminService adminService) {
		return Tasks.oneTime(TASK_EXPORT, Void.class)
				.execute(
						(inst, ctx) -> {
							log.info("JOB [Export]: Starting Archive Export...");
							adminService.createArchiveExport();
							log.info("JOB [Export]: Completed.");
						});
	}

	// REMOVED: IngestJobData class (no longer needed here)
}
