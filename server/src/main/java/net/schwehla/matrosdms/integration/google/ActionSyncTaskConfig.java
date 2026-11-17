/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.ExecutionOperations;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;

/**
 * The persistent "sync this action" job. Being a db-scheduler job, it survives a
 * restart, and a laptop that is offline for a while catches up once it is online.
 */
@Configuration
public class ActionSyncTaskConfig {

	private static final Logger log = LoggerFactory.getLogger(ActionSyncTaskConfig.class);

	public static final String TASK_ACTION_SYNC = "action-sync";
	public static final String TASK_GOOGLE_PULL = "google-pull";

	static final Duration FIRST_RETRY = Duration.ofSeconds(30);
	static final Duration MAX_RETRY = Duration.ofHours(1);
	/** 30s, 1m, 2m … then hourly: about two days of trying before giving up. */
	static final int MAX_ATTEMPTS = 60;

	/** The task data is just the action uuid: the engine reads everything else fresh. */
	@Bean
	public Task<String> actionSyncTask(ActionSyncEngine engine) {
		return Tasks.oneTime(TASK_ACTION_SYNC, String.class)
				.onFailure(new CappedBackoff())
				.execute((instance, ctx) -> engine.reconcile(instance.getData()));
	}

	/**
	 * Looks for changes made in Google. Recurring tasks are started by the db-scheduler
	 * starter; while Google is not configured each run returns at once.
	 */
	@Bean
	public RecurringTask<Void> googlePullTask(GooglePullService pull, AppServerSpringConfig config) {
		Duration interval = config.getIntegrations().getGoogle().getPollInterval();
		return Tasks.recurring(TASK_GOOGLE_PULL, FixedDelay.of(interval))
				.execute((instance, ctx) -> pull.pollAll());
	}

	/** Delay before retry number {@code failures} (1-based): doubling from 30s, capped at 1h. */
	static Duration retryDelay(int failures) {
		long millis = FIRST_RETRY.toMillis() << Math.min(Math.max(failures - 1, 0), 20);
		return millis >= MAX_RETRY.toMillis() ? MAX_RETRY : Duration.ofMillis(millis);
	}

	static final class CappedBackoff implements FailureHandler<String> {
		@Override
		public void onFailure(ExecutionComplete complete, ExecutionOperations<String> operations) {
			int failures = complete.getExecution().consecutiveFailures + 1;
			if (failures >= MAX_ATTEMPTS) {
				log.error("Giving up syncing action {} to Google after {} attempts. "
						+ "It is retried with its next change or 'Sync now' on the profile page.",
						complete.getExecution().taskInstance.getData(), failures);
				operations.stop();
				return;
			}
			operations.reschedule(complete, complete.getTimeDone().plus(retryDelay(failures)));
		}
	}
}
