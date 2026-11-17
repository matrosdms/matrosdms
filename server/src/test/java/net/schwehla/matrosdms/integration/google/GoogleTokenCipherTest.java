package net.schwehla.matrosdms.integration.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class GoogleTokenCipherTest {

	private static byte[] storeKey(int fill) {
		byte[] key = new byte[32];
		Arrays.fill(key, (byte) fill);
		return key;
	}

	@Test
	void roundTripsAndNeverStoresThePlainToken() {
		GoogleTokenCipher cipher = new GoogleTokenCipher(() -> storeKey(7));

		String stored = cipher.encrypt("1//refresh-token");

		assertThat(stored).startsWith("v1:").doesNotContain("refresh-token");
		assertThat(cipher.decrypt(stored)).isEqualTo("1//refresh-token");
	}

	@Test
	void freshIvPerEncryption() {
		GoogleTokenCipher cipher = new GoogleTokenCipher(() -> storeKey(7));
		assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
	}

	@Test
	void otherStoreKeyCannotDecrypt() {
		String stored = new GoogleTokenCipher(() -> storeKey(7)).encrypt("secret");
		GoogleTokenCipher other = new GoogleTokenCipher(() -> storeKey(8));

		assertThatThrownBy(() -> other.decrypt(stored)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void tamperingIsDetected() {
		GoogleTokenCipher cipher = new GoogleTokenCipher(() -> storeKey(7));
		String stored = cipher.encrypt("secret");
		byte[] raw = Base64.getDecoder().decode(stored.substring("v1:".length()));
		raw[raw.length / 2] ^= 0x01;
		String tampered = "v1:" + Base64.getEncoder().encodeToString(raw);

		assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void withoutStoreEncryptionTheTokenIsTaggedPlain() {
		GoogleTokenCipher cipher = new GoogleTokenCipher(() -> null);

		String stored = cipher.encrypt("secret");

		assertThat(stored).isEqualTo("plain:secret");
		assertThat(cipher.decrypt(stored)).isEqualTo("secret");
	}
}
