/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentFilenameTest {

	@Test
	void theFileGoesByItsTitleWithTheOriginalsExtension() {
		assertThat(DocumentFilename.of("Bezügemitteilung 2026-11",
				"2026-11-17_Lohnart Grundentgelt Tarif Zuschlag Erh. Arbeitszeit, lfd..pdf", null))
				.isEqualTo("Bezügemitteilung 2026-11.pdf");
		assertThat(DocumentFilename.of("Aktivierung deines Benutzerkontos", "0c842e24a287f052.eml", null))
				.isEqualTo("Aktivierung deines Benutzerkontos.eml");
	}

	@Test
	void anOriginalWithoutExtensionGetsTheOneOfItsType() {
		assertThat(DocumentFilename.of("Rechnung Stadtwerke", "01032026", ".pdf")).isEqualTo("Rechnung Stadtwerke.pdf");
		assertThat(DocumentFilename.of("Rechnung Stadtwerke", null, ".pdf")).isEqualTo("Rechnung Stadtwerke.pdf");
		// a year after a dot is no extension
		assertThat(DocumentFilename.of("Abrechnung", "Abrechnung 06.2023", ".pdf")).isEqualTo("Abrechnung.pdf");
	}

	@Test
	void charactersAFileSystemRefusesAreReplaced() {
		assertThat(DocumentFilename.of("Steuer 2013/2014", "scan.pdf", null)).isEqualTo("Steuer 2013-2014.pdf");
		assertThat(DocumentFilename.of("AW: Termin \"Zahnarzt\"?", "mail.eml", null))
				.isEqualTo("AW Termin Zahnarzt.eml");
		assertThat(DocumentFilename.of("Notiz...", "scan.pdf", null)).isEqualTo("Notiz.pdf");
		assertThat(DocumentFilename.of("CON", "scan.pdf", null)).isEqualTo("CON_.pdf");
		assertThat(DocumentFilename.of("x".repeat(300), "scan.pdf", null)).hasSize(DocumentFilename.MAX_BASE + 4);
	}

	@Test
	void withoutATitleTheOriginalNameStays() {
		assertThat(DocumentFilename.of(null, "Scan0001.pdf", null)).isEqualTo("Scan0001.pdf");
		assertThat(DocumentFilename.of("  ", "01032026", ".pdf")).isEqualTo("01032026.pdf");
		assertThat(DocumentFilename.of(null, null, null)).isEqualTo("document");
		// a title that already ends in the extension keeps it once
		assertThat(DocumentFilename.of("Rechnung.pdf", "scan.pdf", null)).isEqualTo("Rechnung.pdf");
	}
}
