/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.search.MSearchResult;
import net.schwehla.matrosdms.search.SearchCriteria;
import net.schwehla.matrosdms.service.SearchService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class SearchController {

	@Autowired
	SearchService searchService;

	@GetMapping("/search")
	@Operation(summary = "Simple Fulltext Search (Paged)")
	public ResponseEntity<Page<MSearchResult>> searchSimple(
			@RequestParam("q") String query,
			@RequestParam(name = "offset", defaultValue = "0") int offset,
			@RequestParam(name = "limit", defaultValue = "20") int limit) {

		SearchCriteria criteria = SearchCriteria.forText(query);
		return ResponseEntity.ok(searchService.search(criteria, offset, limit));
	}

	@PostMapping("/search")
	@Operation(summary = "Execute Structured Search (Paged)")
	public ResponseEntity<Page<MSearchResult>> searchStructured(
			@RequestBody SearchCriteria query,
			@RequestParam(name = "offset", defaultValue = "0") int offset,
			@RequestParam(name = "limit", defaultValue = "20") int limit) {

		return ResponseEntity.ok(searchService.search(query, offset, limit));
	}
}
