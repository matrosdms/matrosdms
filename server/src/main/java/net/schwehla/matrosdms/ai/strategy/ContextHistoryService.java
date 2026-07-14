/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.ai.strategy;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.domain.search.EOperator;
import net.schwehla.matrosdms.domain.search.ESearchDimension;
import net.schwehla.matrosdms.domain.search.MSearchResult;
import net.schwehla.matrosdms.search.SearchCriteria;
import net.schwehla.matrosdms.service.SearchService;

/**
 * Cached lookup of the filenames recently filed into a context.
 *
 * The heuristic classifier scores the incoming filename against every context's
 * history on EVERY ingested document - without this cache that is one Lucene
 * search per context per document (O(contexts) per ingest).
 */
@Service
public class ContextHistoryService {

	static final int HISTORY_SIZE = 15;

	@Autowired
	SearchService searchService;

	@Cacheable(value = "contextFilenameHistory", key = "#contextUuid")
	public List<String> getRecentFilenames(String contextUuid) {
		SearchCriteria criteria = new SearchCriteria();
		criteria.setField(ESearchDimension.CONTEXT);
		criteria.setOperator(EOperator.EQ);
		criteria.setValue(contextUuid);

		Page<MSearchResult> results = searchService.search(criteria, 0, HISTORY_SIZE);
		return results.getContent().stream()
				.map(MSearchResult::getFilename)
				.filter(f -> f != null && !f.isBlank())
				.map(String::toLowerCase)
				.collect(Collectors.toList());
	}
}
