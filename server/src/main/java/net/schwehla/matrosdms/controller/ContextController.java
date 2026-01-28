/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.core.EArchivedState;
import net.schwehla.matrosdms.domain.core.MContext;
import net.schwehla.matrosdms.service.domain.ContextService;
import net.schwehla.matrosdms.service.message.CreateContextMessage;
import net.schwehla.matrosdms.service.message.UpdateContextMessage;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/contexts") // PLURALIZED
public class ContextController {

	@Autowired
	ContextService contextService;

	@GetMapping
	@Operation(summary = "Get all contexts (with sorting/limiting)")
	public ResponseEntity<List<MContext>> getAllInfoContext(
			@RequestParam(name = "archiveState", defaultValue = EArchivedState.Names.ONLYACTIVE) EArchivedState archiveState,
			@RequestParam(name = "sort", defaultValue = "name") String sort,
			@RequestParam(name = "limit", defaultValue = "100") int limit) {

		List<MContext> resultList = contextService.loadContextList(archiveState, limit, sort);
		return new ResponseEntity<>(resultList, HttpStatus.OK);
	}

	@PostMapping
	@Operation(summary = "Create a new context")
	public ResponseEntity<MContext> createContext(
			@Valid @RequestBody CreateContextMessage infoContextMessage) {
		MContext context = contextService.createContext(infoContextMessage);
		return new ResponseEntity<>(context, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get context by id")
	public ResponseEntity<MContext> getInfoContextById(@PathVariable("id") String tsid) {
		MContext result = contextService.loadContext(tsid);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update context by id")
	public ResponseEntity<MContext> updateInfoContext(
			@PathVariable("id") String tsid, @Valid @RequestBody UpdateContextMessage message) {
		MContext result = contextService.updateContext(tsid, message);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete context by id")
	public ResponseEntity<HttpStatus> deleteContext(@PathVariable("id") String tsid) {
		contextService.deleteContext(tsid);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
