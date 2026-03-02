/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.command;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.cli.client.MatrosApiClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Recursively scans a folder and moves every file that already exists in the
 * DMS into a separate "duplicate" folder, preserving the relative subfolder
 * structure.  Files that are not in the DMS are left untouched.
 *
 * <pre>
 *   matros find-duplicate --docfolder F:\inbox --duplicate-folder F:\duplicates
 * </pre>
 *
 * Requires an active session — run {@code matros login} first.
 */
@Component
@Command(
        name = "find-duplicate",
        description = "Scan a folder recursively and move files that already exist in the DMS "
                    + "into a target folder (subfolder structure is preserved).",
        mixinStandardHelpOptions = true)
public class FindDuplicateCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(FindDuplicateCommand.class);

    @Option(names = {"--docfolder", "-d"}, required = true,
            description = "Folder to scan recursively for documents")
    private Path docFolder;

    @Option(names = {"--duplicate-folder", "-t"}, required = true,
            description = "Target folder where duplicate files will be moved")
    private Path duplicateFolder;

    @Option(names = {"--dry-run"}, defaultValue = "false",
            description = "If set, only log what would be moved without actually moving files")
    private boolean dryRun;

    @Autowired
    private MatrosApiClient apiClient;

    // -------------------------------------------------------------------------
    // Entry
    // -------------------------------------------------------------------------

    @Override
    public Integer call() {
        log.info("=== find-duplicate started ===");
        log.info("  Source  : {}", docFolder.toAbsolutePath());
        log.info("  Target  : {}", duplicateFolder.toAbsolutePath());
        log.info("  Dry-run : {}", dryRun);

        if (!Files.isDirectory(docFolder)) {
            log.error("Source folder does not exist or is not a directory: {}", docFolder);
            System.err.println("✘  Not a directory: " + docFolder);
            return 1;
        }

        // Counters
        AtomicLong total        = new AtomicLong();
        AtomicLong duplicates   = new AtomicLong();
        AtomicLong moved        = new AtomicLong();
        AtomicLong errors       = new AtomicLong();

        try (Stream<Path> walk = Files.walk(docFolder)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                total.incrementAndGet();
                try {
                    processFile(file, total.get(), duplicates, moved);
                } catch (Exception e) {
                    errors.incrementAndGet();
                    log.error("  [ERROR]  {} — {}", docFolder.relativize(file), e.getMessage());
                }
            });
        } catch (IOException e) {
            log.error("Failed to walk source folder: {}", e.getMessage(), e);
            return 1;
        }

        // Summary
        log.info("=== find-duplicate completed ===");
        log.info("  Scanned   : {}", total.get());
        log.info("  Duplicates: {}", duplicates.get());
        log.info("  Moved     : {}", moved.get());
        log.info("  Errors    : {}", errors.get());

        System.out.printf("Done.  Scanned: %d  |  Duplicates: %d  |  Moved: %d  |  Errors: %d%n",
                total.get(), duplicates.get(), moved.get(), errors.get());

        return errors.get() > 0 ? 2 : 0;
    }

    // -------------------------------------------------------------------------
    // Per-file logic
    // -------------------------------------------------------------------------

    private void processFile(Path file, long index,
                             AtomicLong duplicates, AtomicLong moved) throws Exception {

        Path relative = docFolder.relativize(file);
        log.debug("  [{} ] Checking: {}", index, relative);

        // 1 — Compute SHA-256
        String hash = sha256Hex(file);
        log.debug("         SHA-256 : {}", hash);

        // 2 — Ask the DMS
        boolean isDuplicate = apiClient.existsByHash(hash);

        if (!isDuplicate) {
            log.debug("         → not in DMS, skipping");
            return;
        }

        duplicates.incrementAndGet();
        log.info("  [DUPLICATE]  {}  ({})", relative, hash);

        if (dryRun) {
            log.info("  [DRY-RUN]   would move → {}", duplicateFolder.resolve(relative));
            return;
        }

        // 3 — Determine target path (no overwrite)
        Path target = safeTarget(duplicateFolder.resolve(relative));

        // 4 — Create parent dirs and move
        Files.createDirectories(target.getParent());
        Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
        moved.incrementAndGet();

        log.info("  [MOVED]      {} → {}", relative, duplicateFolder.relativize(target));

        // 5 — Remove empty source parent directories (best-effort)
        pruneEmptyParents(file.getParent(), docFolder);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a path that does not yet exist by appending {@code _1}, {@code _2}, …
     * before the file extension if the direct target is already taken.
     */
    private static Path safeTarget(Path proposed) {
        if (!Files.exists(proposed)) {
            return proposed;
        }

        String filename = proposed.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        String base = dot >= 0 ? filename.substring(0, dot) : filename;
        String ext  = dot >= 0 ? filename.substring(dot)    : "";

        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            Path candidate = proposed.resolveSibling(base + "_" + i + ext);
            if (!Files.exists(candidate)) {
                log.debug("         Target conflict resolved → {}", candidate.getFileName());
                return candidate;
            }
        }
        throw new IllegalStateException("Could not find a free filename for: " + proposed);
    }

    /**
     * Walks upward from {@code dir} toward {@code root} and removes any
     * directories that are now empty as a result of moving files away.
     * Stops as soon as it encounters a non-empty directory.
     */
    private static void pruneEmptyParents(Path dir, Path root) {
        try {
            Path current = dir;
            while (current != null && !current.equals(root) && current.startsWith(root)) {
                try (Stream<Path> contents = Files.list(current)) {
                    if (contents.findFirst().isPresent()) {
                        break; // still has children
                    }
                }
                Files.delete(current);
                log.debug("  [CLEANUP] Removed empty directory: {}", current);
                current = current.getParent();
            }
        } catch (IOException e) {
            log.warn("  [CLEANUP] Could not prune empty directories: {}", e.getMessage());
        }
    }

    /**
     * Computes the SHA-256 hex digest of a file using a streaming read
     * (safe for large files).
     */
    private static String sha256Hex(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        try (InputStream is = Files.newInputStream(file)) {
            int n;
            while ((n = is.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
