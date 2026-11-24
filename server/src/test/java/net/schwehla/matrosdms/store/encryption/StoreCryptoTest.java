/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoreCryptoTest {

	/** The shipped default password and salt; public, so the key is no secret either. */
	private static final byte[] DEFAULT_KEY = StoreCrypto.deriveKey(StoreCrypto.DEFAULT_PASSWORD, StoreCrypto.DEFAULT_SALT);
	private static final byte[] OTHER_KEY = StoreCrypto.deriveKey("a much better password", "x7Kp2Qa9");

	@TempDir
	Path dir;

	@Test
	void theKeyIsTheOneEveryExistingStoreWasWrittenWith() {
		// computed independently with the same Argon2id parameters; it opened every file of three
		// real stores, so a change here would make all of them unreadable
		assertThat(HexFormat.of().formatHex(DEFAULT_KEY))
				.isEqualTo("f84a26b824f4af487ec84b25147f1d5fc0b2c739f52fa6fffd5d22080275f212");
	}

	@Test
	void theServerReadsWhatStoreCryptoWritesAndTheOtherWayRound() throws Exception {
		EncryptionService service = new EncryptionService();
		byte[] plain = "Renteninformation 2015".getBytes(StandardCharsets.UTF_8);

		Path byCrypto = dir.resolve("a.pdf.enc");
		Files.write(byCrypto, StoreCrypto.encrypt(plain, DEFAULT_KEY));
		assertThat(service.decryptBytes(byCrypto, DEFAULT_KEY)).isEqualTo(plain);

		Path byService = dir.resolve("b.pdf.enc");
		service.encryptBytes(plain, byService, DEFAULT_KEY);
		assertThat(StoreCrypto.decrypt(Files.readAllBytes(byService), DEFAULT_KEY)).isEqualTo(plain);
	}

	@Test
	void aFileOpensOnlyWithItsOwnKey() throws Exception {
		Path f = dir.resolve("c.txt.enc");
		Files.write(f, StoreCrypto.encrypt("text".getBytes(StandardCharsets.UTF_8), DEFAULT_KEY));

		assertThat(StoreCrypto.opens(f, DEFAULT_KEY)).isTrue();
		assertThat(StoreCrypto.opens(f, OTHER_KEY)).isFalse();
		assertThat(StoreCrypto.opens(dir.resolve("missing.enc"), DEFAULT_KEY)).isFalse();
	}

	@Test
	void theStartRefusesAKeyThatOpensNoneOfTheStoredFiles() throws Exception {
		Path store = storeWith(DEFAULT_KEY);

		assertThatThrownBy(() -> EncryptionConfig.verifyKeyFitsStore(store, OTHER_KEY))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("store password does not fit")
				.hasMessageContaining("changed nothing");
		EncryptionConfig.verifyKeyFitsStore(store, DEFAULT_KEY); // the right one passes
	}

	@Test
	void oneDamagedFileDoesNotStopTheStart() throws Exception {
		Path store = storeWith(DEFAULT_KEY);
		// smallest file of the store, so it is among the samples
		Files.write(store.resolve("49A/49ADAMAGED1.txt.enc"), new byte[40]);
		// a broken write shorter than IV + tag is no sample at all
		Files.write(store.resolve("49A/49AEMPTYYY1.txt.enc"), new byte[0]);

		EncryptionConfig.verifyKeyFitsStore(store, DEFAULT_KEY);
	}

	@Test
	void aNewOrMissingStorePasses() throws Exception {
		Path empty = Files.createDirectories(dir.resolve("empty"));

		EncryptionConfig.verifyKeyFitsStore(empty, OTHER_KEY);
		EncryptionConfig.verifyKeyFitsStore(dir.resolve("missing"), OTHER_KEY);
		EncryptionConfig.verifyKeyFitsStore(null, OTHER_KEY);
	}

	private Path storeWith(byte[] key) throws Exception {
		Path store = Files.createDirectories(dir.resolve("documents/49A"));
		for (int i = 0; i < 4; i++) {
			byte[] plain = ("document " + i + " ").repeat(10 + i).getBytes(StandardCharsets.UTF_8);
			Files.write(store.resolve("49A0000000" + i + ".txt.enc"), StoreCrypto.encrypt(plain, key));
		}
		return store.getParent();
	}
}
