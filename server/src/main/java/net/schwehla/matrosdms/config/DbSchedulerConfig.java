/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;

import net.schwehla.matrosdms.domain.admin.EJobType;
import net.schwehla.matrosdms.entity.admin.DBAdminJob;
import net.schwehla.matrosdms.entity.admin.DBAdminJob.JobStatus;
import net.schwehla.matrosdms.repository.AdminJobRepository;
import net.schwehla.matrosdms.service.SearchService;
import net.schwehla.matrosdms.service.domain.AdminService;

@Configuration
public class DbSchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(DbSchedulerConfig.class);

    public static final String TASK_REINDEX_ALL = "reindex-all";
    public static final String TASK_INDEX_ITEM = "index-item";
    public static final String TASK_INTEGRITY = "integrity-check";
    public static final String TASK_EXPORT = "export-archive";

    // --- BEAN DEFINITIONS ---

    @Bean
    public Task<Long> indexItemTask(SearchService searchService) {
        // Quick background task, usually doesn't need persistent history log in UI
        return Tasks.oneTime(TASK_INDEX_ITEM, Long.class)
                .execute((inst, ctx) -> {
                    Long itemId = inst.getData();
                    log.debug("JOB [Index]: Indexing Item ID: {}", itemId);
                    searchService.indexSingleItem(itemId);
                });
    }

    @Bean
    public Task<Void> reindexTask(SearchService searchService, AdminJobRepository jobRepo) {
        return Tasks.oneTime(TASK_REINDEX_ALL, Void.class)
                .execute((inst, ctx) -> {
                    // 1. Create Log Entry
                    DBAdminJob job = createJobLog(jobRepo, EJobType.REINDEX_SEARCH, "Reindexing Lucene...");
                    
                    try {
                        log.info("JOB [Reindex All]: Starting...");
                        searchService.reindexAll();
                        
                        // 2. Success
                        completeJobLog(jobRepo, job, "Reindex Complete");
                        log.info("JOB [Reindex All]: Completed.");
                        
                    } catch (Exception e) {
                        // 3. Failure
                        failJobLog(jobRepo, job, e);
                        throw e; // Rethrow to let db-scheduler handle retry logic if needed
                    }
                });
    }

    @Bean
    public Task<Void> integrityTask(AdminService adminService, AdminJobRepository jobRepo) {
        return Tasks.oneTime(TASK_INTEGRITY, Void.class)
                .execute((inst, ctx) -> {
                    DBAdminJob job = createJobLog(jobRepo, EJobType.INTEGRITY_CHECK, "Checking Files...");
                    try {
                        log.info("JOB [Integrity]: Starting...");
                        var report = adminService.runIntegrityCheck();
                        
                        String result = "Scanned " + report.getTotalDbItems() + ", Missing: " + report.getMissingCount();
                        completeJobLog(jobRepo, job, result);
                        
                        log.info("JOB [Integrity]: Completed.");
                    } catch (Exception e) {
                        failJobLog(jobRepo, job, e);
                        throw e;
                    }
                });
    }

    @Bean
    public Task<Void> exportTask(AdminService adminService, AdminJobRepository jobRepo) {
        return Tasks.oneTime(TASK_EXPORT, Void.class)
                .execute((inst, ctx) -> {
                    DBAdminJob job = createJobLog(jobRepo, EJobType.EXPORT_ARCHIVE, "Exporting to ZIP...");
                    try {
                        log.info("JOB [Export]: Starting Archive Export...");
                        adminService.createArchiveExport();
                        
                        completeJobLog(jobRepo, job, "Export saved to /export folder");
                        log.info("JOB [Export]: Completed.");
                    } catch (Exception e) {
                        failJobLog(jobRepo, job, e);
                        throw e;
                    }
                });
    }

    // --- Helper Methods to manage DB History ---

    private DBAdminJob createJobLog(AdminJobRepository repo, EJobType type, String info) {
        DBAdminJob job = new DBAdminJob();
        job.setType(type);
        job.setStatus(JobStatus.RUNNING);
        job.setStartTime(LocalDateTime.now());
        job.setProgressInfo(info);
        return repo.save(job);
    }

    private void completeJobLog(AdminJobRepository repo, DBAdminJob job, String resultInfo) {
        job.setStatus(JobStatus.COMPLETED);
        job.setEndTime(LocalDateTime.now());
        job.setProgressInfo(resultInfo);
        repo.save(job);
    }

    private void failJobLog(AdminJobRepository repo, DBAdminJob job, Exception e) {
        job.setStatus(JobStatus.FAILED);
        job.setEndTime(LocalDateTime.now());
        job.setProgressInfo("Error: " + e.getMessage());
        job.addLog("ERROR", e.getMessage()); // Assuming DBAdminJob has this helper
        repo.save(job);
    }
}