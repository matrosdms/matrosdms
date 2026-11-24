/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.rekey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.schwehla.matrosdms.store.encryption.StoreCrypto;

class StoreRekeyerTest {

	private static final byte[] OLD = StoreCrypto.deriveKey(StoreCrypto.DEFAULT_PASSWORD, StoreCrypto.DEFAULT_SALT);
	private static final byte[] NEW = StoreCrypto.deriveKey("correct horse battery staple", "f3Zq8LmX1bVt");
	private static final byte[] STRANGER = StoreCrypto.deriveKey("some other password", "s4lt");

	@TempDir
	Path tmp;

	Path repo;
	Path backup;
	final Map<String, byte[]> plain = new HashMap<>();
	final List<String> log = new ArrayList<>();

	@BeforeEach
	void repository() throws Exception {
		repo = Files.createDirectories(tmp.resolve("cloud/repository/schwehla-household"));
		backup = tmp.resolve("local/rekey-backup");
		write("documents/49A/49ADF7PW2QT.pdf.enc", "%PDF Renteninformation 2006", OLD);
		write("documents/49A/49ADF7PW2QT.txt.enc", "<root><![CDATA[Ihre Renteninformation]]></root>", OLD);
		write("documents/48R/48RWM3Z1VAJ.pdf.enc", "%PDF Bezügemitteilung 2016-09", OLD);
		write("recovered-2026-11-17/49JZE1SK25Q.pdf.enc", "%PDF recovered", OLD);
		Files.createDirectories(repo.resolve("db-backup"));
		Files.writeString(repo.resolve("db-backup/matros-db-daily.zip"), "not encrypted, not touched");
	}

	@Test
	void everyFileGetsTheNewKeyAndTheOldOnesWaitOutsideTheRepository() throws Exception {
		StoreRekeyer.Report r = new StoreRekeyer(repo, OLD, NEW, log::add).apply(backup);

		assertThat(r.ok()).isTrue();
		assertThat(r.rekeyed()).isEqualTo(4);
		for (var e : plain.entrySet()) {
			Path f = repo.resolve(e.getKey());
			assertThat(StoreCrypto.decrypt(Files.readAllBytes(f), NEW)).isEqualTo(e.getValue());
			assertThat(StoreCrypto.opens(f, OLD)).isFalse();
			// the original, still with the old key, in the backup
			assertThat(StoreCrypto.decrypt(Files.readAllBytes(backup.resolve(e.getKey())), OLD)).isEqualTo(e.getValue());
		}
		assertThat(Files.readString(backup.resolve(StoreRekeyer.MANIFEST))).contains("documents/49A/49ADF7PW2QT.pdf.enc\t");
		assertThat(repo.resolve(StoreRekeyer.STAGING)).doesNotExist();
		assertThat(Files.readString(repo.resolve("db-backup/matros-db-daily.zip"))).isEqualTo("not encrypted, not touched");
	}

	@Test
	void aFileNeitherKeyOpensStopsEverythingBeforeAnyChange() throws Exception {
		write("documents/49B/49BFOREIGN01.pdf.enc", "%PDF with another password", STRANGER);
		Map<String, byte[]> before = bytesOfAll();

		StoreRekeyer.Report r = new StoreRekeyer(repo, OLD, NEW, log::add).apply(backup);

		assertThat(r.ok()).isFalse();
		assertThat(r.unreadable()).containsExactly("documents/49B/49BFOREIGN01.pdf.enc");
		assertThat(bytesOfAll()).containsExactlyInAnyOrderEntriesOf(before);
		assertThat(backup).doesNotExist();
		assertThat(repo.resolve(StoreRekeyer.STAGING)).doesNotExist();
	}

	@Test
	void runningAgainFinishesWhatAnInterruptedRunLeftAndChangesNothingElse() throws Exception {
		new StoreRekeyer(repo, OLD, NEW, log::add).apply(backup);
		// as if a later document had still been written with the old key
		write("documents/4WV/4WV22Z7RQG6.pdf.enc", "%PDF Spendenbescheinigung", OLD);

		StoreRekeyer.Report again = new StoreRekeyer(repo, OLD, NEW, log::add).apply(tmp.resolve("local/second"));

		assertThat(again.rekeyed()).isEqualTo(1);
		assertThat(again.withNewKey()).isEqualTo(4);
		assertThat(new StoreRekeyer(repo, NEW, null, log::add).check().withCurrentKey()).isEqualTo(5);
	}

	@Test
	void nothingLeftToDoCreatesNoBackup() throws Exception {
		new StoreRekeyer(repo, OLD, NEW, log::add).apply(backup);

		StoreRekeyer.Report again = new StoreRekeyer(repo, OLD, NEW, log::add).apply(tmp.resolve("local/third"));

		assertThat(again.rekeyed()).isZero();
		assertThat(again.withNewKey()).isEqualTo(4);
		assertThat(tmp.resolve("local/third")).doesNotExist();
	}

	@Test
	void theCheckOnlyReads() throws Exception {
		Map<String, byte[]> before = bytesOfAll();

		StoreRekeyer.Report r = new StoreRekeyer(repo, OLD, null, log::add).check();

		assertThat(r.ok()).isTrue();
		assertThat(r.withCurrentKey()).isEqualTo(4);
		assertThat(bytesOfAll()).containsExactlyInAnyOrderEntriesOf(before);
		assertThat(new StoreRekeyer(repo, STRANGER, null, log::add).check().unreadable()).hasSize(4);
	}

	@Test
	void theBackupMustBeOutsideTheRepositoryAndEmpty() throws Exception {
		StoreRekeyer rekeyer = new StoreRekeyer(repo, OLD, NEW, log::add);

		assertThatThrownBy(() -> rekeyer.apply(repo.resolve("backup")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("outside the repository");
		Files.createDirectories(backup);
		Files.writeString(backup.resolve("something"), "x");
		assertThatThrownBy(() -> rekeyer.apply(backup))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("new or empty");
		assertThatThrownBy(() -> new StoreRekeyer(tmp.resolve("local"), OLD, NEW, log::add).check())
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Not a MatrosDMS repository");
	}

	private void write(String rel, String content, byte[] key) throws Exception {
		byte[] p = content.getBytes(StandardCharsets.UTF_8);
		Path f = repo.resolve(rel);
		Files.createDirectories(f.getParent());
		Files.write(f, StoreCrypto.encrypt(p, key));
		if (key != STRANGER)
			plain.put(rel, p);
	}

	private Map<String, byte[]> bytesOfAll() throws Exception {
		Map<String, byte[]> m = new HashMap<>();
		try (var files = Files.walk(repo)) {
			for (Path f : files.filter(Files::isRegularFile).toList())
				m.put(repo.relativize(f).toString(), Files.readAllBytes(f));
		}
		return m;
	}
}
