/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.exception.MatrosServiceException;
import net.schwehla.matrosdms.store.path.StoragePathService;

/**
 * Service for managing document trash operations.
 * Provides soft-delete functionality with recovery capability.
 */
@Service
public class TrashService {

    private static final Logger log = LoggerFactory.getLogger(TrashService.class);

    private final AppServerSpringConfig appServerSpringConfig;
    private final StoragePathService pathService;

    private Path trashRoot;

    public TrashService(
            AppServerSpringConfig appServerSpringConfig,
            StoragePathService pathService) {
        this.appServerSpringConfig = appServerSpringConfig;
        this.pathService = pathService;
    }

    @PostConstruct
    public void init() {
        this.trashRoot = Path.of(appServerSpringConfig.getServer().getTrash().getPath());
        
        try {
            Files.createDirectories(trashRoot);
            log.info("Trash directory initialized at: {}", trashRoot.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create trash directory: " + trashRoot, e);
        }
    }

    /**
     * Moves all files associated with a document to trash.
     * Files are prefixed with timestamp for recovery tracking.
     * 
     * @param rootFolder Document storage root
     * @param uuid Document UUID
     */
    public void moveToTrash(Path rootFolder, String uuid) {
        try {
            Path documentDir = pathService.resolveDocumentDirectory(rootFolder, uuid);
            
            if (!Files.exists(documentDir)) {
                log.warn("Document directory not found for UUID {}, nothing to trash", uuid);
                return;
            }

            long timestamp = Instant.now().toEpochMilli();
            int movedCount = 0;

            try (Stream<Path> files = Files.list(documentDir)) {
                for (Path file : files.filter(p -> p.getFileName().toString().startsWith(uuid)).toList()) {
                    String trashedFileName = timestamp + "_" + file.getFileName().toString();
                    Path targetPath = trashRoot.resolve(trashedFileName);

                    try {
                        Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        movedCount++;
                        log.debug("Moved to trash: {} -> {}", file.getFileName(), trashedFileName);
                    } catch (IOException e) {
                        log.error("Failed to move file to trash: {}", file, e);
                    }
                }
            }

            if (movedCount > 0) {
                log.info("Moved {} file(s) to trash for document: {}", movedCount, uuid);
            } else {
                log.warn("No files found to trash for document: {}", uuid);
            }

        } catch (IOException e) {
            log.error("Failed to move document to trash: {}", uuid, e);
            throw new MatrosServiceException("Trash operation failed for document: " + uuid, e);
        }
    }

    /**
     * Permanently deletes all files associated with a document.
     * This operation cannot be undone.
     * 
     * @param rootFolder Document storage root
     * @param uuid Document UUID
     */
    public void permanentlyDelete(Path rootFolder, String uuid) {
        try {
            Path documentDir = pathService.resolveDocumentDirectory(rootFolder, uuid);
            
            if (!Files.exists(documentDir)) {
                log.warn("Document directory not found for UUID {}, nothing to delete", uuid);
                return;
            }

            int deletedCount = 0;

            try (Stream<Path> files = Files.list(documentDir)) {
                for (Path file : files.filter(p -> p.getFileName().toString().startsWith(uuid)).toList()) {
                    try {
                        Files.delete(file);
                        deletedCount++;
                        log.debug("Permanently deleted: {}", file.getFileName());
                    } catch (IOException e) {
                        log.error("Failed to delete file: {}", file, e);
                    }
                }
            }

            // Try to remove directory if empty
            try (Stream<Path> remaining = Files.list(documentDir)) {
                if (remaining.findAny().isEmpty()) {
                    Files.delete(documentDir);
                    log.debug("Removed empty directory: {}", documentDir);
                }
            }

            log.warn("Permanently deleted {} file(s) for document: {}", deletedCount, uuid);

        } catch (IOException e) {
            log.error("Failed to permanently delete document: {}", uuid, e);
            throw new MatrosServiceException("Permanent deletion failed for document: " + uuid, e);
        }
    }

    /**
     * Empties the trash by permanently deleting all trashed files.
     * 
     * @return Number of files deleted
     */
    public int emptyTrash() {
        log.info("Emptying trash...");
        
        try {
            int deletedCount = 0;
            
            try (Stream<Path> files = Files.list(trashRoot)) {
                for (Path file : files.toList()) {
                    try {
                        Files.delete(file);
                        deletedCount++;
                    } catch (IOException e) {
                        log.error("Failed to delete trash file: {}", file, e);
                    }
                }
            }

            log.info("Trash emptied: {} file(s) deleted", deletedCount);
            return deletedCount;

        } catch (IOException e) {
            throw new MatrosServiceException("Failed to empty trash", e);
        }
    }

    /**
     * Counts the number of files currently in trash.
     * 
     * @return Number of trashed files
     */
    public long countTrashedFiles() {
        try (Stream<Path> files = Files.list(trashRoot)) {
            return files.count();
        } catch (IOException e) {
            log.error("Failed to count trash files", e);
            return 0;
        }
    }

    /**
     * Gets the total size of all files in trash.
     * 
     * @return Total size in bytes
     */
    public long getTrashedFilesSize() {
        try (Stream<Path> files = Files.list(trashRoot)) {
            return files
                .filter(Files::isRegularFile)
                .mapToLong(file -> {
                    try {
                        return Files.size(file);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        } catch (IOException e) {
            log.error("Failed to calculate trash size", e);
            return 0;
        }
    }
}