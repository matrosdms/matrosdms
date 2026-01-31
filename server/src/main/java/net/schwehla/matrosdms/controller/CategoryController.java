/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

import net.schwehla.matrosdms.domain.core.ERootCategory;
import net.schwehla.matrosdms.domain.core.MCategory;
import net.schwehla.matrosdms.domain.core.RootCategoryMeta;
import net.schwehla.matrosdms.service.domain.CategoryImportService;
import net.schwehla.matrosdms.service.domain.CategoryService;
import net.schwehla.matrosdms.service.message.CategoryImportMessage;
import net.schwehla.matrosdms.service.message.CreateCategoryMessage;
import net.schwehla.matrosdms.service.message.UpdateCategoryMessage;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/category")
public class CategoryController {

	@Autowired
	CategoryService categoryService;
	@Autowired
	CategoryImportService importService;

	// --- CONFIG ENDPOINT ---

	@GetMapping("/definitions")
	@Operation(summary = "Get metadata for all Root Categories (Scope, Label)")
	public ResponseEntity<List<RootCategoryMeta>> getDefinitions() {
		List<RootCategoryMeta> list = Arrays.stream(ERootCategory.values())
				.map(RootCategoryMeta::fromEnum)
				.collect(Collectors.toList());
		return ResponseEntity.ok(list);
	}

	// --- ROOT ACCESS ---

	@GetMapping("/root/{type}")
	@Operation(summary = "Get a system root category by type (WHO, WHAT, WHERE, KIND)")
	public ResponseEntity<MCategory> getRootCategory(
			@PathVariable("type") ERootCategory type,
			@RequestParam(name = "transitive", defaultValue = "false") boolean transitive) {

		MCategory element = categoryService.getCategory(type.getUuid(), transitive);
		return new ResponseEntity<>(element, HttpStatus.OK);
	}

	// --- IMPORT OPERATIONS ---

	@PostMapping("/root/{type}/import")
	@Operation(summary = "Import category tree into a System Root (Restricted to WHO, WHAT, WHERE, KIND)")
	public ResponseEntity<String> importRootCategories(
			@PathVariable ERootCategory type, @RequestBody CategoryImportMessage message) {

		try {
			if (message.isSimulate()) {
				// Dry-run: just validate YAML syntax
				importService.parseYaml(message.getYaml());
				return ResponseEntity.ok("Valid.");
			}
			importService.importFromText(type.getUuid(), message.getYaml(), message.isReplace());
			return ResponseEntity.ok("Imported.");
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
		}
	}

	// --- STANDARD CRUD ---

	@PostMapping("/{id}")
	@Operation(summary = "Create a new Category under parent {id}")
	public ResponseEntity<MCategory> createCategory(
			@Valid @RequestBody CreateCategoryMessage item, @PathVariable("id") String parentId) {
		MCategory info = categoryService.createCategory(item, parentId);
		return new ResponseEntity<>(info, HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get category by UUID")
	public ResponseEntity<MCategory> loadCategory(
			@PathVariable("id") String tsid,
			@RequestParam(name = "transitive", defaultValue = "false") boolean transitive) {
		MCategory element = categoryService.getCategory(tsid, transitive);
		return new ResponseEntity<>(element, HttpStatus.OK);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update category (Rename / Move)")
	public ResponseEntity<MCategory> updateCategory(
			@PathVariable("id") String id, @Valid @RequestBody UpdateCategoryMessage message) {
		MCategory updated = categoryService.updateCategory(id, message);
		return new ResponseEntity<>(updated, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete category by id")
	public ResponseEntity<HttpStatus> deleteCategory(@PathVariable("id") String id) {
		categoryService.deleteCategory(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
