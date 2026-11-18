/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;

import net.schwehla.matrosdms.domain.admin.EJobStatus;
import net.schwehla.matrosdms.domain.admin.EJobType;
import net.schwehla.matrosdms.domain.admin.MAdminJob;
import net.schwehla.matrosdms.entity.admin.DBAdminJob;
import net.schwehla.matrosdms.entity.admin.DBAdminJob.JobStatus;
import net.schwehla.matrosdms.repository.AdminJobRepository;
import net.schwehla.matrosdms.service.mapper.MAdminJobMapperImpl;

class AdminJobControllerTest {

	@Test
	void aJobThatIsAlreadyQueuedOrRunningIsNotStartedAgain() {
		AdminController controller = new AdminController();
		controller.scheduler = mock(Scheduler.class);
		controller.integrityTask = task("integrity-check");
		controller.reindexTask = task("reindex-all");
		controller.exportTask = task("export-archive");

		when(controller.scheduler.scheduleIfNotExists(any(TaskInstance.class), any(Instant.class)))
				.thenReturn(true, false);

		ResponseEntity<String> first = controller.startJob(EJobType.INTEGRITY_CHECK, null);
		ResponseEntity<String> second = controller.startJob(EJobType.INTEGRITY_CHECK, null);

		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(second.getBody()).contains("already queued or running");
	}

	@Test
	void theJobDetailCarriesResultAndLogLines() {
		DBAdminJob job = new DBAdminJob();
		job.setId(153L);
		job.setType(EJobType.INTEGRITY_CHECK);
		job.setStatus(JobStatus.COMPLETED);
		job.setStartTime(LocalDateTime.of(2026, 11, 17, 22, 6, 8));
		job.setEndTime(LocalDateTime.of(2026, 11, 17, 22, 6, 11));
		job.setProgressInfo("Checked 387 items. Missing: 0");
		job.addLog("INFO", "387 documents in the database, 387 in the document store.");
		job.addLog("INFO", "No problems found.");

		JobController controller = new JobController();
		controller.adminJobRepository = mock(AdminJobRepository.class);
		controller.jobMapper = new MAdminJobMapperImpl();
		when(controller.adminJobRepository.findWithLogsById(anyLong()))
				.thenAnswer(i -> i.getArgument(0).equals(153L) ? Optional.of(job) : Optional.empty());

		MAdminJob detail = controller.getJob(153L).getBody();

		assertThat(detail.getStatus()).isEqualTo(EJobStatus.COMPLETED);
		assertThat(detail.getProgressInfo()).isEqualTo("Checked 387 items. Missing: 0");
		assertThat(detail.getLogs()).extracting(l -> l.getMessage())
				.containsExactly("387 documents in the database, 387 in the document store.", "No problems found.");
		assertThat(controller.getJob(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void tooLongLogLinesAndResultsAreCutToTheColumnSize() {
		DBAdminJob job = new DBAdminJob();
		job.addLog("ERROR", "Missing: " + "x".repeat(400));
		job.setProgressInfo("Error: " + "y".repeat(400));

		assertThat(job.getLogs().get(0).getMessage()).hasSize(255).endsWith("…");
		assertThat(job.getProgressInfo()).hasSize(255).endsWith("…");
	}

	private static OneTimeTask<Void> task(String name) {
		return Tasks.oneTime(name, Void.class).execute((inst, ctx) -> {
		});
	}
}
