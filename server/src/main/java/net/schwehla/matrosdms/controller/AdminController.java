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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;

import net.schwehla.matrosdms.domain.admin.EJobType;
import net.schwehla.matrosdms.domain.api.StoreCheckResponse;
import net.schwehla.matrosdms.service.management.H2BackupService;
import net.schwehla.matrosdms.service.management.StoreConsistencyService;

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
	Task<Void> exportTask;
	@Autowired
	StoreConsistencyService storeConsistency;

	@PostMapping("/jobs/{type}")
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Start a system job manually")
	public ResponseEntity<String> startJob(
			@PathVariable EJobType type, @RequestParam(required = false) String config) {

		// One instance id per job: while a run is queued or running, a second start is refused
		// instead of running the same job twice side by side.
		String instanceId = "manual";
		Task<Void> task = switch (type) {
			case REINDEX_SEARCH -> reindexTask;
			case INTEGRITY_CHECK -> integrityTask;
			case EXPORT_ARCHIVE -> exportTask;
		};

		if (!scheduler.scheduleIfNotExists(task.instance(instanceId), Instant.now())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(type + " is already queued or running.");
		}
		return ResponseEntity.accepted().body("Scheduled: " + type);
	}

	@GetMapping("/store-check")
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Last comparison of the document store with the database")
	public ResponseEntity<StoreCheckResponse> storeCheck() {
		return ResponseEntity.ok(StoreCheckResponse.of(storeConsistency.latest()));
	}

	@PostMapping("/backup")
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> triggerBackup() {
		if (backupService.createBackup()) {
			return ResponseEntity.ok("Backup completed.");
		}
		return ResponseEntity.internalServerError().body("Backup FAILED - check server logs.");
	}
}
