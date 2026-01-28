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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import net.schwehla.matrosdms.domain.action.ActionLog;
import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.domain.action.MAction;
import net.schwehla.matrosdms.entity.DBAction;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.plugin.PluginManager;
import net.schwehla.matrosdms.repository.ActionRepository;
import net.schwehla.matrosdms.repository.ContextRepository;
import net.schwehla.matrosdms.repository.ItemRepository;
import net.schwehla.matrosdms.repository.UserRepository;
import net.schwehla.matrosdms.service.mapper.MActionMapper;
import net.schwehla.matrosdms.service.message.CreateActionMessage;
import net.schwehla.matrosdms.service.message.UpdateActionMessage;
import net.schwehla.matrosdms.util.UUIDProvider;

@Service
@Transactional
public class ActionService {

	@Autowired
	ActionRepository actionRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	ItemRepository itemRepository;
	@Autowired
	ContextRepository contextRepository;
	@Autowired
	MActionMapper actionMapper;
	@Autowired
	UUIDProvider uuidProvider;
	@Autowired
	PluginManager pluginManager; // NEW

	@CacheEvict(value = "actionList", allEntries = true)
	public MAction createAction(CreateActionMessage msg, String creatorUuid) {
		DBAction action = actionMapper.modelToEntity(msg);
		action.setUuid(uuidProvider.getTimeBasedUUID());

		DBUser creator = userRepository
				.findByUuid(creatorUuid)
				.orElseThrow(
						() -> new ResponseStatusException(
								HttpStatus.NOT_FOUND, "Creator User not found: " + creatorUuid));
		action.setCreator(creator);

		if (msg.getAssigneeIdentifier() != null) {
			action.setAssignee(
					userRepository
							.findByUuid(msg.getAssigneeIdentifier())
							.orElseThrow(
									() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found")));
		} else {
			action.setAssignee(creator);
		}

		if (msg.getItemIdentifier() != null) {
			action.setItem(
					itemRepository
							.findByUuid(msg.getItemIdentifier())
							.orElseThrow(
									() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found")));
		}

		if (msg.getContextIdentifier() != null) {
			action.setContext(
					contextRepository
							.findByUuid(msg.getContextIdentifier())
							.orElseThrow(
									() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Context not found")));
		}

		action.getHistory().add(new ActionLog(creator.getName(), "Created Task"));

		action = actionRepository.save(action);
		MAction result = actionMapper.entityToModel(action);

		// Notify Plugins
		pluginManager.notifyActionCreated(result);

		return result;
	}

	@Caching(evict = {
			@CacheEvict(value = "actionList", allEntries = true),
			@CacheEvict(value = "actions", key = "#uuid")
	})
	public MAction updateAction(String uuid, UpdateActionMessage msg) {
		DBAction action = actionRepository
				.findByUuid(uuid)
				.orElseThrow(
						() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found"));

		EActionStatus oldStatus = action.getStatus();
		EActionStatus newStatus = msg.getStatus() != null ? msg.getStatus() : oldStatus;
		boolean statusChanged = oldStatus != newStatus;

		if (newStatus.isCompleted() && !oldStatus.isCompleted()) {
			action.setCompletedDate(LocalDateTime.now());
		} else if (!newStatus.isCompleted() && oldStatus.isCompleted()) {
			action.setCompletedDate(null);
		}

		if (msg.getAssigneeIdentifier() != null
				&& !msg.getAssigneeIdentifier().equals(action.getAssignee().getUuid())) {
			action.setAssignee(
					userRepository
							.findByUuid(msg.getAssigneeIdentifier())
							.orElseThrow(
									() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "New Assignee not found")));
			logChange("Reassigned to " + action.getAssignee().getName(), action);
		}

		actionMapper.updateEntity(msg, action);

		if (statusChanged) {
			logChange("Status changed: " + oldStatus + " -> " + newStatus, action);
		}

		DBAction saved = actionRepository.save(action);
		MAction result = actionMapper.entityToModel(saved);

		// Notify Plugins
		pluginManager.notifyActionUpdated(result);

		return result;
	}

	private void logChange(String message, DBAction action) {
		String actor = "System";
		try {
			Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			if (principal instanceof DBUser) {
				actor = ((DBUser) principal).getName();
			} else if (principal instanceof String) {
				actor = (String) principal;
			}
		} catch (Exception ignored) {
		}
		action.getHistory().add(new ActionLog(actor, message));
	}

	@Caching(evict = {
			@CacheEvict(value = "actionList", allEntries = true),
			@CacheEvict(value = "actions", key = "#uuid")
	})
	public void deleteAction(String uuid) {
		DBAction action = actionRepository
				.findByUuid(uuid)
				.orElseThrow(
						() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found"));
		actionRepository.delete(action);
		pluginManager.notifyActionDeleted(uuid);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "actions", key = "#uuid")
	public MAction getAction(String uuid) {
		DBAction action = actionRepository
				.findByUuid(uuid)
				.orElseThrow(
						() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found"));
		return actionMapper.entityToModel(action);
	}

	@Transactional(readOnly = true)
	public Page<MAction> searchActions(
			List<EActionStatus> statuses, String assigneeUuid, LocalDateTime minDate, Pageable pageable) {
		return actionRepository
				.findByFilters(statuses, assigneeUuid, minDate, pageable)
				.map(actionMapper::entityToModel);
	}

	@Transactional(readOnly = true)
	public List<MAction> getActionsForItem(String itemUuid) {
		return actionMapper.map(actionRepository.findByItemUuid(itemUuid));
	}
}
