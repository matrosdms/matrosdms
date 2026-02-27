/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.ai.strategy;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.ai.IPredictionStrategy;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates.Candidate;
import net.schwehla.matrosdms.domain.inbox.Prediction;
import net.schwehla.matrosdms.domain.search.EOperator;
import net.schwehla.matrosdms.domain.search.ESearchDimension;
import net.schwehla.matrosdms.domain.search.MSearchResult;
import net.schwehla.matrosdms.search.SearchCriteria;
import net.schwehla.matrosdms.service.SearchService;
import net.schwehla.matrosdms.service.message.DigestResultMessage;

import opennlp.tools.tokenize.SimpleTokenizer;

@Component
public class HeuristicPredictionStrategy implements IPredictionStrategy {

	private static final Logger log = LoggerFactory.getLogger(HeuristicPredictionStrategy.class);

	@Autowired
	private SearchService searchService;

	// Enhanced Date Pattern for ISO (YYYY-MM-DD) and German/English (DD.MM.YYYY /
	// DD Month YYYY)
	// Group 1-3: ISO | Group 4-6: European/Text
	private static final Pattern DATE_PATTERN = Pattern.compile(
			"\\b(?:(\\d{4})-(\\d{1,2})-(\\d{1,2}))|(?:(\\d{1,2})[\\.\\/\\-]\\s?(?:([a-zA-ZäöüÄÖÜß]{3,9})|(\\d{1,2}))[\\.\\/\\-]\\s?(\\d{2,4}))\\b",
			Pattern.CASE_INSENSITIVE);

	private static final Map<String, Integer> MONTH_MAP = new HashMap<>();
	static {
		MONTH_MAP.put("jan", 1);
		MONTH_MAP.put("januar", 1);
		MONTH_MAP.put("january", 1);
		MONTH_MAP.put("feb", 2);
		MONTH_MAP.put("februar", 2);
		MONTH_MAP.put("february", 2);
		MONTH_MAP.put("mär", 3);
		MONTH_MAP.put("märz", 3);
		MONTH_MAP.put("mar", 3);
		MONTH_MAP.put("march", 3);
		MONTH_MAP.put("apr", 4);
		MONTH_MAP.put("april", 4);
		MONTH_MAP.put("mai", 5);
		MONTH_MAP.put("may", 5);
		MONTH_MAP.put("jun", 6);
		MONTH_MAP.put("juni", 6);
		MONTH_MAP.put("june", 6);
		MONTH_MAP.put("jul", 7);
		MONTH_MAP.put("juli", 7);
		MONTH_MAP.put("july", 7);
		MONTH_MAP.put("aug", 8);
		MONTH_MAP.put("august", 8);
		MONTH_MAP.put("sep", 9);
		MONTH_MAP.put("september", 9);
		MONTH_MAP.put("okt", 10);
		MONTH_MAP.put("oktober", 10);
		MONTH_MAP.put("oct", 10);
		MONTH_MAP.put("october", 10);
		MONTH_MAP.put("nov", 11);
		MONTH_MAP.put("november", 11);
		MONTH_MAP.put("dez", 12);
		MONTH_MAP.put("dezember", 12);
		MONTH_MAP.put("dec", 12);
		MONTH_MAP.put("december", 12);
	}

	@Override
	public String getId() {
		return "heuristic";
	}

	@Override
	public void analyze(String fullText, String filename, ClassificationCandidates candidates,
			DigestResultMessage result) {
		long start = System.currentTimeMillis();
		if (fullText == null || fullText.isBlank())
			return;

		Prediction p = result.getPrediction();

		// 1. Normalize Text via OpenNLP (Resolves hidden breaks, OCR spacing issues)
		String normalizedText = normalizeText(fullText);

		// 2. Build Aho-Corasick Trie for contexts
		Map<String, Candidate> contextMap = new HashMap<>();
		Trie.TrieBuilder builder = Trie.builder().ignoreCase().ignoreOverlaps().onlyWholeWords();

		for (Candidate c : candidates.contexts()) {
			String name = c.name().toLowerCase();
			builder.addKeyword(name);
			contextMap.put(name, c);
		}
		Trie trie = builder.build();

		// 3. Scan for contexts in O(n) time
		Collection<Emit> emits = trie.parseText(normalizedText);
		if (!emits.isEmpty()) {
			// Find the first/best match
			Emit bestMatch = emits.iterator().next();
			Candidate matchedContext = contextMap.get(bestMatch.getKeyword().toLowerCase());

			if (matchedContext != null) {
				p.setContext(matchedContext.uuid());
				p.setSummary("Matched folder '" + matchedContext.name() + "'");

				// 4. Best of Breed classification for KIND based on context history
				proposeBestKindFromContext(matchedContext.uuid(), candidates.categories(), p);
			}
		}

		// 5. Date Extraction (Regex + Dictionary)
		p.setDocumentDate(findBestDate(normalizedText));

		long duration = System.currentTimeMillis() - start;
		log.debug("Heuristic analysis took {}ms for {}", duration, filename);
	}

	private String normalizeText(String rawText) {
		// Use OpenNLP SimpleTokenizer to handle punctuation and spacing
		try {
			SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
			String[] tokens = tokenizer.tokenize(rawText);
			return String.join(" ", tokens);
		} catch (Exception e) {
			// Fallback if OpenNLP fails for any reason
			return rawText.replaceAll("\\s+", " ");
		}
	}

	private LocalDate findBestDate(String text) {
		Matcher m = DATE_PATTERN.matcher(text);
		if (m.find()) {
			try {
				int year, month, day;

				// Check ISO Group (YYYY-MM-DD)
				if (m.group(1) != null) {
					year = Integer.parseInt(m.group(1));
					month = Integer.parseInt(m.group(2));
					day = Integer.parseInt(m.group(3));
				} else {
					// Check European/Text Group
					day = Integer.parseInt(m.group(4));
					String monthStr = m.group(5);
					String monthNum = m.group(6);
					year = Integer.parseInt(m.group(7));

					if (monthStr != null) {
						String key = monthStr.toLowerCase().substring(0, 3);
						if (!MONTH_MAP.containsKey(key))
							return null;
						month = MONTH_MAP.get(key);
					} else {
						month = Integer.parseInt(monthNum);
					}
				}

				if (year < 100)
					year += 2000; // Handle 2-digit years
				return LocalDate.of(year, month, day);
			} catch (Exception e) {
				// Ignore invalid dates
			}
		}
		return null;
	}

	/**
	 * Looks up the last document stored in the given context and proposes its
	 * Category (KIND).
	 * "If the last thing filed in 'Allianz' was 'Insurance Policy', this is likely
	 * one too."
	 */
	private void proposeBestKindFromContext(String contextUuid, List<Candidate> categories, Prediction p) {
		try {
			SearchCriteria criteria = new SearchCriteria();
			criteria.setField(ESearchDimension.CONTEXT);
			criteria.setOperator(EOperator.EQ);
			criteria.setValue(contextUuid);

			// Fetch only the most recent item (Limit 1)
			Page<MSearchResult> results = searchService.search(criteria, 0, 1);

			if (results.hasContent()) {
				MSearchResult lastItem = results.getContent().get(0);

				// Extract tags from the result
				List<String> tags = lastItem.getTags();
				if (tags != null && !tags.isEmpty()) {
					// Find which tag corresponds to a valid KIND category
					for (String tagName : tags) {
						for (Candidate cat : categories) {
							if (cat.name().equalsIgnoreCase(tagName)) {
								p.setKind(cat.uuid());
								p.setSummary(p.getSummary() + " (Category '" + cat.name() + "' inferred from history)");
								return;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.warn("Best-of-breed classification failed: {}", e.getMessage());
		}
	}
}