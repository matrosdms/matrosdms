/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * High-performance file type detection using "Magic Bytes" (File Signatures).
 * This avoids the overhead of loading the entire Tika framework for common file types.
 *
 * Performance: ~0.01ms vs Tika's ~50-200ms
 */
@Service
public class FileSignatureService {

    private static final Logger log = LoggerFactory.getLogger(FileSignatureService.class);

    // Common Signatures (Hex)
    private static final byte[] SIG_PDF = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] SIG_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] SIG_JPG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] SIG_ZIP = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK.. (Zip, Docx, Odt)

    public String quickDetect(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) return null;

            if (startsWith(header, SIG_PDF)) return "application/pdf";
            if (startsWith(header, SIG_PNG)) return "image/png";
            if (startsWith(header, SIG_JPG)) return "image/jpeg";
            
            // Zip containers need deeper inspection (could be docx, odt, or plain zip)
            // We return generic zip here or null to let Tika handle specific Office formats
            if (startsWith(header, SIG_ZIP)) return null; 

            return null; // Fallback to Tika
        } catch (IOException e) {
            log.warn("Failed to read magic bytes: {}", e.getMessage());
            return null;
        }
    }

    private boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) return false;
        }
        return true;
    }
}