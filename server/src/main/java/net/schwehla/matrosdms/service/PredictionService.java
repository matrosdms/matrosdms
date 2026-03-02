/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.ai.IPredictionStrategy;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig.StrategyConfig;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates.Candidate;
import net.schwehla.matrosdms.domain.core.EArchiveFilter;
import net.schwehla.matrosdms.repository.CategoryRepository;
import net.schwehla.matrosdms.service.domain.AttributeLookupService;
import net.schwehla.matrosdms.service.domain.ContextService;
import net.schwehla.matrosdms.service.message.DigestResultMessage;
import net.schwehla.matrosdms.service.CategoryLookupService;

@Service
public class PredictionService {

	private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

	@Autowired
	List<IPredictionStrategy> strategies;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	ContextService contextService; // <--- Changed from Repository to Service (Cached)

	@Autowired
	CategoryLookupService categoryLookupService;

	@Autowired
	AttributeLookupService attributeLookupService;

	@Autowired
	AppServerSpringConfig appConfig;

	@Transactional(readOnly = true)
	public void predictMetadata(String fullText, String filename, DigestResultMessage result) {

		// 1. Select Strategy
		IPredictionStrategy strategy = strategies.stream()
				.filter(this::isEnabled)
				.min(Comparator.comparingInt(this::getPreference))
				.orElseThrow(() -> new RuntimeException("No active AI Strategy found."));

		log.debug("AI: Selected Strategy '{}'", strategy.getId());

		// 2. Get Candidates using EXISTING CACHE via Service layer
		// ContextService.loadContextList is @Cacheable("contextList")
		List<Candidate> contexts = contextService.loadContextList(EArchiveFilter.ACTIVE_ONLY, 2000, "name")
				.stream()
				.map(c -> new Candidate(c.getUuid(), c.getName(), c.getDescription()))
				.collect(Collectors.toList());

		// Kinds: only entries under ROOT_KIND (document type classification)
		List<Candidate> kinds = categoryRepository.findAll().stream()
				.filter(c -> "ROOT_KIND".equals(categoryLookupService.getRootFor(c.getUuid())))
				.map(c -> new Candidate(c.getUuid(), c.getName(), c.getDescription()))
				.collect(Collectors.toList());

		// 3. Analyze
		ClassificationCandidates candidates = new ClassificationCandidates(contexts, kinds);
		strategy.analyze(fullText, filename, candidates, result);
	}

	public String resolveAttributeUuid(String attributeName) {
		return attributeLookupService.getUuid(attributeName);
	}

	private boolean isEnabled(IPredictionStrategy s) {
		return getConfig(s.getId()).isEnabled();
	}

	private int getPreference(IPredictionStrategy s) {
		return getConfig(s.getId()).getPreference();
	}

	private StrategyConfig getConfig(String id) {
		if ("ollama".equalsIgnoreCase(id))
			return appConfig.getAi().getClassification().getOllama();
		if ("heuristic".equalsIgnoreCase(id))
			return appConfig.getAi().getClassification().getHeuristic();
		StrategyConfig fallback = new StrategyConfig();
		fallback.setEnabled(false);
		return fallback;
	}
}