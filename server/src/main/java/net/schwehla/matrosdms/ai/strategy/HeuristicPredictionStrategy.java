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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

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

		Prediction p = result.getPrediction();
		Map<String, Double> fieldConf = new LinkedHashMap<>();

		// 1. Normalize text if available
		String normalizedText = (fullText != null && !fullText.isBlank()) ? normalizeText(fullText) : "";
		String safeFilename = filename != null ? filename.toLowerCase() : "";

		// ── PHASE A: Context detection ──────────────────────────────────────────────

		// A1. Dynamic Filename History Scoring (highest priority)
		// Scores each context by comparing the incoming filename against filenames
		// that have already been filed there — no stored patterns needed.
		Candidate selectedContext = null;

		if (!safeFilename.isBlank()) {
			Candidate contextByHistory = scoreContextByFilenameHistory(safeFilename, candidates.contexts());
			if (contextByHistory != null) {
				selectedContext = contextByHistory;
				p.setContext(selectedContext.uuid());
				p.setSummary("Filename matched history for '" + selectedContext.name() + "'");
				fieldConf.put("context", 0.90);
				log.debug("Heuristic: context matched via filename history → '{}'", selectedContext.name());
			}
		}

		// A2. Text-based Aho-Corasick (score-based, picks highest-frequency match)
		if (selectedContext == null && !normalizedText.isBlank()) {
			Map<String, Candidate> contextMap = new HashMap<>();
			Trie.TrieBuilder builder = Trie.builder().ignoreCase().ignoreOverlaps().onlyWholeWords();
			for (Candidate c : candidates.contexts()) {
				String name = c.name().toLowerCase();
				if (name.length() < 3) continue; // Skip very short names (too many false positives)
				builder.addKeyword(name);
				contextMap.put(name, c);
			}
			Trie trie = builder.build();

			Collection<Emit> emits = trie.parseText(normalizedText);
			if (!emits.isEmpty()) {
				// Score by frequency: count how many times each candidate name appears
				Map<Candidate, Long> scores = new HashMap<>();
				for (Emit e : emits) {
					Candidate c = contextMap.get(e.getKeyword().toLowerCase());
					if (c != null) scores.merge(c, 1L, Long::sum);
				}
				Optional<Map.Entry<Candidate, Long>> best = scores.entrySet().stream()
						.max(Comparator.comparingLong(Map.Entry::getValue));

				if (best.isPresent()) {
					selectedContext = best.get().getKey();
					p.setContext(selectedContext.uuid());
					p.setSummary("Matched folder '" + selectedContext.name() + "' (" + best.get().getValue() + "x in text)");
					// Confidence scales with frequency: 1 hit = 0.80, 3+ hits = 0.92
					double textConf = Math.min(0.92, 0.80 + (best.get().getValue() - 1) * 0.06);
					fieldConf.put("context", textConf);
				}
			}
		}

		// ── PHASE B: Kind detection ─────────────────────────────────────────────────

		if (selectedContext != null) {
			// B1. Majority-vote : last N documents filed into this context → most common kind
			boolean kindMatched = proposeBestKindFromContext(selectedContext.uuid(), candidates.kinds(), p);
			if (kindMatched) {
				fieldConf.put("kind", 0.72);
			}
		}

		// B2. Semantic similarity: search nearest-neighbour docs by fulltext → cross-ref kind
		if (!fieldConf.containsKey("kind") && !normalizedText.isBlank()) {
			boolean semanticKind = proposeSimilarDocKind(normalizedText, candidates.kinds(), p,
					selectedContext != null ? selectedContext.uuid() : null);
			if (semanticKind) {
				fieldConf.put("kind", 0.68);
			}
		}

		// B3. Text-based kind keyword scoring (independent fallback)
		if (!fieldConf.containsKey("kind") && !normalizedText.isBlank()) {
			Candidate bestKind = scoreKindFromText(normalizedText, candidates.kinds());
			if (bestKind != null) {
				p.setKind(bestKind.uuid());
				if (p.getSummary() == null || p.getSummary().isBlank()) {
					p.setSummary("Document type '" + bestKind.name() + "' detected from content");
				}
				fieldConf.put("kind", 0.65);
			}
		}

		// ── PHASE C: Date extraction ─────────────────────────────────────────────────
		if (!normalizedText.isBlank()) {
			LocalDate date = findBestDate(normalizedText);
			p.setDocumentDate(date);
			if (date != null) {
				fieldConf.put("documentDate", 0.80);
			}
		}

		// ── PHASE D: Metadata ────────────────────────────────────────────────────────
		p.setStrategyId("heuristic");
		p.setFieldConfidences(fieldConf);
		p.setConfidence(fieldConf.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

		log.debug("Heuristic: {}ms '{}' → context={} kind={} conf={}",
				System.currentTimeMillis() - start, filename,
				p.getContext(), p.getKind(), String.format("%.2f", p.getConfidence()));
	}

	/**
	 * Dynamic filename history scorer.
	 * For each context candidate, fetches the most recently filed documents and
	 * scores the incoming filename against their actual filenames using two signals:
	 * <ol>
	 *   <li><b>Extension match</b>: ratio of historical files sharing the same
	 *       file extension as the incoming filename (weight 0.5)</li>
	 *   <li><b>Token overlap</b>: Jaccard-like overlap of filename tokens (split on
	 *       {@code [._\- 0-9]}) between the incoming name and the historical corpus
	 *       (weight 0.5)</li>
	 * </ol>
	 * Returns the best-scoring context if its combined score exceeds 0.35.
	 * Returns {@code null} if no context passes the threshold.
	 */
	private Candidate scoreContextByFilenameHistory(String incomingFilename, List<Candidate> contexts) {
		if (contexts == null || contexts.isEmpty() || incomingFilename.isBlank()) return null;

		// Extract extension and tokens from the incoming filename
		String incomingExt = extractExtension(incomingFilename);
		java.util.Set<String> incomingTokens = tokenizeFilename(incomingFilename);

		Candidate best = null;
		double bestScore = 0.35; // minimum threshold

		for (Candidate ctx : contexts) {
			try {
				SearchCriteria criteria = new SearchCriteria();
				criteria.setField(ESearchDimension.CONTEXT);
				criteria.setOperator(EOperator.EQ);
				criteria.setValue(ctx.uuid());

				Page<MSearchResult> results = searchService.search(criteria, 0, 15);
				if (!results.hasContent()) continue;

				List<String> histFilenames = results.getContent().stream()
						.map(MSearchResult::getFilename)
						.filter(f -> f != null && !f.isBlank())
						.map(String::toLowerCase)
						.collect(Collectors.toList());

				if (histFilenames.isEmpty()) continue;

				// Signal 1: extension match ratio
				double extScore = 0.0;
				if (!incomingExt.isEmpty()) {
					long extMatches = histFilenames.stream()
							.filter(f -> f.endsWith("." + incomingExt))
							.count();
					extScore = (double) extMatches / histFilenames.size();
				}

				// Signal 2: token overlap (Jaccard-like)
				java.util.Set<String> corpusTokens = new java.util.HashSet<>();
				for (String hf : histFilenames) {
					corpusTokens.addAll(tokenizeFilename(hf));
				}
				long shared = incomingTokens.stream()
						.filter(corpusTokens::contains)
						.count();
				java.util.Set<String> union = new java.util.HashSet<>(incomingTokens);
				union.addAll(corpusTokens);
				double tokenScore = union.isEmpty() ? 0.0 : (double) shared / union.size();

				double combined = (extScore * 0.5) + (tokenScore * 0.5);

				if (combined > bestScore) {
					bestScore = combined;
					best = ctx;
				}
			} catch (Exception e) {
				log.warn("scoreContextByFilenameHistory failed for context '{}': {}", ctx.name(), e.getMessage());
			}
		}

		if (best != null) {
			log.debug("Heuristic filename-history: best context='{}' score={}", best.name(),
					String.format("%.2f", bestScore));
		}
		return best;
	}

	/** Extracts the lowercase file extension, or empty string if none. */
	private String extractExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot + 1).toLowerCase() : "";
	}

	/** Splits a filename into meaningful tokens, ignoring digits and short tokens. */
	private java.util.Set<String> tokenizeFilename(String filename) {
		String base = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
		String[] parts = base.split("[._ \\-]+");
		java.util.Set<String> tokens = new java.util.HashSet<>();
		for (String p : parts) {
			String t = p.toLowerCase().replaceAll("[0-9]", "");
			if (t.length() >= 3) tokens.add(t);
		}
		return tokens;
	}

	/**
	 * Scores kind candidates by keyword frequency in the normaliszed text.
	 * Returns the best-scoring candidate, or null if no match above threshold.
	 */
	private Candidate scoreKindFromText(String normalizedText, List<Candidate> kinds) {
		if (kinds == null || kinds.isEmpty()) return null;

		Candidate best = null;
		int bestScore = 0;

		for (Candidate kind : kinds) {
			if (kind.name() == null || kind.name().length() < 3) continue;
			int score = countOccurrences(normalizedText, kind.name().toLowerCase());
			// Also check description keywords
			if (kind.description() != null && !kind.description().isBlank()) {
				for (String kw : kind.description().split("[,;\\s]+")) {
					if (kw.length() >= 4) {
						score += countOccurrences(normalizedText, kw.toLowerCase());
					}
				}
			}
			if (score > bestScore) {
				bestScore = score;
				best = kind;
			}
		}
		return bestScore >= 2 ? best : null; // Require at least 2 hits to avoid noise
	}

	private int countOccurrences(String text, String keyword) {
		int count = 0;
		int idx = 0;
		while ((idx = text.indexOf(keyword, idx)) != -1) {
			count++;
			idx += keyword.length();
		}
		return count;
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
	 * Fetches the N most recent documents stored in the given context and
	 * determines the most common KIND via majority vote.
	 *
	 * Confidence scales with the vote ratio:  4/5 same kind → 0.88, 5/5 → 0.92.
	 * Minimum required: at least 2 votes.
	 *
	 * @return true if a kind was inferred with sufficient confidence
	 */
	private boolean proposeBestKindFromContext(String contextUuid, List<Candidate> kinds, Prediction p) {
		if (kinds == null || kinds.isEmpty()) return false;
		// Build a reverse name→candidate lookup (case-insensitive)
		Map<String, Candidate> byName = kinds.stream().collect(
				Collectors.toMap(c -> c.name().toLowerCase(), c -> c, (a, b) -> a));

		try {
			SearchCriteria criteria = new SearchCriteria();
			criteria.setField(ESearchDimension.CONTEXT);
			criteria.setOperator(EOperator.EQ);
			criteria.setValue(contextUuid);

			// Fetch last 10 documents from this context
			Page<MSearchResult> results = searchService.search(criteria, 0, 10);
			if (!results.hasContent()) return false;

			// Build frequency map: kind UUID → vote count
			Map<String, Integer> votes = new LinkedHashMap<>();
			Map<String, Candidate> uuidToCandidate = new HashMap<>();
			int total = 0;

			for (MSearchResult item : results.getContent()) {
				List<String> tags = item.getTags();
				if (tags == null) continue;
				for (String tag : tags) {
					Candidate c = byName.get(tag.toLowerCase());
					if (c != null) {
						votes.merge(c.uuid(), 1, Integer::sum);
						uuidToCandidate.put(c.uuid(), c);
						total++;
						break; // One kind per document is enough
					}
				}
			}

			if (votes.isEmpty() || total < 2) return false;

			// Majority winner
			Map.Entry<String, Integer> winner = votes.entrySet().stream()
					.max(Comparator.comparingInt(Map.Entry::getValue))
					.orElse(null);
			if (winner == null) return false;

			int winnerVotes = winner.getValue();
			Candidate winnerCandidate = uuidToCandidate.get(winner.getKey());
			if (winnerCandidate == null) return false;

			p.setKind(winnerCandidate.uuid());
			double voteratio = (double) winnerVotes / results.getContent().size();
			// Scale confidence: 0% match → 0.65, 100% match → 0.92
			double confidence = 0.65 + (voteratio * 0.27);

			String summary = "Type '" + winnerCandidate.name() + "' by history (" + winnerVotes + "/"
					+ results.getContent().size() + " docs in context)";
			p.setSummary(p.getSummary() != null ? p.getSummary() + "; " + summary : summary);

			log.debug("Heuristic majority-vote: kind='{}' votes={}/{} conf={}",
					winnerCandidate.name(), winnerVotes, results.getContent().size(),
					String.format("%.2f", confidence));
			return true;

		} catch (Exception e) {
			log.warn("proposeBestKindFromContext failed: {}", e.getMessage());
		}
		return false;
	}

	/**
	 * Fulltext nearest-neighbour lookup: extracts key terms from the document text,
	 * searches the whole corpus for similar documents, and majority-votes their KIND tags.
	 *
	 * Optionally scoped to a specific context (AND filter) to reduce false positives.
	 *
	 * @return true if a kind was inferred from neighbours
	 */
	private boolean proposeSimilarDocKind(String normalizedText, List<Candidate> kinds, Prediction p,
			String limitToContextUuid) {
		if (kinds == null || kinds.isEmpty() || normalizedText.isBlank()) return false;

		Map<String, Candidate> byName = kinds.stream().collect(
				Collectors.toMap(c -> c.name().toLowerCase(), c -> c, (a, b) -> a));

		try {
			// Extract 3-5 meaningful terms (≥5 chars) from the beginning of the text for the query
			String[] tokens = normalizedText.split("\\s+");
			String queryTerms = java.util.Arrays.stream(tokens)
					.filter(t -> t.length() >= 5 && t.matches("[a-zA-ZäöüÄÖÜßA-z]+"))
					.limit(5)
					.collect(Collectors.joining(" "));

			if (queryTerms.isBlank()) return false;

			SearchCriteria searchCriteria;
			if (limitToContextUuid != null) {
				// AND(context=X, fulltext contains terms)
				SearchCriteria contextFilter = new SearchCriteria();
				contextFilter.setField(ESearchDimension.CONTEXT);
				contextFilter.setOperator(EOperator.EQ);
				contextFilter.setValue(limitToContextUuid);

				SearchCriteria textFilter = SearchCriteria.forText(queryTerms);

				SearchCriteria group = new SearchCriteria();
				group.setType(net.schwehla.matrosdms.search.SearchCriteria.Type.GROUP);
				group.setLogic(net.schwehla.matrosdms.search.SearchCriteria.Logic.AND);
				group.setChildren(java.util.List.of(contextFilter, textFilter));
				searchCriteria = group;
			} else {
				searchCriteria = SearchCriteria.forText(queryTerms);
			}

			Page<MSearchResult> results = searchService.search(searchCriteria, 0, 5);
			if (!results.hasContent()) return false;

			// Majority vote on tags across similar documents
			Map<String, Integer> votes = new LinkedHashMap<>();
			Map<String, Candidate> uuidToCandidate = new HashMap<>();

			for (MSearchResult item : results.getContent()) {
				List<String> tags = item.getTags();
				if (tags == null) continue;
				for (String tag : tags) {
					Candidate c = byName.get(tag.toLowerCase());
					if (c != null) {
						votes.merge(c.uuid(), 1, Integer::sum);
						uuidToCandidate.put(c.uuid(), c);
						break;
					}
				}
			}

			if (votes.isEmpty()) return false;

			Map.Entry<String, Integer> winner = votes.entrySet().stream()
					.max(Comparator.comparingInt(Map.Entry::getValue))
					.orElse(null);
			if (winner == null || winner.getValue() < 2) return false; // Need at least 2 agreeing neighbours

			Candidate winnerCandidate = uuidToCandidate.get(winner.getKey());
			if (winnerCandidate == null) return false;

			p.setKind(winnerCandidate.uuid());
			String summary = "Type '" + winnerCandidate.name() + "' from " + winner.getValue() + " similar documents";
			p.setSummary(p.getSummary() != null ? p.getSummary() + "; " + summary : summary);

			log.debug("Heuristic similarity: kind='{}' from {}/{} neighbours",
					winnerCandidate.name(), winner.getValue(), results.getContent().size());
			return true;

		} catch (Exception e) {
			log.warn("proposeSimilarDocKind failed: {}", e.getMessage());
		}
		return false;
	}
}