/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopSplashTest {

	@AfterEach
	void clearDataDir() {
		System.clearProperty("MATROS_DATA_DIR");
	}

	@Test
	void reportsTheInnermostCause() {
		// What a second start of the same profile really produces: the sentence the user can act on
		// sits at the bottom of the chain, Spring's bean-creation wrapper at the top.
		Throwable failure = new IllegalStateException("Error creating bean with name 'entityManagerFactory'",
				new RuntimeException("Unable to obtain connection from database",
						new Exception("Database may be already in use: matros.mv.db")));

		assertThat(DesktopSplash.describe(failure)).startsWith("Database may be already in use: matros.mv.db");
	}

	@Test
	void namesTheLogFileWhenTheDataDirIsKnown(@TempDir Path dataDir) {
		// The desktop build has no console, so the notice must say where the stack trace landed.
		System.setProperty("MATROS_DATA_DIR", dataDir.toString());

		assertThat(DesktopSplash.describe(new IllegalStateException("boom")))
				.contains(dataDir.resolve("workspace").resolve("log").resolve("matrosdms.log").toString());
	}

	@Test
	void omitsTheLogFileWithoutADataDir() {
		assertThat(DesktopSplash.describe(new IllegalStateException("boom"))).isEqualTo("boom");
	}

	@Test
	void fallsBackToTheTypeWhenThereIsNoMessage() {
		assertThat(DesktopSplash.describe(new IllegalStateException())).contains("IllegalStateException");
	}
}
