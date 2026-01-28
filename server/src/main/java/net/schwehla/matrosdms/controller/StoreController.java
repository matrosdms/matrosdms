/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.core.MStore;
import net.schwehla.matrosdms.service.domain.StoreService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/stores") // PLURALIZED
public class StoreController {

	@Autowired
	StoreService storeService;

	@GetMapping("/{id}/next-number")
	@Operation(summary = "Get the next free number for physical filing")
	public ResponseEntity<Integer> getNextStoreItemNumber(@PathVariable("id") String storeUuid) {
		Integer nextNumber = storeService.getNextStoreItemNumber(storeUuid);
		return new ResponseEntity<>(nextNumber, HttpStatus.OK);
	}

	@GetMapping
	@Operation(summary = "Get all stores")
	public ResponseEntity<List<MStore>> loadStoreList() {
		var elementList = storeService.loadStoreList();
		if (elementList.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(elementList, HttpStatus.OK);
	}

	@PostMapping
	@Operation(summary = "Create a new store")
	public ResponseEntity<MStore> createStore(@RequestBody MStore matrosUser) {
		MStore element = storeService.createStore(matrosUser);
		return new ResponseEntity<>(element, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get store by id")
	public ResponseEntity<MStore> loadStoreDetail(@PathVariable("id") String tsid) {
		MStore element = storeService.loadStoreDetail(tsid);
		return new ResponseEntity<>(element, HttpStatus.OK);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update store by id")
	public ResponseEntity<MStore> updateStore(
			@PathVariable("id") String tsid, @RequestBody MStore store) {
		MStore updated = storeService.updateStore(tsid, store);
		return new ResponseEntity<>(updated, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete store by id")
	public ResponseEntity<HttpStatus> deleteStore(@PathVariable("id") String tsid) {
		storeService.deleteStore(tsid);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
