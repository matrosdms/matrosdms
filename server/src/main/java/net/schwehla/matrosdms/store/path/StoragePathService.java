/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store.path;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for resolving physical storage paths from document UUIDs.
 * Uses a sharding strategy based on UUID prefix for better filesystem
 * performance.
 */
@Service
public class StoragePathService {

	private static final Logger log = LoggerFactory.getLogger(StoragePathService.class);

	private static final int UUID_PREFIX_LENGTH = 3;
	private static final int MIN_UUID_LENGTH = 8;

	/**
	 * Resolves the physical file path for a given UUID and suffix.
	 * Uses first 3 characters of UUID as subfolder for filesystem sharding.
	 * 
	 * @param rootFolder
	 *            Root storage directory
	 * @param uuid
	 *            Document UUID
	 * @param suffix
	 *            File suffix (e.g., ".pdf", ".txt.enc")
	 * @return Physical path to the file
	 * @throws IllegalArgumentException
	 *             if UUID is invalid
	 */
	public Path resolveFilePath(Path rootFolder, String uuid, String suffix) {
		validateUuid(uuid);
		validateRootFolder(rootFolder);

		String prefix = uuid.substring(0, UUID_PREFIX_LENGTH);
		Path subFolder = rootFolder.resolve(prefix);

		String fileName = suffix != null ? uuid + suffix : uuid;
		return subFolder.resolve(fileName);
	}

	/**
	 * Resolves the directory containing all files for a given UUID.
	 * 
	 * @param rootFolder
	 *            Root storage directory
	 * @param uuid
	 *            Document UUID
	 * @return Directory path
	 */
	public Path resolveDocumentDirectory(Path rootFolder, String uuid) {
		validateUuid(uuid);
		validateRootFolder(rootFolder);

		String prefix = uuid.substring(0, UUID_PREFIX_LENGTH);
		return rootFolder.resolve(prefix);
	}

	/**
	 * Finds all files associated with a UUID.
	 * 
	 * @param rootFolder
	 *            Root storage directory
	 * @param uuid
	 *            Document UUID
	 * @return Stream of paths matching the UUID
	 * @throws IOException
	 *             if directory listing fails
	 */
	public Stream<Path> findDocumentFiles(Path rootFolder, String uuid) throws IOException {
		Path documentDir = resolveDocumentDirectory(rootFolder, uuid);

		if (!Files.exists(documentDir)) {
			log.debug("Document directory does not exist: {}", documentDir);
			return Stream.empty();
		}

		return Files.list(documentDir)
				.filter(path -> path.getFileName().toString().startsWith(uuid));
	}

	/**
	 * Finds the main document file (excludes .txt and .thumb files).
	 * 
	 * @param rootFolder
	 *            Root storage directory
	 * @param uuid
	 *            Document UUID
	 * @return Path to main document file
	 * @throws IOException
	 *             if file not found or directory listing fails
	 */
	public Path findMainDocumentFile(Path rootFolder, String uuid) throws IOException {
		try (Stream<Path> files = findDocumentFiles(rootFolder, uuid)) {
			return files
					.filter(path -> {
						String fileName = path.getFileName().toString();
						return !fileName.contains(".txt") && !fileName.contains(".thumb");
					})
					.findFirst()
					.orElseThrow(() -> new IOException("Main document file not found for UUID: " + uuid));
		}
	}

	/**
	 * Checks if a file exists for the given UUID and suffix.
	 * 
	 * @param rootFolder
	 *            Root storage directory
	 * @param uuid
	 *            Document UUID
	 * @param suffix
	 *            File suffix
	 * @return true if file exists
	 */
	public boolean fileExists(Path rootFolder, String uuid, String suffix) {
		Path filePath = resolveFilePath(rootFolder, uuid, suffix);
		return Files.exists(filePath);
	}

	/**
	 * Creates all necessary parent directories for a file path.
	 * 
	 * @param filePath
	 *            Path to the file
	 * @throws IOException
	 *             if directory creation fails
	 */
	public void ensureDirectoriesExist(Path filePath) throws IOException {
		Path parentDir = filePath.getParent();
		if (parentDir != null && !Files.exists(parentDir)) {
			Files.createDirectories(parentDir);
			log.debug("Created directory: {}", parentDir);
		}
	}

	/**
	 * Validates that a UUID is suitable for path resolution.
	 * 
	 * @param uuid
	 *            UUID to validate
	 * @throws IllegalArgumentException
	 *             if UUID is invalid
	 */
	private void validateUuid(String uuid) {
		if (uuid == null || uuid.isEmpty()) {
			throw new IllegalArgumentException("UUID cannot be null or empty");
		}

		if (uuid.length() < MIN_UUID_LENGTH) {
			throw new IllegalArgumentException(
					String.format("UUID too short for path resolution (minimum %d characters): %s",
							MIN_UUID_LENGTH, uuid));
		}

		// Check for path traversal attempts
		if (uuid.contains("..") || uuid.contains("/") || uuid.contains("\\")) {
			throw new IllegalArgumentException("Invalid UUID contains path traversal characters: " + uuid);
		}
	}

	/**
	 * Validates the root folder.
	 * 
	 * @param rootFolder
	 *            Root folder to validate
	 * @throws IllegalArgumentException
	 *             if root folder is invalid
	 */
	private void validateRootFolder(Path rootFolder) {
		if (rootFolder == null) {
			throw new IllegalArgumentException("Root folder cannot be null");
		}
	}
}