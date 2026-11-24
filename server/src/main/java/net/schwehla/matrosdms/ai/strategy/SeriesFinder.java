/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.ai.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.schwehla.matrosdms.domain.ai.ClassificationCandidates.Candidate;
import net.schwehla.matrosdms.domain.search.MSearchResult;

/**
 * The series a new document continues: documents filed under a dated title ("Bezügemitteilung
 * 2026-05", "VBL Versicherungsnachweis 2013") whose words, without the date, all appear in the new
 * document. Where the latest documents of the series went, and which type they got, is the user's
 * current way of filing it, even when older ones lie elsewhere: pay slips that were filed under the
 * bank account for a while and under the employer since.
 */
final class SeriesFinder {

	/** How many of the latest documents of a series decide, and how many of them must agree. */
	static final int LATEST = 5;
	static final double AGREEMENT = 0.6;

	/**
	 * @param name the series' title without its date ("Bezügemitteilung")
	 * @param contextUuid where most of its latest documents went
	 * @param agreeing how many of the latest went there
	 * @param considered how many of the latest were looked at
	 * @param members its documents in that context, the latest first
	 */
	record Series(String name, String contextUuid, int agreeing, int considered, List<MSearchResult> members) {

		/** 0.86 for three of the latest five, 0.90 for all five. */
		double confidence() {
			return Math.min(0.90, 0.80 + 0.02 * agreeing);
		}
	}

	private SeriesFinder() {
	}

	/**
	 * @param filed every filed document (title, context, types, issue date)
	 * @param contexts the contexts the new document may go to
	 */
	static Optional<Series> find(List<MSearchResult> filed, String text, String filename, Set<String> contexts) {
		if (filed == null || filed.isEmpty())
			return Optional.empty();
		Set<String> words = TitleProposer.tokens(text);
		words.addAll(TitleProposer.tokens(TitleProposer.withoutExtension(filename)));
		if (words.isEmpty())
			return Optional.empty();

		Map<Set<String>, List<MSearchResult>> series = new HashMap<>();
		for (MSearchResult doc : filed) {
			if (doc.getIssueDate() == null || doc.getContextUuid() == null || !contexts.contains(doc.getContextUuid()))
				continue;
			if (!TitleProposer.hasDate(doc.getName()))
				continue; // a single document ("Freistellungsauftrag"), no series
			Set<String> key = new TreeSet<>(TitleProposer.significant(TitleProposer.tokens(doc.getName())));
			if (!key.isEmpty() && words.containsAll(key))
				series.computeIfAbsent(key, k -> new ArrayList<>()).add(doc);
		}

		// the most specific series first ("VBL Versicherungsnachweis" before "Versicherungsnachweis"),
		// then the longest; one that is spread over several contexts decides nothing
		return series.entrySet().stream()
				.sorted(Comparator.<Map.Entry<Set<String>, List<MSearchResult>>>comparingInt(e -> e.getKey().size())
						.reversed()
						.thenComparing(e -> e.getValue().size(), Comparator.reverseOrder())
						.thenComparing(e -> String.join(" ", e.getKey())))
				.map(e -> decide(e.getValue()))
				.flatMap(Optional::stream)
				.findFirst();
	}

	private static Optional<Series> decide(List<MSearchResult> docs) {
		List<MSearchResult> latestFirst = docs.stream()
				.sorted(Comparator.comparing(MSearchResult::getIssueDate).reversed())
				.toList();
		List<MSearchResult> latest = latestFirst.subList(0, Math.min(LATEST, latestFirst.size()));
		Map<String, Long> votes = latest.stream()
				.collect(Collectors.groupingBy(MSearchResult::getContextUuid, LinkedHashMap::new,
						Collectors.counting()));
		// on a tie the context of the very latest document, LinkedHashMap keeps that order
		Map.Entry<String, Long> top = votes.entrySet().stream()
				.max(Comparator.comparingLong(Map.Entry::getValue))
				.orElseThrow();
		int agreeing = top.getValue().intValue();
		if (agreeing < 2 || agreeing < AGREEMENT * latest.size())
			return Optional.empty();

		List<MSearchResult> members = latestFirst.stream()
				.filter(d -> d.getContextUuid().equals(top.getKey()))
				.toList();
		return Optional.of(new Series(TitleProposer.withoutDates(members.get(0).getName()), top.getKey(), agreeing,
				latest.size(), members));
	}

	/**
	 * The type most of the series' latest documents have. A document carries its type and the type's
	 * parents ("Gehaltsbezug", "Beleg", "KIND"); the first one that is a kind counts.
	 */
	static Optional<Candidate> kind(Series series, Collection<Candidate> kinds) {
		Map<String, Candidate> byName = kinds.stream()
				.collect(Collectors.toMap(k -> k.name().toLowerCase(), Function.identity(), (a, b) -> a));
		Map<Candidate, Long> votes = series.members().stream()
				.limit(LATEST)
				.map(d -> d.getTags() == null ? null
						: d.getTags().stream().map(t -> byName.get(t.toLowerCase())).filter(c -> c != null)
								.findFirst().orElse(null))
				.filter(c -> c != null)
				.collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
		return votes.entrySet().stream()
				.max(Comparator.comparingLong(Map.Entry::getValue))
				.filter(e -> e.getValue() >= 2)
				.map(Map.Entry::getKey);
	}
}
