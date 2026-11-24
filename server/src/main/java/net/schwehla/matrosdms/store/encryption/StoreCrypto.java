/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store.encryption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

/**
 * The store's file format and key, without Spring, so the server and the CLI (which only sees the
 * plugin-api jar) encrypt and derive keys with the very same code.
 *
 * <p>
 * Key: Argon2id(password, salt as UTF-8; 3 iterations, 64 MB, parallelism 1), 32 bytes. File: a
 * 12-byte random IV followed by AES-256-GCM ciphertext with a 128-bit tag.
 */
public final class StoreCrypto {

	public static final String CIPHER = "AES/GCM/NoPadding";
	public static final int IV_LENGTH = 12;
	public static final int TAG_LENGTH_BIT = 128;
	public static final int KEY_LENGTH_BYTES = 32;

	/** Shipped defaults: public, so a store encrypted with them is readable by anyone. */
	public static final String DEFAULT_PASSWORD = "CHANGE-THIS-PASSWORD";
	public static final String DEFAULT_SALT = "MATROS_DMS_SALT";

	static final int ARGON2_ITERATIONS = 3;
	static final int ARGON2_MEMORY_KB = 65536;
	static final int ARGON2_PARALLELISM = 1;

	private static final SecureRandom RANDOM = new SecureRandom();

	private StoreCrypto() {
	}

	/** The store key for a password and salt. */
	public static byte[] deriveKey(String password, String salt) {
		if (password == null || password.isEmpty())
			throw new IllegalArgumentException("No store password");
		if (salt == null || salt.isEmpty())
			throw new IllegalArgumentException("No store salt");
		Argon2BytesGenerator generator = new Argon2BytesGenerator();
		generator.init(new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
				.withVersion(Argon2Parameters.ARGON2_VERSION_13)
				.withIterations(ARGON2_ITERATIONS)
				.withMemoryAsKB(ARGON2_MEMORY_KB)
				.withParallelism(ARGON2_PARALLELISM)
				.withSalt(salt.getBytes(StandardCharsets.UTF_8))
				.build());
		byte[] key = new byte[KEY_LENGTH_BYTES];
		generator.generateBytes(password.toCharArray(), key);
		return key;
	}

	/**
	 * Decrypts a whole store file. Throws {@link GeneralSecurityException} when the key does not fit or
	 * the file was changed; GCM releases no plaintext before the tag is verified.
	 */
	public static byte[] decrypt(byte[] storeFile, byte[] key) throws GeneralSecurityException {
		if (storeFile.length < IV_LENGTH)
			throw new GeneralSecurityException("Not a store file: shorter than its IV");
		Cipher cipher = Cipher.getInstance(CIPHER);
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
				new GCMParameterSpec(TAG_LENGTH_BIT, storeFile, 0, IV_LENGTH));
		return cipher.doFinal(storeFile, IV_LENGTH, storeFile.length - IV_LENGTH);
	}

	/** Encrypts in the store format with a fresh IV. */
	public static byte[] encrypt(byte[] plain, byte[] key) throws GeneralSecurityException {
		byte[] iv = new byte[IV_LENGTH];
		RANDOM.nextBytes(iv);
		Cipher cipher = Cipher.getInstance(CIPHER);
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
		byte[] out = new byte[IV_LENGTH + cipher.getOutputSize(plain.length)];
		System.arraycopy(iv, 0, out, 0, IV_LENGTH);
		cipher.doFinal(plain, 0, plain.length, out, IV_LENGTH);
		return out;
	}

	/** True when the key opens the file: decrypted completely and the tag matched. */
	public static boolean opens(Path file, byte[] key) {
		try {
			decrypt(Files.readAllBytes(file), key);
			return true;
		} catch (IOException | GeneralSecurityException e) {
			return false;
		}
	}
}
