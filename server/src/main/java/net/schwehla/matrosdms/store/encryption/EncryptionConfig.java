/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store.encryption;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig.StoreElement;
import net.schwehla.matrosdms.domain.storage.EStorageLocation;

/**
 * Configuration service for encryption.
 * Manages encryption keys derived from passwords using Argon2id KDF.
 * 
 * Thread-safe and immutable after initialization.
 */
@Component
// Created at start-up despite spring.main.lazy-initialization: the key check below must refuse a
// wrong password before the first document is filed with it, not on the first download.
@Lazy(false)
public class EncryptionConfig {

	private static final Logger log = LoggerFactory.getLogger(EncryptionConfig.class);

	private final AppServerSpringConfig appServerSpringConfig;

	private volatile boolean encryptionEnabled;
	private volatile byte[] encryptionKey;
	private volatile String cryptographyMode;

	public EncryptionConfig(AppServerSpringConfig appServerSpringConfig) {
		this.appServerSpringConfig = appServerSpringConfig;
	}

	@PostConstruct
	public void init() {
		// Register BouncyCastle provider if not already registered
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
			log.info("BouncyCastle security provider registered");
		}

		StoreElement storeConfig = appServerSpringConfig.getServer().getStore().stream()
				.filter(e -> e.getType() == EStorageLocation.LOCAL)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No LOCAL store configured"));

		String cryptor = storeConfig.getCryptor();
		this.cryptographyMode = cryptor;
		this.encryptionEnabled = cryptor != null &&
				(cryptor.contains("AES") || cryptor.contains("CTR") || cryptor.contains("GCM"));

		if (encryptionEnabled) {
			initializeEncryption(storeConfig);
			log.info("🔐 Store Encryption ENABLED (Mode: {})", cryptographyMode);
		} else {
			log.info("🔓 Store Encryption DISABLED");
		}
	}

	@PreDestroy
	public void cleanup() {
		// Zero out encryption key from memory
		if (encryptionKey != null) {
			Arrays.fill(encryptionKey, (byte) 0);
			log.debug("Encryption key wiped from memory");
		}
	}

	private void initializeEncryption(StoreElement storeConfig) {
		String password = storeConfig.getPassword();
		String saltString = storeConfig.getSalt();

		if (password == null || password.isEmpty()) {
			throw new IllegalStateException("Encryption enabled but no password configured. " +
					"Set MATROS_STORE_PASSWORD environment variable.");
		}

		if (saltString == null || saltString.isEmpty()) {
			throw new IllegalStateException("Encryption enabled but no salt configured. " +
					"Set MATROS_STORE_SALT environment variable.");
		}

		// Warn about insecure defaults
		if (StoreCrypto.DEFAULT_PASSWORD.equals(password)) {
			log.warn("⚠️  WARNING: Using default password! Change MATROS_STORE_PASSWORD in production "
					+ "(re-encrypt the store first: matros rekey)!");
		}

		if (StoreCrypto.DEFAULT_SALT.equals(saltString)) {
			log.warn("⚠️  WARNING: Using default salt! Change MATROS_STORE_SALT in production!");
		}

		byte[] key;
		try {
			key = StoreCrypto.deriveKey(password, saltString);
			log.debug("Encryption key derived using Argon2id (iterations={}, memory={}KB)",
					StoreCrypto.ARGON2_ITERATIONS, StoreCrypto.ARGON2_MEMORY_KB);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to derive encryption key", e);
		}

		// Before anything is written with it: a key that fits none of the stored files is a wrong
		// password or salt, and documents filed with it could only be read with that wrong key.
		verifyKeyFitsStore(storeConfig.getPath() == null ? null : Path.of(storeConfig.getPath()), key);
		this.encryptionKey = key;
	}

	/** How many of the smallest store files the start-up check tries. */
	static final int KEY_CHECK_SAMPLES = 3;

	/**
	 * Refuses to start when the key opens none of a few store files. One damaged file does not stop the
	 * start, a wrong password or salt does. An empty or missing store (first start) passes.
	 */
	static void verifyKeyFitsStore(Path storeDir, byte[] key) {
		if (storeDir == null || !Files.isDirectory(storeDir))
			return;
		List<Path> samples;
		try (Stream<Path> files = Files.walk(storeDir, 2)) {
			// shorter than IV + tag is a broken write, not a sample that says anything about the key
			long minimal = StoreCrypto.IV_LENGTH + StoreCrypto.TAG_LENGTH_BIT / 8;
			samples = files.filter(p -> p.getFileName().toString().endsWith(".enc") && Files.isRegularFile(p))
					.filter(p -> sizeOf(p) > minimal)
					.sorted(Comparator.comparingLong(EncryptionConfig::sizeOf))
					.limit(KEY_CHECK_SAMPLES)
					.toList();
		} catch (IOException | UncheckedIOException e) {
			log.warn("Could not list {} to check the store key: {}", storeDir, e.getMessage());
			return;
		}
		if (samples.isEmpty() || samples.stream().anyMatch(p -> StoreCrypto.opens(p, key)))
			return;
		throw new IllegalStateException("The store password does not fit the documents.\n\n"
				+ "None of the files checked in " + storeDir + " can be opened with the configured "
				+ "MATROS_STORE_PASSWORD and MATROS_STORE_SALT. Set the password and salt the documents "
				+ "were encrypted with (after 'matros rekey': the new ones). MatrosDMS has changed nothing.");
	}

	private static long sizeOf(Path p) {
		try {
			return Files.size(p);
		} catch (IOException e) {
			return Long.MAX_VALUE;
		}
	}

	/**
	 * Checks if encryption is enabled.
	 * 
	 * @return true if encryption is enabled
	 */
	public boolean isEncryptionEnabled() {
		return encryptionEnabled;
	}

	/**
	 * Gets the encryption key.
	 * Returns a copy to prevent modification of the internal key.
	 * 
	 * @return Copy of the encryption key, or null if encryption is disabled
	 */
	public byte[] getEncryptionKey() {
		if (!encryptionEnabled || encryptionKey == null) {
			return null;
		}
		return Arrays.copyOf(encryptionKey, encryptionKey.length);
	}

	/**
	 * Gets the cryptography mode configuration.
	 * 
	 * @return Cryptography mode string (e.g., "AES_CTR", "AES_GCM")
	 */
	public String getCryptographyMode() {
		return cryptographyMode;
	}

	/**
	 * Gets the file suffix for encrypted files.
	 * 
	 * @return ".enc" if encryption is enabled, "" otherwise
	 */
	public String getEncryptedFileSuffix() {
		return encryptionEnabled ? ".enc" : "";
	}
}