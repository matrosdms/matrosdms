/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.domain.action.MAction;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.service.domain.ActionService;
import net.schwehla.matrosdms.service.message.CreateActionMessage;
import net.schwehla.matrosdms.service.message.UpdateActionMessage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/actions")
public class ActionController {

	@Autowired
	ActionService actionService;

	@GetMapping
	@Operation(summary = "Get tasks with filters and pagination")
	public ResponseEntity<Page<MAction>> getActions(
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser user,
			@RequestParam(required = false) List<EActionStatus> status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime minDate,
			@RequestParam(required = false) String assignee,
			@PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {

		// FIX: Safe Sort Mapping
		Sort mappedSort = Sort.unsorted();
		for (Sort.Order order : pageable.getSort()) {
			String prop = order.getProperty();

			if ("itemIdentifier".equals(prop)) {
				prop = "item.name";
			} else if ("contextIdentifier".equals(prop)) {
				prop = "context.name";
			} else if ("assignee".equals(prop)) {
				prop = "assignee.name";
			} else if ("creator".equals(prop)) {
				prop = "creator.name";
			}

			mappedSort = mappedSort.and(Sort.by(order.getDirection(), prop));
		}
		Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);

		Page<MAction> page = actionService.searchActions(status, assignee, minDate, safePageable);
		return ResponseEntity.ok(page);
	}

	@GetMapping("/item/{itemUuid}")
	@Operation(summary = "Get tasks linked to a specific document")
	public ResponseEntity<List<MAction>> getActionsForItem(@PathVariable String itemUuid) {
		List<MAction> list = actionService.getActionsForItem(itemUuid);
		return ResponseEntity.ok(list);
	}

	@PostMapping
	@Operation(summary = "Create a new Task")
	public ResponseEntity<MAction> createAction(
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser user,
			@Valid @RequestBody CreateActionMessage message) {

		MAction created = actionService.createAction(message, user.getUuid());
		return new ResponseEntity<>(created, HttpStatus.CREATED);
	}

	@PutMapping("/{uuid}")
	@Operation(summary = "Update Task (Status, Assignee, etc)")
	public ResponseEntity<MAction> updateAction(
			@PathVariable String uuid, @RequestBody UpdateActionMessage message) {
		MAction updated = actionService.updateAction(uuid, message);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{uuid}")
	@Operation(summary = "Delete Task")
	public ResponseEntity<Void> deleteAction(@PathVariable String uuid) {
		actionService.deleteAction(uuid);
		return ResponseEntity.noContent().build();
	}
}
