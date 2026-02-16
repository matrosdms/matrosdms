/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store.encryption;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.exception.MatrosServiceException;

/**
 * Service for encrypting and decrypting files using AES-256-GCM.
 * Provides authenticated encryption with confidentiality and integrity.
 */
@Service
public class EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);

    private static final String AES_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    
    private static final int IV_LENGTH = 12; // GCM standard
    private static final int TAG_LENGTH_BIT = 128;
    private static final int BUFFER_SIZE = 8192;

    private final SecureRandom secureRandom;

    public EncryptionService() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypts a file using AES-256-GCM.
     * 
     * @param source Source file to encrypt
     * @param target Target encrypted file
     * @param key Encryption key (must be 32 bytes for AES-256)
     * @throws IOException if file operations fail
     */
    public void encryptFile(Path source, Path target, byte[] key) throws IOException {
        validateKey(key);
        
        byte[] iv = generateIV();
        
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, AES_ALGORITHM), spec);

            try (OutputStream fos = new BufferedOutputStream(Files.newOutputStream(target), BUFFER_SIZE)) {
                fos.write(iv);
                try (CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                     InputStream fis = Files.newInputStream(source)) {
                    fis.transferTo(cos);
                }
            }
            
            log.debug("Encrypted file: {} -> {}", source.getFileName(), target.getFileName());
            
        } catch (Exception e) {
            throw new MatrosServiceException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts a file using AES-256-GCM and returns an input stream.
     * The caller is responsible for closing the stream.
     * 
     * @param encryptedFile Encrypted source file
     * @param key Decryption key
     * @return InputStream for reading decrypted content
     * @throws IOException if file operations fail
     */
    public InputStream decryptFile(Path encryptedFile, byte[] key) throws IOException {
        validateKey(key);
        
        try {
            InputStream fis = new BufferedInputStream(Files.newInputStream(encryptedFile), BUFFER_SIZE);
            
            byte[] iv = new byte[IV_LENGTH];
            int bytesRead = fis.read(iv);
            if (bytesRead != IV_LENGTH) {
                fis.close();
                throw new MatrosServiceException("Invalid encrypted file: missing or incomplete IV");
            }

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, AES_ALGORITHM), spec);

            return new CipherInputStream(fis, cipher);
            
        } catch (Exception e) {
            throw new MatrosServiceException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts a text file and returns the content as a string.
     * 
     * @param encryptedFile Encrypted text file
     * @param key Decryption key
     * @return Decrypted text content
     * @throws IOException if file operations fail
     */
    public String decryptTextFile(Path encryptedFile, byte[] key) throws IOException {
        try (InputStream is = decryptFile(encryptedFile, key)) {
            byte[] decrypted = is.readAllBytes();
            return new String(decrypted, StandardCharsets.UTF_8);
        }
    }

    /**
     * Encrypts text content and writes to file.
     * 
     * @param text Text content to encrypt
     * @param target Target file path
     * @param key Encryption key
     * @throws IOException if file operations fail
     */
    public void encryptText(String text, Path target, byte[] key) throws IOException {
        Path tempFile = Files.createTempFile("matros-text-", ".tmp");
        try {
            Files.writeString(tempFile, text, StandardCharsets.UTF_8);
            encryptFile(tempFile, target, key);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Encrypts byte data.
     * 
     * @param data Data to encrypt
     * @param target Target file path
     * @param key Encryption key
     * @throws IOException if file operations fail
     */
    public void encryptBytes(byte[] data, Path target, byte[] key) throws IOException {
        Path tempFile = Files.createTempFile("matros-bytes-", ".tmp");
        try {
            Files.write(tempFile, data);
            encryptFile(tempFile, target, key);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Decrypts a file and returns the content as bytes.
     * 
     * @param encryptedFile Encrypted file
     * @param key Decryption key
     * @return Decrypted bytes
     * @throws IOException if file operations fail
     */
    public byte[] decryptBytes(Path encryptedFile, byte[] key) throws IOException {
        try (InputStream is = decryptFile(encryptedFile, key)) {
            return is.readAllBytes();
        }
    }

    /**
     * Calculates the size overhead added by encryption.
     * 
     * @return Size overhead in bytes (IV + authentication tag)
     */
    public long getEncryptionOverhead() {
        return IV_LENGTH + (TAG_LENGTH_BIT / 8);
    }

    // Private helper methods

    private byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private void validateKey(byte[] key) {
        if (key == null || key.length != 32) {
            throw new IllegalArgumentException("Encryption key must be 32 bytes for AES-256");
        }
    }
}