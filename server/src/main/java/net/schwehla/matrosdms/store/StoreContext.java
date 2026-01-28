/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class StoreContext {

	private static Path rootFolder;
	private static byte[] keyBytes;
	private static boolean encryptionEnabled;
	private static final String ALGO_AES = "AES/CTR/NoPadding";
	private static final StoragePathStrategy pathStrategy = new StoragePathStrategy();

	public static void init(Path path, byte[] key, boolean encrypt) {
		rootFolder = path;
		keyBytes = key;
		encryptionEnabled = encrypt;
	}

	public static String readTextFile(String uuid) {
		if (rootFolder == null)
			return "";

		if (encryptionEnabled) {
			Path encFile = pathStrategy.getPhysicalPath(rootFolder, uuid, ".txt.enc");
			if (Files.exists(encFile))
				return readEncrypted(encFile);
		}

		Path plainFile = pathStrategy.getPhysicalPath(rootFolder, uuid, ".txt");
		if (Files.exists(plainFile)) {
			try {
				return Files.readString(plainFile, StandardCharsets.UTF_8);
			} catch (IOException e) {
				return "";
			}
		}
		return "";
	}

	private static String readEncrypted(Path file) {
		// CONSOLIDATION: Wrap all stream levels in the try-with-resources header
		try (InputStream fis = new BufferedInputStream(Files.newInputStream(file))) {
			byte[] iv = new byte[16];
			if (fis.read(iv) != 16)
				return "";

			Cipher cipher = Cipher.getInstance(ALGO_AES);
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(iv));

			try (CipherInputStream cis = new CipherInputStream(fis, cipher)) {
				// Note: readAllBytes() is safe here as sidecar .txt files are filtered by Tika
				// safety limits
				return new String(cis.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			return "";
		}
	}
}
