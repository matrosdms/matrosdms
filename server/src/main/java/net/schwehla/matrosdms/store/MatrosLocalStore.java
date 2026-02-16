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
import java.nio.file.StandardCopyOption;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig.StoreElement;
import net.schwehla.matrosdms.domain.content.MDocumentStream;
import net.schwehla.matrosdms.domain.storage.EStorageLocation;
import net.schwehla.matrosdms.exception.EntityNotFoundException;
import net.schwehla.matrosdms.exception.MatrosServiceException;
import net.schwehla.matrosdms.store.encryption.EncryptionConfig;
import net.schwehla.matrosdms.store.encryption.EncryptionService;
import net.schwehla.matrosdms.store.path.StoragePathService;
import net.schwehla.matrosdms.store.service.TrashService;
import net.schwehla.matrosdms.store.util.FileExtensionService;
import net.schwehla.matrosdms.store.util.FileHashService;

/**
 * Local filesystem implementation of document storage.
 * Handles encrypted and unencrypted storage with proper file organization.
 */
@Component
@Primary
public class MatrosLocalStore implements IMatrosStore {

    private static final Logger log = LoggerFactory.getLogger(MatrosLocalStore.class);

    private final AppServerSpringConfig config;
    private final EncryptionConfig encryptionConfig;
    private final EncryptionService encryptionService;
    private final StoragePathService pathService;
    private final FileHashService hashService;
    private final FileExtensionService extensionService;
    private final TrashService trashService;

    private Path rootFolder;

    public MatrosLocalStore(
            AppServerSpringConfig config,
            EncryptionConfig encryptionConfig,
            EncryptionService encryptionService,
            StoragePathService pathService,
            FileHashService hashService,
            FileExtensionService extensionService,
            TrashService trashService) {
        this.config = config;
        this.encryptionConfig = encryptionConfig;
        this.encryptionService = encryptionService;
        this.pathService = pathService;
        this.hashService = hashService;
        this.extensionService = extensionService;
        this.trashService = trashService;
    }

    @PostConstruct
    public void init() {
        StoreElement storeConfig = config.getServer().getStore().stream()
            .filter(e -> e.getType() == EStorageLocation.LOCAL)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No LOCAL store configured"));

        this.rootFolder = Path.of(storeConfig.getPath());

        try {
            Files.createDirectories(rootFolder);
            log.info("📁 Document storage initialized at: {}", rootFolder.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory: " + rootFolder, e);
        }
    }

    @Override
    public StoreResult persist(Path sourceFile, Path textFile, String uuid, String originalFilename) {
        log.debug("Storing document: uuid={}, filename={}", uuid, originalFilename);

        StoreResult result = new StoreResult();
        
        try {
            String extension = extensionService.getExtensionOrDefault(sourceFile);
            String encSuffix = encryptionConfig.getEncryptedFileSuffix();
            
            Path targetFile = pathService.resolveFilePath(rootFolder, uuid, extension + encSuffix);
            Path sidecarText = pathService.resolveFilePath(rootFolder, uuid, ".txt" + encSuffix);

            pathService.ensureDirectoriesExist(targetFile);

            // Store main document
            if (encryptionConfig.isEncryptionEnabled()) {
                byte[] key = encryptionConfig.getEncryptionKey();
                encryptionService.encryptFile(sourceFile, targetFile, key);
                result.setCryptSettings("AES-GCM-256");
            } else {
                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                result.setCryptSettings("NONE");
            }

            // Calculate hash of stored file
            result.setSHA256(hashService.calculateHash(targetFile));

            // Store text layer if provided
            if (textFile != null && Files.exists(textFile)) {
                if (encryptionConfig.isEncryptionEnabled()) {
                    byte[] key = encryptionConfig.getEncryptionKey();
                    encryptionService.encryptFile(textFile, sidecarText, key);
                } else {
                    Files.copy(textFile, sidecarText, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            log.info("✓ Document stored: uuid={}, hash={}, encrypted={}", 
                uuid, result.getSHA256(), encryptionConfig.isEncryptionEnabled());

            return result;

        } catch (IOException e) {
            throw new MatrosServiceException("Failed to store document: " + uuid, e);
        }
    }

    @Override
    public MDocumentStream loadStream(String uuid) {
        log.debug("Loading document stream: uuid={}", uuid);

        try {
            Path documentFile = pathService.findMainDocumentFile(rootFolder, uuid);
            long fileSize = Files.size(documentFile);

            InputStream inputStream;

            if (encryptionConfig.isEncryptionEnabled() && documentFile.toString().endsWith(".enc")) {
                byte[] key = encryptionConfig.getEncryptionKey();
                inputStream = encryptionService.decryptFile(documentFile, key);
                fileSize = Math.max(0, fileSize - encryptionService.getEncryptionOverhead());
            } else {
                inputStream = new BufferedInputStream(Files.newInputStream(documentFile));
            }

            MDocumentStream stream = new MDocumentStream(inputStream, fileSize);
            String fileName = documentFile.getFileName().toString().replace(".enc", "");
            stream.setFilename(fileName);

            log.debug("✓ Document loaded: uuid={}, size={} bytes", uuid, fileSize);
            return stream;

        } catch (IOException e) {
            throw new EntityNotFoundException("Document not found: " + uuid);
        }
    }

    @Override
    public String loadTextLayer(String uuid) {
        log.debug("Loading text layer: uuid={}", uuid);

        String encSuffix = encryptionConfig.getEncryptedFileSuffix();
        Path textFile = pathService.resolveFilePath(rootFolder, uuid, ".txt" + encSuffix);

        if (!Files.exists(textFile)) {
            log.debug("No text layer found for {}", uuid);
            return "";
        }

        try {
            if (encryptionConfig.isEncryptionEnabled()) {
                byte[] key = encryptionConfig.getEncryptionKey();
                return encryptionService.decryptTextFile(textFile, key);
            } else {
                return Files.readString(textFile, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to read text layer for {}", uuid, e);
            return "";
        }
    }

    @Override
    public void moveToTrash(String uuid) {
        log.info("Moving document to trash: uuid={}", uuid);
        trashService.moveToTrash(rootFolder, uuid);
    }

    @Override
    public boolean hasThumbnail(String uuid) {
        String suffix = ".thumb.jpg" + encryptionConfig.getEncryptedFileSuffix();
        return pathService.fileExists(rootFolder, uuid, suffix);
    }

    @Override
    public void storeThumbnail(String uuid, byte[] data) {
        log.debug("Storing thumbnail: uuid={}, size={} bytes", uuid, data.length);

        try {
            String suffix = ".thumb.jpg" + encryptionConfig.getEncryptedFileSuffix();
            Path targetFile = pathService.resolveFilePath(rootFolder, uuid, suffix);
            
            pathService.ensureDirectoriesExist(targetFile);

            if (encryptionConfig.isEncryptionEnabled()) {
                byte[] key = encryptionConfig.getEncryptionKey();
                encryptionService.encryptBytes(data, targetFile, key);
            } else {
                Files.write(targetFile, data);
            }

        } catch (IOException e) {
            throw new MatrosServiceException("Failed to store thumbnail for " + uuid, e);
        }
    }

    @Override
    public byte[] loadThumbnail(String uuid) {
        log.debug("Loading thumbnail: uuid={}", uuid);

        try {
            String suffix = ".thumb.jpg" + encryptionConfig.getEncryptedFileSuffix();
            Path thumbnailFile = pathService.resolveFilePath(rootFolder, uuid, suffix);

            if (!Files.exists(thumbnailFile)) {
                return null;
            }

            if (encryptionConfig.isEncryptionEnabled()) {
                byte[] key = encryptionConfig.getEncryptionKey();
                return encryptionService.decryptBytes(thumbnailFile, key);
            } else {
                return Files.readAllBytes(thumbnailFile);
            }

        } catch (IOException e) {
            log.error("Failed to load thumbnail for {}", uuid, e);
            return null;
        }
    }
}