package net.schwehla.matrosdms.ai.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import net.schwehla.matrosdms.domain.ai.ClassificationCandidates;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates.Candidate;
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
}
