package net.schwehla.matrosdms.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReportServiceTest {

	private final ReportService svc = new ReportService();

	@Test
	void prefixesFormulaInjectionTriggers() {
		assertThat(svc.escape("=cmd()")).isEqualTo("'=cmd()");
		assertThat(svc.escape("+1")).isEqualTo("'+1");
		assertThat(svc.escape("-1")).isEqualTo("'-1");
		assertThat(svc.escape("@ref")).isEqualTo("'@ref");
	}

	@Test
	void doesNotPrefixNumericValues() {
		// A real negative number must stay sortable, not become text
		assertThat(svc.escape(-5.0)).isEqualTo("-5.0");
		assertThat(svc.escape(42)).isEqualTo("42");
	}

	@Test
	void quotesAndDoublesInnerQuotes() {
		assertThat(svc.escape("a;b")).isEqualTo("\"a;b\"");
		assertThat(svc.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
		assertThat(svc.escape("line1\nline2")).isEqualTo("\"line1\nline2\"");
	}

	@Test
	void nullBecomesEmpty() {
		assertThat(svc.escape(null)).isEmpty();
	}

	@Test
	void plainTextPassesThrough() {
		assertThat(svc.escape("Invoice 2024")).isEqualTo("Invoice 2024");
	}
}
