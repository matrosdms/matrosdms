package net.schwehla.matrosdms.ai.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import net.schwehla.matrosdms.domain.ai.ClassificationCandidates;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates.Candidate;
import net.schwehla.matrosdms.domain.search.MSearchResult;
import net.schwehla.matrosdms.service.SearchService;
import net.schwehla.matrosdms.service.message.DigestResultMessage;

/**
 * Unit tests for the rule-based classifier. Empty candidate lists keep the
 * SearchService/ContextHistoryService branches from being touched, so date
 * extraction can be tested with a bare instance.
 */
class HeuristicPredictionStrategyTest {

	private HeuristicPredictionStrategy strategy;

	@BeforeEach
	void setUp() {
		strategy = new HeuristicPredictionStrategy();
	}

	private LocalDate extractDate(String text) {
		DigestResultMessage m = new DigestResultMessage();
		strategy.analyze(text, "", new ClassificationCandidates(List.of(), List.of()), m);
		return m.getPrediction().getDocumentDate();
	}

	@Test
	void extractsIsoDate() {
		// Regression: this returned null before PHASE C was switched to raw text
		assertThat(extractDate("Vertrag beginnt am 2024-03-05 mit dem Kunden"))
				.isEqualTo(LocalDate.of(2024, 3, 5));
	}

	@Test
	void extractsGermanNumericDate() {
		assertThat(extractDate("Rechnung vom 05.03.2024 anbei"))
				.isEqualTo(LocalDate.of(2024, 3, 5));
	}

	@Test
	void extractsGermanTextMonthDate() {
		assertThat(extractDate("Rechnung vom 5. März 2024 anbei"))
				.isEqualTo(LocalDate.of(2024, 3, 5));
	}

	@Test
	void expandsTwoDigitYearToCurrentCentury() {
		assertThat(extractDate("Mahnung vom 05.03.24 anbei"))
				.isEqualTo(LocalDate.of(2024, 3, 5));
	}

	@Test
	void returnsNullWhenNoDatePresent() {
		assertThat(extractDate("kein datum hier vorhanden")).isNull();
	}

	@Test
	void returnsNullForInvalidCalendarDate() {
		// 45th of month 99 must not throw, just yield no date
		assertThat(extractDate("something 2024-99-45 here")).isNull();
	}

	@Test
	void ahoCorasickPicksMostFrequentContext() {
		Candidate insurance = new Candidate("ctx-ins", "Insurance", "");
		Candidate taxes = new Candidate("ctx-tax", "Taxes", "");
		String text = "This insurance policy. Insurance renewal. Your insurance claim. Taxes note.";

		DigestResultMessage m = new DigestResultMessage();
		strategy.analyze(text, "", new ClassificationCandidates(List.of(insurance, taxes), List.of()), m);

		// "insurance" appears 3x, "taxes" 1x -> insurance wins
		assertThat(m.getPrediction().getContext()).isEqualTo("ctx-ins");
	}

	@Test
	void doesNotCrashOnNullText() {
		DigestResultMessage m = new DigestResultMessage();
		strategy.analyze(null, "file.pdf", new ClassificationCandidates(List.of(), List.of()), m);
		assertThat(m.getPrediction().getDocumentDate()).isNull();
	}

	@Test
	void theDayTheFileWasReadIsNotTheDocumentsDate() {
		DigestResultMessage m = new DigestResultMessage();
		strategy.analyze(IssueDateFinderTest.DIN_LETTER, "12_30_2016_02_22_VBL_2014.pdf",
				new ClassificationCandidates(List.of(), List.of()), m);

		assertThat(m.getPrediction().getDocumentDate()).isEqualTo(LocalDate.of(2015, 8, 1));
		assertThat(m.getPrediction().getFieldConfidences()).containsEntry("documentDate", 0.70);
	}

	@Test
	void withoutAContextTheTitleIsTheLettersSubjectLine() {
		DigestResultMessage m = new DigestResultMessage();
		strategy.analyze(IssueDateFinderTest.DIN_LETTER, "scan.pdf", new ClassificationCandidates(List.of(), List.of()),
				m);

		assertThat(m.getPrediction().getTitle()).isEqualTo("Versicherungsnachweis VBLklassik 2014");
		assertThat(m.getPrediction().getFieldConfidences()).containsEntry("title", 0.65);
	}

	@Test
	void theTitleFollowsTheNamingOfTheContext() {
		SearchService searchService = Mockito.mock(SearchService.class);
		ContextHistoryService history = Mockito.mock(ContextHistoryService.class);
		ReflectionTestUtils.setField(strategy, "searchService", searchService);
		ReflectionTestUtils.setField(strategy, "contextHistoryService", history);
		List<MSearchResult> filed = List.of(TitleProposerTest.filed("VBL Versicherungsnachweis 2013",
				LocalDate.of(2014, 8, 1), "12_30_2015_03_01_VBL_2013.pdf"));
		when(history.getRecentFilenames(anyString())).thenReturn(List.of());
		when(searchService.search(any(), anyInt(), anyInt())).thenReturn(new PageImpl<>(filed));

		DigestResultMessage m = new DigestResultMessage();
		strategy.analyze(IssueDateFinderTest.DIN_LETTER, "12_30_2016_02_22_VBL_2014.pdf",
				new ClassificationCandidates(List.of(new Candidate("ctx-vbl", "VBL", "")), List.of()), m);

		assertThat(m.getPrediction().getContext()).isEqualTo("ctx-vbl");
		assertThat(m.getPrediction().getTitle()).isEqualTo("VBL Versicherungsnachweis 2014");
	}

	@Test
	void aPaySlipGoesWhereTheLatestPaySlipsWentWithTheirTypeAndTheNewMonth() {
		SearchService searchService = Mockito.mock(SearchService.class);
		ContextHistoryService history = Mockito.mock(ContextHistoryService.class);
		ReflectionTestUtils.setField(strategy, "searchService", searchService);
		ReflectionTestUtils.setField(strategy, "contextHistoryService", history);
		when(history.getFiledTitles()).thenReturn(SeriesFinderTest.archive());
		// the bank's file names share words with the scanner's name: what used to decide the context
		when(history.getRecentFilenames(SeriesFinderTest.BANK))
				.thenReturn(List.of("2023-06-16_grundentgelt tarif bamf-zulage nicht auszahlungsrelevant.pdf"));
		when(history.getRecentFilenames(SeriesFinderTest.EMPLOYER)).thenReturn(List.of("01032026.pdf"));
		Candidate bank = new Candidate(SeriesFinderTest.BANK, "Sparkasse Ansbach", "");
		Candidate employer = new Candidate(SeriesFinderTest.EMPLOYER, "Gehaltsbezug BAMF", "");
		Candidate info = new Candidate("k-info", "Info", "");
		Candidate receipt = new Candidate("k-beleg", "Beleg", "");
		Candidate paySlip = new Candidate("k-gehalt", "Gehaltsbezug", "");

		DigestResultMessage m = new DigestResultMessage();
		strategy.analyze(SeriesFinderTest.PAY_SLIP_TEXT,
				"2026-11-17_Lohnart Grundentgelt Tarif BAMF-Zulage Nicht auszahlungsrelevant.pdf",
				new ClassificationCandidates(List.of(bank, employer), List.of(info, receipt, paySlip)), m);

		assertThat(m.getPrediction().getContext()).isEqualTo(SeriesFinderTest.EMPLOYER);
		assertThat(m.getPrediction().getKind()).isEqualTo("k-gehalt");
		assertThat(m.getPrediction().getDocumentDate()).isEqualTo(LocalDate.of(2026, 11, 17));
		assertThat(m.getPrediction().getTitle()).isEqualTo("Bezügemitteilung 2026-11");
		assertThat(m.getPrediction().getSummary())
				.startsWith("Series 'Bezügemitteilung': 5 of the latest 5 in 'Gehaltsbezug BAMF'")
				.contains("Type 'Gehaltsbezug' like the series");
	}

	@Test
	void theSentDateAndSubjectOfAnEmailAreKept() {
		DigestResultMessage m = new DigestResultMessage();
		m.getPrediction().setDocumentDate(LocalDate.of(2024, 3, 5));
		m.getPrediction().setTitle("Termin Zahnarzt");

		strategy.analyze("Am 01.01.2020 schrieb Max:\nSehr geehrter Herr Muster,", "mail.eml",
				new ClassificationCandidates(List.of(), List.of()), m);

		assertThat(m.getPrediction().getDocumentDate()).isEqualTo(LocalDate.of(2024, 3, 5));
		assertThat(m.getPrediction().getTitle()).isEqualTo("Termin Zahnarzt");
		assertThat(m.getPrediction().getFieldConfidences()).containsEntry("documentDate", 0.95)
				.containsEntry("title", 0.90);
	}
}
