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
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.attribute.MAttributeType;
import net.schwehla.matrosdms.service.domain.AttributeService;
import net.schwehla.matrosdms.service.message.CreateAttributeMessage;
import net.schwehla.matrosdms.service.message.UpdateAttributeMessage;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class AttributeController {

	@Autowired
	AttributeService attributeService;

	@GetMapping("/attribute-types")
	@Operation(summary = "Get available attribute definitions (Flexfields)")
	public ResponseEntity<List<MAttributeType>> loadAttributeTypes() {
		List<MAttributeType> list = attributeService.loadAttributeTypes();
		return new ResponseEntity<>(list, HttpStatus.OK);
	}

	@PostMapping("/attribute-type")
	@Operation(summary = "Define a new Flexfield type (e.g. 'Amount')")
	public ResponseEntity<MAttributeType> createAttributeType(
			@Valid @RequestBody CreateAttributeMessage message) {
		MAttributeType created = attributeService.createAttributeType(message);
		return new ResponseEntity<>(created, HttpStatus.CREATED);
	}

	@PutMapping("/attribute-type/{uuid}")
	@Operation(summary = "Update definition (Rename)")
	public ResponseEntity<MAttributeType> updateAttributeType(
			@PathVariable("uuid") String uuid, @Valid @RequestBody UpdateAttributeMessage message) {
		MAttributeType updated = attributeService.updateAttributeType(uuid, message);
		return new ResponseEntity<>(updated, HttpStatus.OK);
	}

	@DeleteMapping("/attribute-type/{uuid}")
	@Operation(summary = "Delete attribute type")
	public ResponseEntity<HttpStatus> deleteAttributeType(@PathVariable("uuid") String uuid) {
		attributeService.deleteAttributeType(uuid);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
