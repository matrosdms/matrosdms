/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import net.schwehla.matrosdms.service.domain.ItemBatchService;
import net.schwehla.matrosdms.service.message.BatchRequest;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/items/batch")
public class ItemBatchController {

	@Autowired
	ItemBatchService batchService;

	@PostMapping("/move")
	@Operation(summary = "Batch Move items to a new context (Folder)")
	public ResponseEntity<Void> moveItems(@RequestBody BatchRequest req) {
		batchService.batchMove(req.getItemUuids(), req.getTargetContextUuid());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/tag")
	@Operation(summary = "Batch Add/Remove Categories (Tags)")
	public ResponseEntity<Void> tagItems(@RequestBody BatchRequest req) {
		batchService.batchTag(req.getItemUuids(), req.getAddTags(), req.getRemoveTags());
		return ResponseEntity.ok().build();
	}

	@DeleteMapping
	@Operation(summary = "Batch Delete items")
	public ResponseEntity<Void> deleteItems(@RequestBody BatchRequest req) {
		batchService.batchDelete(req.getItemUuids());
		return ResponseEntity.ok().build();
	}
}
