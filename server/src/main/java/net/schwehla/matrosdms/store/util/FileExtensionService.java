/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store.util;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Service for extracting and normalizing file extensions.
 * Also provides MIME type to extension mapping.
 */
@Service
public class FileExtensionService {

    private static final String DEFAULT_EXTENSION = ".bin";

    // MIME type to extension mapping
    private static final Map<String, String> MIME_TO_EXTENSION = Map.ofEntries(
        // Images
        Map.entry("image/png", ".png"),
        Map.entry("image/jpeg", ".jpg"),
        Map.entry("image/jpg", ".jpg"),
        Map.entry("image/gif", ".gif"),
        Map.entry("image/webp", ".webp"),
        Map.entry("image/svg+xml", ".svg"),
        Map.entry("image/bmp", ".bmp"),
        Map.entry("image/tiff", ".tiff"),
        Map.entry("image/x-icon", ".ico"),
        
        // Documents
        Map.entry("application/pdf", ".pdf"),
        Map.entry("application/msword", ".doc"),
        Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
        Map.entry("application/vnd.ms-excel", ".xls"),
        Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
        Map.entry("application/vnd.ms-powerpoint", ".ppt"),
        Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
        
        // Text
        Map.entry("text/plain", ".txt"),
        Map.entry("text/html", ".html"),
        Map.entry("text/css", ".css"),
        Map.entry("text/javascript", ".js"),
        Map.entry("application/javascript", ".js"),
        Map.entry("text/xml", ".xml"),
        Map.entry("application/xml", ".xml"),
        Map.entry("application/json", ".json"),
        
        // Fonts
        Map.entry("font/woff", ".woff"),
        Map.entry("font/woff2", ".woff2"),
        Map.entry("font/ttf", ".ttf"),
        Map.entry("font/otf", ".otf"),
        Map.entry("application/vnd.ms-fontobject", ".eot"),
        Map.entry("application/font-woff", ".woff"),
        Map.entry("application/font-woff2", ".woff2"),
        
        // Archives
        Map.entry("application/zip", ".zip"),
        Map.entry("application/x-rar-compressed", ".rar"),
        Map.entry("application/x-7z-compressed", ".7z"),
        Map.entry("application/gzip", ".gz"),
        
        // Media
        Map.entry("video/mp4", ".mp4"),
        Map.entry("video/mpeg", ".mpeg"),
        Map.entry("video/webm", ".webm"),
        Map.entry("audio/mpeg", ".mp3"),
        Map.entry("audio/ogg", ".ogg"),
        Map.entry("audio/wav", ".wav")
    );

    /**
     * Gets the file extension for a given MIME type.
     * 
     * @param mimeType The MIME type
     * @return The extension including the dot, or empty string if unknown
     */
    public String getExtensionForMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return "";
        }
        
        // Normalize MIME type (lowercase, remove parameters)
        String normalized = mimeType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        
        return MIME_TO_EXTENSION.getOrDefault(normalized, "");
    }

    /**
     * Gets the file extension for a MIME type with a default fallback.
     * 
     * @param mimeType The MIME type
     * @return The extension or ".bin" if unknown
     */
    public String getExtensionForMimeTypeOrDefault(String mimeType) {
        String extension = getExtensionForMimeType(mimeType);
        return extension.isEmpty() ? DEFAULT_EXTENSION : extension;
    }

    /**
     * Extracts the file extension from a filename.
     * Returns empty string if no extension is found.
     * 
     * @param filename The filename
     * @return The extension including the dot (e.g., ".pdf"), or empty string if none
     */
    public String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        int lastSeparatorIndex = Math.max(
            filename.lastIndexOf('/'),
            filename.lastIndexOf('\\')
        );

        // No extension or dot is part of directory name
        if (lastDotIndex <= 0 || lastDotIndex < lastSeparatorIndex) {
            return "";
        }

        return filename.substring(lastDotIndex);
    }

    /**
     * Extracts the file extension from a Path.
     * 
     * @param filePath The file path
     * @return The extension including the dot, or empty string if none
     */
    public String getExtension(Path filePath) {
        if (filePath == null) {
            return "";
        }
        
        Path fileName = filePath.getFileName();
        if (fileName == null) {
            return "";
        }
        
        return getExtension(fileName.toString());
    }

    /**
     * Gets the extension or returns a default extension if none is found.
     * 
     * @param filename The filename
     * @return The extension or ".bin" if no extension found
     */
    public String getExtensionOrDefault(String filename) {
        String extension = getExtension(filename);
        return extension.isEmpty() ? DEFAULT_EXTENSION : extension;
    }

    /**
     * Gets the extension from a path or returns a default extension.
     * 
     * @param filePath The file path
     * @return The extension or ".bin" if no extension found
     */
    public String getExtensionOrDefault(Path filePath) {
        String extension = getExtension(filePath);
        return extension.isEmpty() ? DEFAULT_EXTENSION : extension;
    }

    /**
     * Normalizes an extension to lowercase.
     * 
     * @param extension The extension to normalize
     * @return Normalized extension
     */
    public String normalize(String extension) {
        if (extension == null) {
            return "";
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    /**
     * Checks if a filename has a specific extension (case-insensitive).
     * 
     * @param filename The filename
     * @param extension The extension to check (with or without dot)
     * @return true if the filename has the specified extension
     */
    public boolean hasExtension(String filename, String extension) {
        if (filename == null || extension == null) {
            return false;
        }

        String fileExt = getExtension(filename);
        String normalizedExt = extension.startsWith(".") ? extension : "." + extension;
        
        return fileExt.equalsIgnoreCase(normalizedExt);
    }

    /**
     * Removes the extension from a filename.
     * 
     * @param filename The filename
     * @return Filename without extension
     */
    public String removeExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        int lastDotIndex = filename.lastIndexOf('.');
        int lastSeparatorIndex = Math.max(
            filename.lastIndexOf('/'),
            filename.lastIndexOf('\\')
        );

        if (lastDotIndex <= 0 || lastDotIndex < lastSeparatorIndex) {
            return filename;
        }

        return filename.substring(0, lastDotIndex);
    }
}
