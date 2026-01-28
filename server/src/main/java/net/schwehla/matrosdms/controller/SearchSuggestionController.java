/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.repository.CategoryRepository;
import net.schwehla.matrosdms.repository.ContextRepository;
import net.schwehla.matrosdms.repository.StoreRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/search/suggest")
public class SearchSuggestionController {

	@Autowired
	CategoryRepository categoryRepository;
	@Autowired
	ContextRepository contextRepository;
	@Autowired
	StoreRepository storeRepository;

	@GetMapping
	@Operation(summary = "Autocomplete/Suggestions for MQL Dimensions", description = "Returns a list of names matching the query for a specific dimension (who, where, folder,"
			+ " etc.)")
	public ResponseEntity<List<String>> getSuggestions(
			@Parameter(description = "MQL Key (e.g. 'who', 'folder', 'store')", required = true) @RequestParam("field") String field,
			@Parameter(description = "User input so far", required = true) @RequestParam("q") String query) {

		if (query == null || query.length() < 1) {
			return ResponseEntity.ok(Collections.emptyList());
		}

		PageRequest limit = PageRequest.of(0, 10);
		List<String> results = Collections.emptyList();

		switch (field.toLowerCase()) {
			case "folder":
			case "context":
			case "ref":
				results = contextRepository.suggestNames(query, limit);
				break;

			case "store":
			case "box":
				// Inline implementation for Store as it wasn't added to Repo interface in patch
				// In production, move to Repo. Here we cast/mock or use simple JPA logic if
				// repo method
				// exists.
				// Assuming StoreRepository has similar findAll or we fallback:
				results = storeRepository.findAll().stream()
						.map(s -> s.getShortname())
						.filter(n -> n.toLowerCase().contains(query.toLowerCase()))
						.limit(10)
						.toList();
				break;

			case "who":
			case "what":
			case "where":
			case "kind":
			case "tag":
			case "cat":
				// Searches ALL categories.
				// Ideally, you'd filter by Root UUID (e.g. ROOT_WHO) if the field is 'who'.
				// But global category search is often good enough for UI dropdowns.
				results = categoryRepository.suggestNames(query, limit);
				break;

			default:
				// No suggestions for fulltext or dates
				break;
		}

		return ResponseEntity.ok(results);
	}
}
