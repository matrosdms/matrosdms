/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

import jakarta.persistence.EntityManager;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.data.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.entity.DBItem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/stats")
@Tag(name = "Statistics", description = "Dashboard Aggregations")
public class StatsController {

	@Autowired
	EntityManager entityManager;

	@GetMapping("/timeline")
	@Operation(summary = "Get document counts by month (last 12 months)")
	public ResponseEntity<Map<String, Long>> getTimeline() {

		SearchSession searchSession = org.hibernate.search.mapper.orm.Search.session(entityManager);

		// Define Aggregation Key
		AggregationKey<Map<Range<LocalDate>, Long>> countByKey = AggregationKey.of("counts");

		LocalDate now = LocalDate.now();
		LocalDate oneYearAgo = now.minusMonths(12).withDayOfMonth(1);

		SearchResult<DBItem> result = searchSession
				.search(DBItem.class)
				.where(f -> f.range().field("issueDate").greaterThan(oneYearAgo))
				.aggregation(
						countByKey,
						f -> f.range()
								.field("issueDate", LocalDate.class)
								// Create monthly buckets
								.ranges(generateMonthlyRanges(oneYearAgo, now)))
				.fetch(0);

		// Convert Range keys to simple Strings ("2024-01") for Frontend
		Map<String, Long> response = new TreeMap<>(); // Sorted
		result
				.aggregation(countByKey)
				.forEach(
						(range, count) -> {
							if (range.lowerBoundValue().isPresent()) {
								String key = range.lowerBoundValue().get().toString().substring(0, 7); // YYYY-MM
								response.put(key, count);
							}
						});

		return ResponseEntity.ok(response);
	}

	@GetMapping("/tags")
	@Operation(summary = "Get top 10 tags")
	public ResponseEntity<Map<String, Long>> getTopTags() {
		SearchSession searchSession = org.hibernate.search.mapper.orm.Search.session(entityManager);
		AggregationKey<Map<String, Long>> tagsKey = AggregationKey.of("tags");

		SearchResult<DBItem> result = searchSession
				.search(DBItem.class)
				.where(f -> f.matchAll())
				.aggregation(
						tagsKey, f -> f.terms().field("kindList.raw", String.class).maxTermCount(10))
				.fetch(0);

		return ResponseEntity.ok(result.aggregation(tagsKey));
	}

	// Helper to generate ranges [Jan-Feb), [Feb-Mar) ...
	private java.util.List<Range<LocalDate>> generateMonthlyRanges(LocalDate start, LocalDate end) {
		java.util.List<Range<LocalDate>> ranges = new java.util.ArrayList<>();
		LocalDate current = start;
		while (current.isBefore(end.plusMonths(1))) {
			LocalDate next = current.plusMonths(1);
			ranges.add(Range.between(current, next));
			current = next;
		}
		return ranges;
	}
}
