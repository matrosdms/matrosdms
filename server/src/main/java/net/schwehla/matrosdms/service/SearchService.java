/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.domain.search.ESearchDimension;
import net.schwehla.matrosdms.domain.search.MSearchResult;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.search.SearchCriteria;

@Service
public class SearchService {

	@Autowired
	private EntityManager entityManager;

	@Transactional
	public void indexSingleItem(Long itemId) {
		SearchSession searchSession = org.hibernate.search.mapper.orm.Search.session(entityManager);
		DBItem item = entityManager.find(DBItem.class, itemId);
		if (item != null) {
			searchSession.indexingPlan().addOrUpdate(item);
		}
	}

	@Transactional(readOnly = true)
	public Page<MSearchResult> search(SearchCriteria rootCriteria, int offset, int limit) {
		if (rootCriteria == null) {
			return new PageImpl<>(Collections.emptyList());
		}

		SearchSession searchSession = org.hibernate.search.mapper.orm.Search.session(entityManager);

		int pageNumber = offset / (limit > 0 ? limit : 20);
		Pageable pageable = PageRequest.of(pageNumber, limit);

		SearchResult<List<?>> result = searchSession
				.search(DBItem.class)
				.select(
						f -> f.composite(
								f.field("uuid", String.class),
								f.field("name", String.class),
								f.field("infoContext.name", String.class),
								f.field("store.shortname", String.class),
								f.field("kindList.name", String.class).multi(),
								f.field("issueDate", LocalDate.class),
								f.score(),
								f.highlight("fulltext")))
				.where(f -> buildPredicate(f, rootCriteria))
				.fetch(offset, limit);

		// Find max score for normalization
		float maxScore = result.hits().stream()
				.map(hit -> (float) hit.get(6))
				.max(Float::compare)
				.orElse(1f);
		if (maxScore <= 0)
			maxScore = 1f;

		final float normalizer = maxScore;

		List<MSearchResult> dtos = result.hits().stream()
				.map(
						hit -> {
							List<String> fragments = (List<String>) hit.get(7);
							String highlight = (fragments != null && !fragments.isEmpty())
									? String.join("...", fragments)
									: null;

							float rawScore = (float) hit.get(6);
							float normalizedScore = rawScore / normalizer;

							return new MSearchResult(
									(String) hit.get(0),
									(String) hit.get(1),
									(String) hit.get(2),
									(String) hit.get(3),
									(List<String>) hit.get(4),
									(LocalDate) hit.get(5),
									normalizedScore,
									highlight);
						})
				.collect(Collectors.toList());

		return new PageImpl<>(dtos, pageable, result.total().hitCount());
	}

	private PredicateFinalStep buildPredicate(SearchPredicateFactory f, SearchCriteria node) {

		if (node.getType() == SearchCriteria.Type.GROUP) {
			var bool = f.bool();

			if (node.getChildren() != null) {
				for (SearchCriteria child : node.getChildren()) {
					PredicateFinalStep childPred = buildPredicate(f, child);

					switch (node.getLogic()) {
						case AND -> bool.must(childPred);
						case OR -> bool.should(childPred);
						case NOT -> bool.mustNot(childPred);
					}
				}
			}

			return bool;
		}

		return buildLeaf(f, node);
	}

	private PredicateFinalStep buildLeaf(SearchPredicateFactory f, SearchCriteria node) {

		// Handle missing field gracefully
		if (node.getField() == null) {
			return f.matchAll();
		}

		String fieldBase = node.getField().getLuceneField();
		String val = node.getValue();

		if (val == null)
			val = "";

		return switch (node.getOperator()) {
			case EQ -> {
                // FIXED: UUID Search Implementation
                if ("uuid".equals(fieldBase)) {
                    yield f.match().field("uuid").matching(val);
                }
				if ("fulltext".equals(fieldBase) || "attr".equals(fieldBase)) {
					yield f.match().field(fieldBase).matching(val);
				}
				yield f.match().field(fieldBase + ".uuid").matching(val);
			}

			case CONTAINS -> {
				if (node.getField() == ESearchDimension.FULLTEXT) {
					yield f.simpleQueryString()
							.field("name")
							.boost(3.0f)
							.field("filename")
							.boost(3.0f)
							.field("description")
							.boost(2.0f)
							.field("uuid")
							.boost(10.0f)
							.fields("fulltext", "attr.*", "kindList.name", "infoContext.name")
							.matching(val)
							.defaultOperator(BooleanOperator.AND);
				}
				yield f.match().field(fieldBase).matching(val).fuzzy(1);
			}

			case GT -> f.range().field(fieldBase).greaterThan(val);
			case LT -> f.range().field(fieldBase).lessThan(val);
			case GTE -> f.range().field(fieldBase).atLeast(val);
			case LTE -> f.range().field(fieldBase).atMost(val);

			default -> f.matchAll();
		};
	}

	@Transactional
	public void reindexAll() {
		try {
			org.hibernate.search.mapper.orm.Search.session(entityManager).massIndexer().startAndWait();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}