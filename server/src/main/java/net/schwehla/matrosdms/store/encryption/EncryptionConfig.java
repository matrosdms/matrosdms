/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store.encryption;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class EncryptionConfig {

	private static final Logger log = LoggerFactory.getLogger(EncryptionConfig.class);

	private static final int KEY_LENGTH_BYTES = 32; // AES-256
	private static final int ARGON2_ITERATIONS = 3;
	private static final int ARGON2_MEMORY_KB = 65536; // 64 MB
	private static final int ARGON2_PARALLELISM = 1;

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
		if ("CHANGE-THIS-PASSWORD".equals(password)) {
			log.warn("⚠️  WARNING: Using default password! Change MATROS_STORE_PASSWORD in production!");
		}

		if ("MATROS_DMS_SALT".equals(saltString)) {
			log.warn("⚠️  WARNING: Using default salt! Change MATROS_STORE_SALT in production!");
		}

		try {
			byte[] salt = saltString.getBytes(StandardCharsets.UTF_8);

			Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
					.withVersion(Argon2Parameters.ARGON2_VERSION_13)
					.withIterations(ARGON2_ITERATIONS)
					.withMemoryAsKB(ARGON2_MEMORY_KB)
					.withParallelism(ARGON2_PARALLELISM)
					.withSalt(salt);

			Argon2BytesGenerator generator = new Argon2BytesGenerator();
			generator.init(builder.build());

			this.encryptionKey = new byte[KEY_LENGTH_BYTES];
			generator.generateBytes(password.toCharArray(), this.encryptionKey);

			log.debug("Encryption key derived using Argon2id (iterations={}, memory={}KB)",
					ARGON2_ITERATIONS, ARGON2_MEMORY_KB);

		} catch (Exception e) {
			throw new IllegalStateException("Failed to derive encryption key", e);
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