/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.repository.ActionRepository;
import net.schwehla.matrosdms.repository.ActionSyncRepository;
import net.schwehla.matrosdms.repository.GoogleAccountRepository;
import net.schwehla.matrosdms.util.AfterCommit;

/**
 * Turns action changes into sync jobs, without ever making the caller wait: the job
 * is scheduled after the commit (a rolled-back change schedules nothing), from a
 * virtual thread, so even the scheduler insert stays off the request thread.
 */
@Service
public class ActionSyncDispatcher {

	private static final Logger log = LoggerFactory.getLogger(ActionSyncDispatcher.class);

	private static final List<EActionStatus> OPEN = Arrays.stream(EActionStatus.values())
			.filter(s -> !s.isCompleted())
			.toList();

	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	@Autowired
	AppServerSpringConfig config;
	@Autowired
	Scheduler scheduler;
	@Autowired
	@Qualifier("actionSyncTask")
	Task<String> actionSyncTask;
	@Autowired
	GoogleAccountRepository accounts;
	@Autowired
	ActionSyncRepository syncs;
	@Autowired
	ActionRepository actions;

	/** Call from inside the transaction that changed (or deleted) the action. */
	public void actionChanged(String actionUuid) {
		if (!config.getIntegrations().getGoogle().isConfigured()) {
			return;
		}
		AfterCommit.run(() -> executor.execute(() -> scheduleIfRelevant(actionUuid)));
	}

	/**
	 * Schedules a sync for every open action assigned to the user plus everything
	 * already pushed for them: after connecting, reconnecting, changing the toggles,
	 * or "Sync now".
	 *
	 * @return the number of actions scheduled
	 */
	public int resyncUser(String userUuid) {
		Set<String> uuids = new LinkedHashSet<>(actions.findUuidsByAssigneeAndStatusIn(userUuid, OPEN));
		uuids.addAll(syncs.findActionUuidsByAccount(userUuid));
		executor.execute(() -> uuids.forEach(this::schedule));
		log.info("Google sync: scheduled {} actions for user {}", uuids.size(), userUuid);
		return uuids.size();
	}

	private void scheduleIfRelevant(String actionUuid) {
		try {
			// Nobody connected and nothing pushed before: no job at all
			if (accounts.existsByRefreshTokenIsNotNull() || syncs.existsByActionUuid(actionUuid)) {
				schedule(actionUuid);
			}
		} catch (Exception e) {
			log.error("Could not schedule Google sync for action {}", actionUuid, e);
		}
	}

	private void schedule(String actionUuid) {
		try {
			// Unique instance per change: a job already running must not swallow a newer
			// change. Extra jobs are cheap, the engine reconciles from current state.
			String id = actionUuid + "-" + Long.toString(System.nanoTime(), 36);
			scheduler.schedule(actionSyncTask.instance(id, actionUuid), Instant.now());
		} catch (Exception e) {
			log.error("Could not schedule Google sync for action {}", actionUuid, e);
		}
	}

	@PreDestroy
	void shutdown() {
		executor.shutdown();
	}
}
