/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.exception.MatrosServiceException;

/**
 * Service for computing cryptographic hashes of files and byte arrays.
 * Uses SHA-256 algorithm for all hash calculations.
 */
@Service
public class FileHashService {

    private static final Logger log = LoggerFactory.getLogger(FileHashService.class);
    
    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    /**
     * Calculates SHA-256 hash of a file.
     * 
     * @param filePath Path to the file
     * @return Hexadecimal string representation of the hash
     * @throws MatrosServiceException if hashing fails
     */
    public String calculateHash(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }
        
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            
            try (InputStream is = new BufferedInputStream(Files.newInputStream(filePath), BUFFER_SIZE)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hashBytes = digest.digest();
            String hash = HEX_FORMAT.formatHex(hashBytes);
            
            log.debug("Calculated {} hash for file {}: {}", ALGORITHM, filePath.getFileName(), hash);
            return hash;
            
        } catch (NoSuchAlgorithmException e) {
            throw new MatrosServiceException(ALGORITHM + " algorithm not available", e);
        } catch (IOException e) {
            throw new MatrosServiceException("Failed to read file for hashing: " + filePath, e);
        }
    }

    /**
     * Calculates SHA-256 hash of a byte array.
     * Useful for computing hashes of in-memory data (uploads, SMTP attachments, etc.).
     * 
     * @param data Byte array to hash
     * @return Hexadecimal string representation of the hash
     * @throws MatrosServiceException if hashing fails
     */
    public String calculateHash(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data array cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            String hash = HEX_FORMAT.formatHex(hashBytes);
            
            log.debug("Calculated {} hash for {} bytes of data", ALGORITHM, data.length);
            return hash;
            
        } catch (NoSuchAlgorithmException e) {
            throw new MatrosServiceException(ALGORITHM + " algorithm not available", e);
        }
    }

    /**
     * Calculates SHA-256 hash from an input stream.
     * The stream is not closed by this method.
     * 
     * @param inputStream Input stream to hash
     * @return Hexadecimal string representation of the hash
     * @throws MatrosServiceException if hashing fails
     */
    public String calculateHash(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = digest.digest();
            return HEX_FORMAT.formatHex(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            throw new MatrosServiceException(ALGORITHM + " algorithm not available", e);
        } catch (IOException e) {
            throw new MatrosServiceException("Failed to read stream for hashing", e);
        }
    }

    /**
     * Verifies if a file matches the expected hash.
     * 
     * @param filePath Path to the file
     * @param expectedHash Expected hash value
     * @return true if hashes match, false otherwise
     */
    public boolean verifyHash(Path filePath, String expectedHash) {
        if (expectedHash == null || expectedHash.isEmpty()) {
            return false;
        }
        
        String actualHash = calculateHash(filePath);
        boolean matches = actualHash.equalsIgnoreCase(expectedHash);
        
        if (!matches) {
            log.warn("Hash mismatch for file {}: expected={}, actual={}", 
                filePath.getFileName(), expectedHash, actualHash);
        }
        
        return matches;
    }
}