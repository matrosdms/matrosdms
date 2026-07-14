/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.core.MItem;
import net.schwehla.matrosdms.service.SemanticSearchService;
import net.schwehla.matrosdms.service.SemanticSearchService.ScoredUuid;
import net.schwehla.matrosdms.service.domain.ItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/search")
@Tag(name = "Semantic Search", description = "Vector similarity search over documents (requires embeddings enabled)")
public class SemanticSearchController {

	private static final Logger log = LoggerFactory.getLogger(SemanticSearchController.class);

	@Autowired
	SemanticSearchService semanticSearchService;

	@Autowired
	ItemService itemService;

	@GetMapping("/semantic")
	@Operation(summary = "Find documents semantically similar to the query text")
	public ResponseEntity<SemanticSearchResponse> semanticSearch(
			@RequestParam("q") String query,
			@RequestParam(name = "topK", defaultValue = "10") int topK) {

		if (!semanticSearchService.isEnabled()) {
			return ResponseEntity.ok(new SemanticSearchResponse(false, List.of()));
		}

		List<ScoredUuid> scored = semanticSearchService.search(query, topK);
		List<SemanticHit> hits = new ArrayList<>();
		for (ScoredUuid s : scored) {
			try {
				MItem item = itemService.loadItem(s.uuid());
				hits.add(new SemanticHit(item, s.score()));
			} catch (Exception e) {
				// Embedding row can outlive its item during a delete race - skip
				log.debug("Semantic hit {} could not be loaded, skipping", s.uuid());
			}
		}
		return ResponseEntity.ok(new SemanticSearchResponse(true, hits));
	}

	@Schema(description = "One semantic search hit: the item plus its cosine similarity score")
	public record SemanticHit(MItem item, double score) {
	}

	@Schema(description = "Semantic search response. 'enabled' is false when embeddings are not configured.")
	public record SemanticSearchResponse(boolean enabled, List<SemanticHit> hits) {
	}
}
