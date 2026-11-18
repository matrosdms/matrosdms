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

import org.junit.jupiter.api.Test;

class IssueDateFinderTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

	/** A DIN letter as the text layer holds it, envelope included; the person is made up. */
	static final String DIN_LETTER = """
			<root source="FILE">
			  <meta>
			    <filename>12_30_2016_02_22_VBL_2014.pdf</filename>
			    <processed_date>2026-11-18</processed_date>
			  </meta>
			  <content type="application/pdf">
			    <![CDATA[
			VBL. 76240 Karlsruhe Versicherungsnummer 1234567890
			Service-Telefon 0721 00 00 00
			Herrn
			Max Muster
			Karlsruhe August 2015
			Versicherungsnachweis VBLklassik 2014.
			Sehr geehrter Herr Muster,
			heute erhalten Sie Ihren aktuellen Versicherungsnachweis VBLklassik.
			Stand Ihres Versorgungskontos VBLklassik zum 31. Dezember 2014
			JM 150389 01.01.-31.12.2014 12 01 10
			Summe der Versorgungspunkte zum 31.12.2014
			Altersteilzeit, die vor dem 01.01.2003 vereinbart wurde
			    ]]>
			  </content>
			</root>
			""";

	private static LocalDate find(String text) {
		return IssueDateFinder.find(text, TODAY).map(IssueDateFinder.Found::date).orElse(null);
	}

	@Test
	void theDateLineOfALetterWinsOverTheDatesItTalksAbout() {
		IssueDateFinder.Found found = IssueDateFinder.find(DIN_LETTER, TODAY).orElseThrow();

		// "August 2015" has no day: the first of the month, a little less sure
		assertThat(found.date()).isEqualTo(LocalDate.of(2015, 8, 1));
		assertThat(found.confidence()).isEqualTo(0.70);
		assertThat(found.evidence()).contains("Karlsruhe August 2015");
	}

	@Test
	void theDayTheFileWasReadIsNotTheDateOfTheDocument() {
		String layer = """
				<root source="FILE">
				  <meta>
				    <filename>scan.pdf</filename>
				    <processed_date>2026-11-18</processed_date>
				  </meta>
				  <content type="application/pdf"><![CDATA[Kein Datum in diesem Text]]></content>
				</root>
				""";

		assertThat(IssueDateFinder.find(layer, TODAY)).isEmpty();
	}

	@Test
	void aDateLineBeatsReferenceDatesAndDeadlines() {
		IssueDateFinder.Found found = IssueDateFinder.find("""
				Stand: 31.12.2015
				Karlsruhe, 22.02.2016
				Ihre Jahresabrechnung
				zahlbar bis 15.03.2016
				""", TODAY).orElseThrow();

		assertThat(found.date()).isEqualTo(LocalDate.of(2016, 2, 22));
		assertThat(found.confidence()).isEqualTo(0.85);
	}

	@Test
	void placeNamesOfSeveralWordsAndDenAreADateLine() {
		assertThat(find("Ihre Nachricht\nFrankfurt am Main, den 3. März 2020\nSehr geehrte Damen und Herren"))
				.isEqualTo(LocalDate.of(2020, 3, 3));
	}

	@Test
	void aSentenceEndingInADateIsNoDateLine() {
		assertThat(find("Die Zahlung erfolgte am 05.03.2024.\nKarlsruhe, 01.03.2024"))
				.isEqualTo(LocalDate.of(2024, 3, 1));
	}

	@Test
	void aLabelledDateWins() {
		assertThat(find("""
				Rechnung
				Kundennummer 4711
				Leistungszeitraum 01.02.2024 - 29.02.2024
				Rechnungsdatum: 05.03.2024
				"""))
				.isEqualTo(LocalDate.of(2024, 3, 5));
	}

	@Test
	void theDateColumnOfADinReferenceLineWins() {
		assertThat(find("""
				Ihr Zeichen   Ihre Nachricht vom   Unser Zeichen   Datum
				ab-12         15.02.2016           xy-34           22.02.2016
				"""))
				.isEqualTo(LocalDate.of(2016, 2, 22));
	}

	@Test
	void theFirstRegistrationOfACarIsNoIssueDate() {
		assertThat(find("Gutachten\nFahrzeug VW Golf EZ 12.02.2013\nDie Prüfung erfolgte am 02.03.2026."))
				.isEqualTo(LocalDate.of(2026, 3, 2));
	}

	@Test
	void aBirthDateIsNeverTheIssueDate() {
		assertThat(find("Versicherter: Max Muster\nGeburtsdatum: 01.02.1970")).isNull();
	}

	@Test
	void theDateOfTheLetterItAnswersCountsLittle() {
		assertThat(find("Ihr Schreiben vom 01.02.2024\nwir bestätigen den Eingang am 10.02.2024."))
				.isEqualTo(LocalDate.of(2024, 2, 10));
	}

	@Test
	void aLetterDatedAheadKeepsItsDateLine() {
		// insurers date the yearly letter by the month the new insurance year starts
		IssueDateFinder.Found found = IssueDateFinder.find("""
				München, Oktober 2027
				Ihre Beitragsrechnung
				fällig am 01.10.2027
				""", TODAY).orElseThrow();

		assertThat(found.date()).isEqualTo(LocalDate.of(2027, 10, 1));
		assertThat(found.confidence()).isEqualTo(0.60);
	}

	@Test
	void aFutureDateInRunningTextIsAnAppointment() {
		assertThat(find("Wir besuchen Sie am 05.12.2026 zwischen 8 und 12 Uhr.")).isNull();
		assertThat(find("Bitte beachten Sie: 01.01.2030")).isNull();
	}

	@Test
	void todaysDateCountsLittle() {
		StringBuilder text = new StringBuilder("Gedruckt am 18.11.2026\n");
		for (int i = 0; i < 30; i++)
			text.append("Zeile ").append(i).append('\n');
		text.append("Die Police wurde am 03.05.2021 ausgestellt.");

		assertThat(find(text.toString())).isEqualTo(LocalDate.of(2021, 5, 3));
	}

	@Test
	void twoDigitYearsBelongToTheirCentury() {
		assertThat(find("Mahnung vom 05.03.98")).isEqualTo(LocalDate.of(1998, 3, 5));
		assertThat(find("Mahnung vom 05.03.24")).isEqualTo(LocalDate.of(2024, 3, 5));
	}

	@Test
	void invalidDatesAndNoDatesGiveNothing() {
		assertThat(find("something 2024-99-45 here")).isNull();
		assertThat(find("kein datum hier vorhanden")).isNull();
		assertThat(find(null)).isNull();
	}

	@Test
	void scannerFileNamesCarryTheScanDay() {
		assertThat(IssueDateFinder.fromFilename("12_30_2016_02_22_VBL_2014.pdf", TODAY).orElseThrow().date())
				.isEqualTo(LocalDate.of(2016, 2, 22));
		assertThat(IssueDateFinder.fromFilename("Scan_20240305_0001.pdf", TODAY).orElseThrow().date())
				.isEqualTo(LocalDate.of(2024, 3, 5));
		assertThat(IssueDateFinder.fromFilename("Rechnung.pdf", TODAY)).isEmpty();
	}
}
