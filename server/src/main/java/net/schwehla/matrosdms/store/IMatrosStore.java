/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import java.nio.file.Path;

import net.schwehla.matrosdms.domain.content.MDocumentStream;

/**
 * Interface for document storage operations.
 * Implementations handle different storage backends (local, S3, Azure, etc.)
 */
public interface IMatrosStore {

    /**
     * Persists a document with optional text layer.
     * 
     * @param sourceFile Main document file to store
     * @param textFile Optional extracted text file
     * @param uuid Document UUID
     * @param originalFilename Original filename for metadata
     * @return Storage result with hash and encryption info
     */
    StoreResult persist(Path sourceFile, Path textFile, String uuid, String originalFilename);

    /**
     * Loads a document as a stream.
     * 
     * @param uuid Document UUID
     * @return Document stream
     */
    MDocumentStream loadStream(String uuid);

    /**
     * Loads the text layer for a document.
     * 
     * @param uuid Document UUID
     * @return Extracted text content
     */
    String loadTextLayer(String uuid);

    /**
     * Moves a document to trash.
     * 
     * @param uuid Document UUID
     */
    void moveToTrash(String uuid);

    /**
     * Checks if a document has a thumbnail.
     * 
     * @param uuid Document UUID
     * @return true if thumbnail exists
     */
    boolean hasThumbnail(String uuid);

    /**
     * Stores a thumbnail for a document.
     * 
     * @param uuid Document UUID
     * @param data Thumbnail image data
     */
    void storeThumbnail(String uuid, byte[] data);

    /**
     * Loads a thumbnail for a document.
     * 
     * @param uuid Document UUID
     * @return Thumbnail data or null
     */
    byte[] loadThumbnail(String uuid);
}