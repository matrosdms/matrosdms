/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.domain.action.ActionLog;
import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.domain.action.MAction;
import net.schwehla.matrosdms.domain.api.EBroadcastSource;
import net.schwehla.matrosdms.domain.api.EBroadcastType;
import net.schwehla.matrosdms.entity.DBAction;
import net.schwehla.matrosdms.messagebus.VUEMessageBus;
import net.schwehla.matrosdms.plugin.PluginManager;
import net.schwehla.matrosdms.repository.ActionRepository;
import net.schwehla.matrosdms.service.mapper.MActionMapper;
import net.schwehla.matrosdms.service.message.ActionChangedMessage;
import net.schwehla.matrosdms.util.AfterCommit;

/**
 * Applies changes that came from outside the UI (ticked off in Google Tasks, event
 * moved in Google Calendar) to an action.
 *
 * <p>Separate from {@link ActionService}: that one schedules sync jobs, and the sync
 * job is what calls this. It deliberately schedules nothing - the caller pushes the
 * merged state itself, in the same job.
 */
@Service
public class ActionRemoteChangeService {

	/** One field change from a remote system; {@code status} null = leave as is. */
	public record RemoteChange(EActionStatus status, boolean dueChanged, LocalDateTime due, List<String> log) {
		public boolean isEmpty() {
			return status == null && !dueChanged;
		}
	}

	@Autowired
	ActionRepository actionRepository;
	@Autowired
	MActionMapper actionMapper;
	@Autowired
	PluginManager pluginManager;
	@Autowired
	VUEMessageBus messageBus;

	/** @return the updated action, or null when it no longer exists */
	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "actionList", allEntries = true),
			@CacheEvict(value = "actions", key = "#actionUuid")
	})
	public MAction apply(String actionUuid, RemoteChange change, String source) {
		DBAction action = actionRepository.findByUuid(actionUuid).orElse(null);
		if (action == null || change.isEmpty()) {
			return null;
		}

		if (change.status() != null && change.status() != action.getStatus()) {
			boolean wasCompleted = action.getStatus().isCompleted();
			action.setStatus(change.status());
			if (change.status().isCompleted() && !wasCompleted) {
				action.setCompletedDate(LocalDateTime.now());
			} else if (!change.status().isCompleted() && wasCompleted) {
				action.setCompletedDate(null);
			}
		}
		if (change.dueChanged()) {
			action.setDueDate(change.due());
		}
		for (String entry : change.log()) {
			action.getHistory().add(new ActionLog(source, entry));
		}

		DBAction saved = actionRepository.save(action);
		MAction result = actionMapper.entityToModel(saved);
		pluginManager.notifyActionUpdated(result);

		// Open task lists reload; without this the UI would hold a stale version and
		// its next save of this task would fail with a conflict
		ActionChangedMessage message = new ActionChangedMessage(actionUuid, saved.getName(), source,
				String.join(", ", change.log()));
		AfterCommit.run(() -> messageBus.sendMessageToGUI(EBroadcastSource.ACTION, EBroadcastType.STATUS, message));
		return result;
	}
}
