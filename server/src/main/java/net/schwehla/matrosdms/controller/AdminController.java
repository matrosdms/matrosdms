/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;

import net.schwehla.matrosdms.domain.admin.EJobType;
import net.schwehla.matrosdms.service.management.H2BackupService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	Scheduler scheduler;
	@Autowired
	H2BackupService backupService;

	@Autowired
	Task<Void> reindexTask;
	@Autowired
	Task<Void> integrityTask;
	@Autowired
	Task<Void> exportTask; // NEW INJECTION

	@PostMapping("/jobs/{type}")
	@Operation(summary = "Start a system job manually")
	public ResponseEntity<String> startJob(
			@PathVariable EJobType type, @RequestParam(required = false) String config) {

		String instanceId = "manual-" + System.currentTimeMillis();

		switch (type) {
			case REINDEX_SEARCH:
				scheduler.schedule(reindexTask.instance(instanceId), Instant.now());
				break;
			case INTEGRITY_CHECK:
				scheduler.schedule(integrityTask.instance(instanceId), Instant.now());
				break;
			case EXPORT_ARCHIVE: // NEW CASE
				scheduler.schedule(exportTask.instance(instanceId), Instant.now());
				break;
			default:
				return ResponseEntity.badRequest().body("Job Type not supported for manual trigger");
		}

		return ResponseEntity.accepted().body("Scheduled: " + type);
	}

	@PostMapping("/backup")
	public ResponseEntity<String> triggerBackup() {
		backupService.createBackup();
		return ResponseEntity.ok("Backup initiated.");
	}
}
