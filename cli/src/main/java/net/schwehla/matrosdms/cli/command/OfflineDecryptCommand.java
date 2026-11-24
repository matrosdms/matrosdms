/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.store.encryption.StoreCrypto;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Decrypts one store file without a running server. Every store file, document and text layer
 * alike, is AES-256-GCM (see {@link StoreCrypto}); the file is only written when the tag matched,
 * so a wrong password or salt yields an error, never garbage.
 */
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

    @Override
    public Integer call() {
        if (!Files.exists(inputFile)) {
            System.err.println("[ERROR] File not found: " + inputFile);
            return 1;
        }
        try {
            System.err.println("Deriving key ...");
            byte[] plain = StoreCrypto.decrypt(Files.readAllBytes(inputFile), StoreCrypto.deriveKey(password, salt));
            if (outputFile != null) {
                Files.write(outputFile, plain);
                System.err.println("[OK] Decrypted content saved to: " + outputFile);
            } else {
                System.out.write(plain);
                System.out.flush();
            }
            return 0;
        } catch (GeneralSecurityException e) {
            System.err.println("[ERROR] Cannot decrypt: wrong password or salt, or the file is damaged.");
            return 1;
        } catch (Exception e) {
            System.err.println("[ERROR] Decryption failed: " + e.getMessage());
            return 1;
        }
    }
}
