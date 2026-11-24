/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.ai.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.schwehla.matrosdms.domain.ai.ClassificationCandidates.Candidate;
import net.schwehla.matrosdms.domain.search.MSearchResult;

class SeriesFinderTest {

	static final String BANK = "ctx-bank";
	static final String EMPLOYER = "ctx-employer";
	static final Set<String> CONTEXTS = Set.of(BANK, EMPLOYER, "ctx-other");

	static final List<String> PAY_SLIP_TYPE = List.of("Gehaltsbezug", "Beleg", "KIND");

	static final String PAY_SLIP_TEXT = """
			BA für Migration & Flüchtlinge Seite: 112 Datum: 17.11.2026
			Bezügemitteilung November 2026 Persönliche Daten
			Art der Auszahlung: Überweisung Sparkasse Ansbach
			für den Zeitraum 01.11. bis 30.11.2026
			Grundentgelt Tarif BAMF-Zulage Jahressonderzahlung
			""";

	static MSearchResult filed(String title, String context, LocalDate issueDate, List<String> types) {
		return new MSearchResult("uuid-" + title + context, title, null, context, context, null, null, null, types,
				issueDate, "ACTIVE", "scan.pdf", 1f, null);
	}

	/**
	 * Pay slips under the bank account until March 2024, under the employer since; one of them with a
	 * slip in its title, and the bank's own documents.
	 */
	static List<MSearchResult> archive() {
		List<MSearchResult> filed = new ArrayList<>();
		for (int month = 1; month <= 12; month++)
			filed.add(filed("Bezügemitteilung 2023-%02d".formatted(month), BANK, LocalDate.of(2023, month, 15),
					PAY_SLIP_TYPE));
		filed.add(filed("Bezügemitteilung 2024-03", BANK, LocalDate.of(2024, 3, 14), PAY_SLIP_TYPE));
		for (int month = 4; month <= 12; month++)
			filed.add(filed("Bezügemitteilung 2025-%02d".formatted(month), EMPLOYER, LocalDate.of(2025, month, 14),
					PAY_SLIP_TYPE));
		filed.add(filed("Bezügemitteilung 2026-04", EMPLOYER, LocalDate.of(2026, 4, 16), PAY_SLIP_TYPE));
		filed.add(filed("Bezügemitteilung 2026-05", EMPLOYER, LocalDate.of(2026, 5, 14), PAY_SLIP_TYPE));
		filed.add(filed("Kontoauszug Darlehen 2020", BANK, LocalDate.of(2020, 2, 10), List.of("Info", "KIND")));
		filed.add(filed("Freistellungsauftrag", BANK, LocalDate.of(2016, 7, 9), List.of("Info", "KIND")));
		return filed;
	}

	@Test
	void theLatestDocumentsOfTheSeriesDecideWhereTheNextOneGoes() {
		SeriesFinder.Series s = SeriesFinder.find(archive(), PAY_SLIP_TEXT, "2026-11-17_Lohnart Grundentgelt.pdf",
				CONTEXTS).orElseThrow();

		// more pay slips lie under the bank than its name appears in the text, but the latest five
		// went to the employer
		assertThat(s.contextUuid()).isEqualTo(EMPLOYER);
		assertThat(s.name()).isEqualTo("Bezügemitteilung");
		assertThat(s.agreeing()).isEqualTo(5);
		assertThat(s.considered()).isEqualTo(5);
		assertThat(s.confidence()).isEqualTo(0.90);
		assertThat(s.members()).first().extracting(MSearchResult::getName).isEqualTo("Bezügemitteilung 2026-05");
		assertThat(s.members()).allMatch(d -> d.getContextUuid().equals(EMPLOYER));
	}

	@Test
	void theTypeIsTheOneOfTheSeriesNotItsParent() {
		Candidate info = new Candidate("k-info", "Info", "");
		Candidate receipt = new Candidate("k-beleg", "Beleg", "");
		Candidate paySlip = new Candidate("k-gehalt", "Gehaltsbezug", "");
		SeriesFinder.Series s = SeriesFinder.find(archive(), PAY_SLIP_TEXT, "scan.pdf", CONTEXTS).orElseThrow();

		assertThat(SeriesFinder.kind(s, List.of(info, receipt, paySlip))).contains(paySlip);
	}

	@Test
	void aSeriesSpreadOverSeveralContextsDecidesNothing() {
		List<MSearchResult> invoices = List.of(
				filed("Rechnung 2026-05", "ctx-a", LocalDate.of(2026, 5, 2), List.of()),
				filed("Rechnung 2026-04", "ctx-b", LocalDate.of(2026, 4, 2), List.of()),
				filed("Rechnung 2026-03", "ctx-c", LocalDate.of(2026, 3, 2), List.of()),
				filed("Rechnung 2026-02", "ctx-a", LocalDate.of(2026, 2, 2), List.of()),
				filed("Rechnung 2026-01", "ctx-b", LocalDate.of(2026, 1, 2), List.of()));

		assertThat(SeriesFinder.find(invoices, "Ihre Rechnung Nr. 4711", "scan.pdf",
				Set.of("ctx-a", "ctx-b", "ctx-c"))).isEmpty();
	}

	@Test
	void theMoreSpecificSeriesWins() {
		List<MSearchResult> filed = List.of(
				filed("Versicherungsnachweis 2024", "ctx-misc", LocalDate.of(2025, 2, 1), List.of()),
				filed("Versicherungsnachweis 2023", "ctx-misc", LocalDate.of(2024, 2, 1), List.of()),
				filed("VBL Versicherungsnachweis 2013", "ctx-vbl", LocalDate.of(2014, 8, 1), List.of()),
				filed("VBL Versicherungsnachweis 2012", "ctx-vbl", LocalDate.of(2013, 8, 1), List.of()));

		assertThat(SeriesFinder.find(filed, "Ihr VBL Versicherungsnachweis 2014", "scan.pdf",
				Set.of("ctx-misc", "ctx-vbl")).orElseThrow().contextUuid()).isEqualTo("ctx-vbl");
	}

	@Test
	void noSeriesWithoutItsWordsADateTwoMembersOrAnOpenContext() {
		// the title's words are not in the document
		assertThat(SeriesFinder.find(archive(), "Ihre Stromrechnung", "scan.pdf", CONTEXTS)).isEmpty();
		// undated titles are single documents
		List<MSearchResult> undated = List.of(
				filed("Freistellungsauftrag", BANK, LocalDate.of(2016, 7, 9), List.of()),
				filed("Freistellungsauftrag", BANK, LocalDate.of(2019, 7, 9), List.of()));
		assertThat(SeriesFinder.find(undated, "Freistellungsauftrag", "scan.pdf", CONTEXTS)).isEmpty();
		// one document is no series
		assertThat(SeriesFinder.find(List.of(filed("Bezügemitteilung 2026-05", EMPLOYER, LocalDate.of(2026, 5, 14),
				PAY_SLIP_TYPE)), PAY_SLIP_TEXT, "scan.pdf", CONTEXTS)).isEmpty();
		// the employer's context is closed: the bank's slips decide
		assertThat(SeriesFinder.find(archive(), PAY_SLIP_TEXT, "scan.pdf", Set.of(BANK)).orElseThrow().contextUuid())
				.isEqualTo(BANK);
	}
}
