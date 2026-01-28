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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import net.schwehla.matrosdms.domain.core.MUser;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.service.domain.UserService;
import net.schwehla.matrosdms.service.message.CreateUserMessage;
import net.schwehla.matrosdms.service.message.SavedSearchMessage;
import net.schwehla.matrosdms.service.message.UpdateUserMessage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/users")
public class UserController {

	@Autowired
	UserService userService;

	// --- STANDARD CRUD ---

	@PostMapping
	@Operation(summary = "Create a new User")
	public ResponseEntity<MUser> createUser(@Valid @RequestBody CreateUserMessage MUser) {
		MUser user = userService.createUser(MUser);
		return new ResponseEntity<>(user, HttpStatus.CREATED);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update User")
	public ResponseEntity<MUser> updateUser(
			@PathVariable("id") String id, @Valid @RequestBody UpdateUserMessage msg) {
		MUser user = userService.updateUser(id, msg);
		return new ResponseEntity<>(user, HttpStatus.OK);
	}

	@GetMapping
	@Operation(summary = "Get all users")
	public ResponseEntity<List<MUser>> loadUserList() {
		var userList = userService.loadUserList();
		if (userList.isEmpty())
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		return new ResponseEntity<>(userList, HttpStatus.OK);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get User by id")
	public ResponseEntity<MUser> loadUserDetail(@PathVariable("id") String tsid) {
		MUser user = userService.loadUserDetail(tsid);
		return new ResponseEntity<>(user, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete user by id")
	public ResponseEntity<HttpStatus> deleteUser(@PathVariable("id") String tsid) {
		userService.deleteUser(tsid);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	// --- SAVED SEARCHES (PERSISTENT) ---

	@GetMapping("/me/searches")
	@Operation(summary = "Get saved searches for current user")
	public ResponseEntity<List<SavedSearchMessage>> getMySearches(
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser user) {
		return ResponseEntity.ok(userService.getSavedSearches(user.getUuid()));
	}

	@PostMapping("/me/searches")
	@Operation(summary = "Save a search (Name + MQL). Overwrites if name exists.")
	public ResponseEntity<Void> addSearch(
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser user,
			@RequestBody SavedSearchMessage search) {
		userService.addSavedSearch(user.getUuid(), search);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/me/searches/{name}")
	@Operation(summary = "Delete a saved search by name")
	public ResponseEntity<Void> deleteSearch(
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser user, @PathVariable String name) {
		userService.removeSavedSearch(user.getUuid(), name);
		return ResponseEntity.ok().build();
	}
}
