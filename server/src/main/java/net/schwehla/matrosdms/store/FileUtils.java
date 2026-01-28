/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class FileUtils {

	static final String ALGORITHM = "SHA-256";

	public String getSHA256(File sourcePath) throws Exception {
		return getSHA256(sourcePath.toPath());
	}

	public String getSHA256(Path sourcePath) throws Exception {
		MessageDigest md = MessageDigest.getInstance(ALGORITHM);
		try (InputStream is = Files.newInputStream(sourcePath)) {
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				md.update(buffer, 0, bytesRead);
			}
		}
		return HexFormat.of().formatHex(md.digest());
	}

	// NEW: Calculate Hash from Memory (for SMTP/Uploads)
	public String getSHA256(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance(ALGORITHM);
			byte[] hash = md.digest(data);
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			throw new RuntimeException("SHA-256 algorithm not found", e);
		}
	}

	public String getExtension(String filename) {
		if (filename == null)
			return ".bin";
		int i = filename.lastIndexOf('.');
		return (i >= 0) ? filename.substring(i) : "";
	}
}
