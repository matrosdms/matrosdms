/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.schwehla.matrosdms.service.management.StoreConsistencyService.Result;
import net.schwehla.matrosdms.service.management.StoreConsistencyService.UnknownDocument;

class StoreConsistencyServiceTest {

	@TempDir
	Path store;

	@Test
	void storeAndDatabaseMatch() throws Exception {
		document("48AHX4ZGDR6", ".pdf.enc", ".txt.enc", ".thumb.jpg.enc");
		document("4BAYZA1YEU8", ".pdf.enc");

		Result result = StoreConsistencyService.compare(store, Set.of("48AHX4ZGDR6", "4BAYZA1YEU8"));

		assertThat(result.consistent()).isTrue();
		assertThat(result.storeDocuments()).isEqualTo(2);
		assertThat(result.databaseItems()).isEqualTo(2);
		assertThat(result.warnings()).isEmpty();
	}

	@Test
	void documentsTheDatabaseDoesNotKnowAreListedOldestFirst() throws Exception {
		// What the household store looked like after the database fell back in March 2026
		document("48AHX4ZGDR6", ".pdf.enc", ".txt.enc");
		touch(document("4B6AUJV6CX8", ".pdf.enc", ".txt.enc"), "2026-03-09T08:03:00Z");
		touch(document("49JZE1SK25Q", ".pdf.enc", ".txt.enc"), "2026-03-02T19:17:00Z");

		Result result = StoreConsistencyService.compare(store, Set.of("48AHX4ZGDR6"));

		assertThat(result.consistent()).isFalse();
		assertThat(result.storeDocuments()).isEqualTo(3);
		assertThat(result.unknownInStore()).extracting(UnknownDocument::id)
				.containsExactly("49JZE1SK25Q", "4B6AUJV6CX8");
		assertThat(result.unknownInStore().get(0).files())
				.containsExactly("49JZE1SK25Q.pdf.enc", "49JZE1SK25Q.txt.enc");
		assertThat(result.missingInStore()).isEmpty();
		assertThat(result.warnings()).singleElement().asString()
				.startsWith("2 documents in the document store have no database entry, the newest from 2026-03-09.")
				.contains("Integrity Check");
	}

	@Test
	void documentsWithoutAFileAreMissing() throws Exception {
		document("48AHX4ZGDR6", ".pdf.enc");

		Result result = StoreConsistencyService.compare(store, Set.of("48AHX4ZGDR6", "4BAYZA1YEU8"));

		assertThat(result.missingInStore()).containsExactly("4BAYZA1YEU8");
		assertThat(result.warnings()).singleElement().asString()
				.startsWith("1 document in the database has no file in the document store.");
	}

	@Test
	void aStoreFolderThatIsNotThereMissesEverything() {
		// e.g. a cloud drive that is not mounted
		Result result = StoreConsistencyService.compare(store.resolve("not-mounted"), Set.of("48AHX4ZGDR6"));

		assertThat(result.storeDocuments()).isZero();
		assertThat(result.missingInStore()).containsExactly("48AHX4ZGDR6");
	}

	@Test
	void filesOutsideTheLayoutAreStrayAndSystemFilesAreIgnored() throws Exception {
		document("48AHX4ZGDR6", ".pdf.enc");
		Files.writeString(store.resolve("4BA3NSBTAX3.pdf.enc"), "x"); // loose in the root
		Files.writeString(store.resolve("48A").resolve("4BAYZA1YEU8.pdf.enc"), "x"); // wrong shard
		Files.createDirectories(store.resolve("48A").resolve("nested"));
		Files.writeString(store.resolve("desktop.ini"), "x");
		Files.writeString(store.resolve("48A").resolve("Thumbs.db"), "x");
		Files.writeString(store.resolve("48A").resolve(".DS_Store"), "x");

		Result result = StoreConsistencyService.compare(store, Set.of("48AHX4ZGDR6"));

		assertThat(result.strayFiles()).containsExactlyInAnyOrder(
				"4BA3NSBTAX3.pdf.enc", "48A/4BAYZA1YEU8.pdf.enc", "48A/nested");
		assertThat(result.unknownInStore()).isEmpty();
		assertThat(result.warnings()).singleElement().asString()
				.startsWith("3 files in the document store do not belong to any document.");
	}

	@Test
	void anEmptyStoreAndDatabaseMatch() {
		Result result = StoreConsistencyService.compare(store, Set.of());

		assertThat(result.consistent()).isTrue();
	}

	/** Writes the files of one document; returns its shard folder. */
	private Path document(String id, String... suffixes) throws Exception {
		Path shard = Files.createDirectories(store.resolve(id.substring(0, 3)));
		for (String suffix : suffixes) {
			Files.writeString(shard.resolve(id + suffix), "x");
		}
		return shard;
	}

	/** Sets the modification time of all files of the documents in that shard folder. */
	private static void touch(Path shard, String instant) throws Exception {
		try (var files = Files.list(shard)) {
			for (Path f : files.toList()) {
				Files.setLastModifiedTime(f, FileTime.from(Instant.parse(instant)));
			}
		}
	}
}
