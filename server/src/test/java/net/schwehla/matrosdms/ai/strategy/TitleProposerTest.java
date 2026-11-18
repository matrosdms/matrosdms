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
import java.util.List;

import org.junit.jupiter.api.Test;

import net.schwehla.matrosdms.domain.search.MSearchResult;

class TitleProposerTest {

	static MSearchResult filed(String title, LocalDate issueDate, String filename) {
		return new MSearchResult("uuid-" + title.hashCode(), title, null, "Rente", "ctx", null, null, null, List.of(),
				issueDate, "ACTIVE", filename, 1f, null);
	}

	private static final MSearchResult VBL_2013 = filed("VBL Versicherungsnachweis 2013", LocalDate.of(2014, 8, 1),
			"12_30_2015_03_01_VBL_2013.pdf");
	private static final MSearchResult DRV_2014 = filed("Deutsche Rentenversicherung Renteninformation 2014",
			LocalDate.of(2014, 10, 1), "drv_2014.pdf");

	@Test
	void theSubjectOfAnEmailWithoutReplyMarks() {
		assertThat(TitleProposer.fromEmailSubject("AW: WG: Re: Termin Zahnarzt")).isEqualTo("Termin Zahnarzt");
		assertThat(TitleProposer.fromEmailSubject("Re[2]: Angebot")).isEqualTo("Angebot");
		assertThat(TitleProposer.fromEmailSubject("No Subject")).isNull();
		assertThat(TitleProposer.fromEmailSubject("  ")).isNull();
	}

	@Test
	void theSubjectLineOfADinLetterIsTheLineBeforeTheSalutation() {
		TitleProposer.Proposal p = TitleProposer.fromSubjectLine(IssueDateFinderTest.DIN_LETTER).orElseThrow();

		assertThat(p.title()).isEqualTo("Versicherungsnachweis VBLklassik 2014");
		assertThat(p.confidence()).isEqualTo(0.65);
	}

	@Test
	void aBetreffLabelIsDropped() {
		assertThat(TitleProposer.fromSubjectLine("Betreff: Kündigung Ihres Vertrags\nSehr geehrte Damen und Herren,")
				.orElseThrow().title()).isEqualTo("Kündigung Ihres Vertrags");
	}

	@Test
	void dateLinesAddressesAndTextWithoutSalutationGiveNoSubject() {
		assertThat(TitleProposer.fromSubjectLine("Karlsruhe, 22.02.2016\nSehr geehrte Frau Muster,")).isEmpty();
		assertThat(TitleProposer.fromSubjectLine("Karlsruhe August 2015\nSehr geehrte Frau Muster,")).isEmpty();
		assertThat(TitleProposer.fromSubjectLine("Frankfurt am Main, den 3. März 2020\nGuten Tag,")).isEmpty();
		assertThat(TitleProposer.fromSubjectLine("Max Muster\nMusterstr. 1\n12345 Musterstadt\nSehr geehrter Herr,"))
				.isEmpty();
		assertThat(TitleProposer.fromSubjectLine("Ein Text ohne Anrede.")).isEmpty();
		// page frame and information block
		assertThat(TitleProposer.fromSubjectLine("Seite 1 von 2\nSehr geehrte Damen und Herren,")).isEmpty();
		assertThat(TitleProposer.fromSubjectLine("Anlage(n): Digitaler Eingang\nSehr geehrter Herr,")).isEmpty();
		assertThat(TitleProposer.fromSubjectLine("Kundennummer 4711 0815\nSehr geehrter Herr,")).isEmpty();
	}

	@Test
	void theTitleOfTheSameSeriesMovesOnByTheYearsBetweenTheDocuments() {
		TitleProposer.Proposal p = TitleProposer.fromSimilarDocument(List.of(DRV_2014, VBL_2013),
				IssueDateFinderTest.DIN_LETTER, "12_30_2016_02_22_VBL_2014.pdf", LocalDate.of(2015, 8, 1))
				.orElseThrow();

		// the 2013 statement came in 2014, so the one from 2015 is the 2014 statement
		assertThat(p.title()).isEqualTo("VBL Versicherungsnachweis 2014");
		assertThat(p.confidence()).isEqualTo(0.70);
	}

	@Test
	void withoutTheNewDocumentsDateTheYearCannotMoveOn() {
		TitleProposer.Proposal p = TitleProposer.fromSimilarDocument(List.of(VBL_2013),
				IssueDateFinderTest.DIN_LETTER, "scan.pdf", null).orElseThrow();

		assertThat(p.title()).isEqualTo("VBL Versicherungsnachweis 2013");
		assertThat(p.confidence()).isEqualTo(0.35);
	}

	@Test
	void documentsTheNewOneDoesNotResembleGiveNoTitle() {
		assertThat(TitleProposer.fromSimilarDocument(List.of(DRV_2014), IssueDateFinderTest.DIN_LETTER, "scan.pdf",
				LocalDate.of(2015, 8, 1))).isEmpty();
		assertThat(TitleProposer.fromSimilarDocument(List.of(), "text", "scan.pdf", null)).isEmpty();
	}

	@Test
	void scannerAndPhoneFileNamesAreRecognised() {
		assertThat(TitleProposer.looksGenerated("12_30_2016_02_22_VBL_2014.__2014_rente_vbl2014_ov_43.pdf")).isTrue();
		assertThat(TitleProposer.looksGenerated("Scan0001.pdf")).isTrue();
		assertThat(TitleProposer.looksGenerated("IMG_20240305_123456.jpg")).isTrue();
		assertThat(TitleProposer.looksGenerated("Rechnung Stadtwerke März.pdf")).isFalse();
	}

	@Test
	void theLastResortIsTypeContextAndYear() {
		assertThat(TitleProposer.composed("Schreiben", "Rente", LocalDate.of(2015, 8, 1)).orElseThrow().title())
				.isEqualTo("Schreiben Rente 2015");
		assertThat(TitleProposer.composed(null, null, LocalDate.of(2015, 8, 1))).isEmpty();
	}

	@Test
	void everyYearInATitleMovesOn() {
		assertThat(TitleProposer.shiftYears("Steuer 2013/2014", 2)).isEqualTo("Steuer 2015/2016");
		assertThat(TitleProposer.shiftYears("Police 4711", 2)).isEqualTo("Police 4711");
	}
}
