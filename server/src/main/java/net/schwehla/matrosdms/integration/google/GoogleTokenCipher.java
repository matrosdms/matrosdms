/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Supplier;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.store.encryption.EncryptionConfig;

/**
 * Encrypts the Google refresh token at rest. The database and its snapshots usually
 * sit in a cloud-synced folder, and a refresh token grants lasting access to the
 * user's Google Tasks and MatrosDMS calendar.
 *
 * <p>AES-256-GCM with a key derived from the document store key (HMAC-SHA256 with a
 * fixed label), so it is never the same key that encrypts documents. With store
 * encryption switched off there is no key to derive from and the token is stored
 * as-is, tagged so it can be told apart.
 */
@Component
public class GoogleTokenCipher {

	private static final Logger log = LoggerFactory.getLogger(GoogleTokenCipher.class);

	private static final String PREFIX_GCM = "v1:";
	private static final String PREFIX_PLAIN = "plain:";
	private static final byte[] KEY_LABEL = "matrosdms/google-oauth-token/v1".getBytes(StandardCharsets.UTF_8);
	private static final int IV_LENGTH = 12;
	private static final int TAG_BITS = 128;

	private final Supplier<byte[]> baseKey;
	private final SecureRandom random = new SecureRandom();
	private volatile SecretKeySpec key;
	private volatile boolean resolved;

	@Autowired
	public GoogleTokenCipher(EncryptionConfig encryptionConfig) {
		this(encryptionConfig::getEncryptionKey);
	}

	GoogleTokenCipher(Supplier<byte[]> baseKey) {
		this.baseKey = baseKey;
	}

	public String encrypt(String plain) {
		SecretKeySpec k = key();
		if (k == null) {
			return PREFIX_PLAIN + plain;
		}
		try {
			byte[] iv = new byte[IV_LENGTH];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, k, new GCMParameterSpec(TAG_BITS, iv));
			byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			byte[] out = ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array();
			return PREFIX_GCM + Base64.getEncoder().encodeToString(out);
		} catch (Exception e) {
			throw new IllegalStateException("Could not encrypt Google token", e);
		}
	}

	public String decrypt(String stored) {
		if (stored.startsWith(PREFIX_PLAIN)) {
			return stored.substring(PREFIX_PLAIN.length());
		}
		if (!stored.startsWith(PREFIX_GCM)) {
			throw new IllegalStateException("Unknown Google token format");
		}
		SecretKeySpec k = key();
		if (k == null) {
			throw new IllegalStateException(
					"Google token is encrypted but store encryption is off - reconnect the Google account");
		}
		try {
			byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX_GCM.length()));
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(TAG_BITS, in, 0, IV_LENGTH));
			byte[] plain = cipher.doFinal(in, IV_LENGTH, in.length - IV_LENGTH);
			return new String(plain, StandardCharsets.UTF_8);
		} catch (Exception e) {
			// Wrong key (store password changed) or tampered value
			throw new IllegalStateException("Could not decrypt Google token - reconnect the Google account", e);
		}
	}

	private SecretKeySpec key() {
		if (!resolved) {
			synchronized (this) {
				if (!resolved) {
					key = deriveKey(baseKey.get());
					resolved = true;
				}
			}
		}
		return key;
	}

	private static SecretKeySpec deriveKey(byte[] base) {
		if (base == null) {
			log.warn("Store encryption is off: the Google refresh token is stored unencrypted");
			return null;
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(base, "HmacSHA256"));
			return new SecretKeySpec(mac.doFinal(KEY_LABEL), "AES");
		} catch (Exception e) {
			throw new IllegalStateException("Could not derive Google token key", e);
		} finally {
			Arrays.fill(base, (byte) 0);
		}
	}
}
