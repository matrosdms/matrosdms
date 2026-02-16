/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.command;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.concurrent.Callable;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "decrypt", description = "Offline recovery tool for encrypted store files")
public class OfflineDecryptCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the encrypted file (.enc)")
    private Path inputFile;

    @Option(names = {"-p", "--password"}, required = true, description = "Store Password")
    private String password;

    @Option(names = {"-s", "--salt"}, required = true, description = "Store Salt")
    private String salt;

    @Option(names = {"-o", "--out"}, description = "Output file (default: stdout)")
    private Path outputFile;

    // Crypto Constants
    private static final String ALGO_GCM = "AES/GCM/NoPadding";
    private static final String ALGO_CTR = "AES/CTR/NoPadding";
    private static final int IV_GCM = 12;
    private static final int IV_CTR = 16;
    private static final int TAG_LEN = 128;

    @Override
    public Integer call() {
        Security.addProvider(new BouncyCastleProvider());

        if (!Files.exists(inputFile)) {
            System.err.println("❌ File not found: " + inputFile);
            return 1;
        }

        try {
            System.err.println("🔐 Deriving Key...");
            byte[] key = deriveKey(password, salt);

            // Heuristic: Text layers and Thumbnails used Legacy CTR
            boolean isLegacy = inputFile.toString().endsWith(".txt.enc") 
                            || inputFile.toString().contains(".thumb.");

            String algoLabel = isLegacy ? "AES-CTR (Legacy/Text)" : "AES-GCM (Standard)";
            System.err.println("🔓 Decrypting using " + algoLabel + "...");

            try (InputStream fis = new BufferedInputStream(Files.newInputStream(inputFile))) {
                
                Cipher cipher;
                if (isLegacy) {
                    byte[] iv = new byte[IV_CTR];
                    if (fis.read(iv) != IV_CTR) throw new IllegalArgumentException("File too short for IV");
                    cipher = Cipher.getInstance(ALGO_CTR);
                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
                } else {
                    byte[] iv = new byte[IV_GCM];
                    if (fis.read(iv) != IV_GCM) throw new IllegalArgumentException("File too short for IV");
                    cipher = Cipher.getInstance(ALGO_GCM);
                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LEN, iv));
                }

                try (CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                    if (outputFile != null) {
                        try (OutputStream fos = new FileOutputStream(outputFile.toFile())) {
                            cis.transferTo(fos);
                        }
                        System.err.println("✅ Decrypted content saved to: " + outputFile);
                    } else {
                        // Pipe to stdout
                        cis.transferTo(System.out);
                    }
                }
            }
            return 0;

        } catch (Exception e) {
            System.err.println("❌ Decryption Failed: " + e.getMessage());
            // e.printStackTrace(); // Uncomment for debug
            return 1;
        }
    }

    private byte[] deriveKey(String password, String saltStr) {
        byte[] salt = saltStr.getBytes(StandardCharsets.UTF_8);
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(3)
                .withMemoryAsKB(65536)
                .withParallelism(1)
                .withSalt(salt);

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(builder.build());
        byte[] result = new byte[32];
        gen.generateBytes(password.toCharArray(), result);
        return result;
    }
}