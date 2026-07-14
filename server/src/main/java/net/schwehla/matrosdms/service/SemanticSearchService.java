/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.entity.DBItemEmbedding;
import net.schwehla.matrosdms.repository.ItemEmbeddingRepository;
import net.schwehla.matrosdms.util.VectorMath;

/**
 * Vector semantic search over items.
 *
 * Embeddings are generated at ingest (when {@code app.ai.embedding.enabled}
 * and an embedding model are configured) and ranked here by cosine similarity.
 *
 * NOTE: ranking is a brute-force scan of all stored vectors. That is entirely
 * adequate for a personal DMS (thousands of documents); a corpus in the
 * hundreds of thousands would want an ANN index instead.
 */
@Service
public class SemanticSearchService {

	private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

	@Autowired
	EmbeddingService embeddingService;

	@Autowired
	ItemEmbeddingRepository embeddingRepository;

	@Autowired
	AppServerSpringConfig appConfig;

	public boolean isEnabled() {
		return appConfig.getAi().getEmbedding().isEnabled();
	}

	/** Generates and stores (or replaces) the embedding for one item. */
	@Transactional
	public void indexItem(String itemUuid, String text) {
		if (!isEnabled() || text == null || text.isBlank()) {
			return;
		}
		float[] vector = embeddingService.generateEmbedding(text);
		if (vector == null || vector.length == 0) {
			log.debug("No embedding produced for item {} (model unavailable?)", itemUuid);
			return;
		}
		DBItemEmbedding entity = embeddingRepository.findByItemUuid(itemUuid).orElseGet(DBItemEmbedding::new);
		entity.setItemUuid(itemUuid);
		entity.setModel(appConfig.getAi().getEmbedding().getModel());
		entity.setDimension(vector.length);
		entity.setVector(VectorMath.toBytes(vector));
		entity.setDateCreated(LocalDateTime.now());
		embeddingRepository.save(entity);
		log.debug("Stored {}-dim embedding for item {}", vector.length, itemUuid);
	}

	@Transactional
	public void removeItem(String itemUuid) {
		embeddingRepository.deleteByItemUuid(itemUuid);
	}

	/** Returns item UUIDs ranked by cosine similarity to the query text. */
	@Transactional(readOnly = true)
	public List<ScoredUuid> search(String queryText, int topK) {
		if (!isEnabled() || queryText == null || queryText.isBlank()) {
			return List.of();
		}
		float[] query = embeddingService.generateEmbedding(queryText);
		if (query == null || query.length == 0) {
			return List.of();
		}
		return embeddingRepository.findAll().stream()
				// A model change leaves old vectors of a different length - skip them
				.filter(e -> e.getDimension() == query.length)
				.map(e -> new ScoredUuid(e.getItemUuid(), VectorMath.cosine(query, VectorMath.fromBytes(e.getVector()))))
				.sorted(Comparator.comparingDouble(ScoredUuid::score).reversed())
				.limit(Math.max(1, topK))
				.toList();
	}

	public record ScoredUuid(String uuid, double score) {
	}
}
