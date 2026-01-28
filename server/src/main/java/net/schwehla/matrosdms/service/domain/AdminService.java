/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig.StoreElement;
import net.schwehla.matrosdms.domain.content.MDocumentStream;
import net.schwehla.matrosdms.domain.storage.EStorageLocation;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.repository.ItemRepository;
import net.schwehla.matrosdms.service.message.IntegrityReport;
import net.schwehla.matrosdms.store.IMatrosStore;

@Service
public class AdminService {

	private static final Logger log = LoggerFactory.getLogger(AdminService.class);

	@Autowired
	ItemRepository itemRepository;
	@Autowired
	AppServerSpringConfig appConfig;
	@Autowired
	IMatrosStore matrosStore;

	// Export to rootdir/export (outside workspace for cloud sync safety)
	@Value("${app.base-path}/export")
	private String exportBasePath;

	@Transactional(readOnly = true)
	public IntegrityReport runIntegrityCheck() {
		log.info("Starting Integrity Check...");
		IntegrityReport report = new IntegrityReport();

		StoreElement localStore = getLocalStore();
		Path rootPath = Path.of(localStore.getPath());

		List<DBItem> allItems = itemRepository.findAll();
		report.setTotalDbItems(allItems.size());

		for (DBItem item : allItems) {
			String uuid = item.getUuid();
			if (uuid == null)
				continue;

			Path folder = rootPath.resolve(uuid.substring(0, 3));
			File folderFile = folder.toFile();

			boolean found = false;
			if (folderFile.exists()) {
				for (File f : folderFile.listFiles()) {
					if (f.getName().startsWith(uuid) && !f.getName().contains(".txt")) {
						found = true;
						break;
					}
				}
			}

			if (!found) {
				report.addMissingItem(uuid, item.getName());
				log.warn("INTEGRITY FAIL: Missing file for item '{}' ({})", item.getName(), uuid);
			}
		}

		log.info(
				"Integrity Check Complete. Scanned {} items, found {} missing.",
				report.getTotalDbItems(),
				report.getMissingCount());
		return report;
	}

	@Transactional(readOnly = true)
	public void createArchiveExport() {
		Path targetDir = Paths.get(exportBasePath);
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		Path exportDir = targetDir.resolve("export-" + timestamp);

		try {
			Files.createDirectories(exportDir);
			log.info("Starting decrypted export to: {}", exportDir);

			List<DBItem> allItems = itemRepository.findAll();
			int exported = 0;
			int failed = 0;

			for (DBItem item : allItems) {
				try {
					// Get context name for folder structure
					String contextName = item.getInfoContext() != null
							? sanitizeFilename(item.getInfoContext().getName())
							: "_unsorted";

					Path contextDir = exportDir.resolve(contextName);
					if (!Files.exists(contextDir)) {
						Files.createDirectories(contextDir);
					}

					// Load decrypted stream from store
					MDocumentStream stream = matrosStore.loadStream(item.getUuid());
					if (stream == null || stream.getInputStream() == null) {
						log.warn("Export: No content for item {} ({})", item.getName(), item.getUuid());
						failed++;
						continue;
					}

					// Get original filename from DB, fallback to item name + extension
					String fileName;
					if (item.getFile() != null && item.getFile().getFilename() != null) {
						fileName = sanitizeFilename(item.getFile().getFilename());
					} else {
						// Fallback: item name + extension from mime type
						String baseName = sanitizeFilename(item.getName());
						String extension = getExtension(stream.getFilename(), item);
						fileName = baseName + extension;
					}

					// Handle duplicates
					Path targetFile = contextDir.resolve(fileName);
					int counter = 1;
					while (Files.exists(targetFile)) {
						int dotIdx = fileName.lastIndexOf('.');
						String base = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
						String ext = dotIdx > 0 ? fileName.substring(dotIdx) : "";
						targetFile = contextDir.resolve(base + "_" + counter + ext);
						counter++;
					}

					// Write decrypted content
					try (InputStream is = stream.getInputStream();
							OutputStream os = Files.newOutputStream(targetFile)) {
						is.transferTo(os);
					}

					exported++;
					if (exported % 100 == 0) {
						log.info("Export progress: {} items exported", exported);
					}

				} catch (Exception e) {
					log.warn("Export failed for item {} ({}): {}",
							item.getName(), item.getUuid(), e.getMessage());
					failed++;
				}
			}

			log.info("Export completed: {} items exported, {} failed, target: {}",
					exported, failed, exportDir);

		} catch (Exception e) {
			log.error("Export failed", e);
			throw new RuntimeException("Export failed", e);
		}
	}

	private String sanitizeFilename(String name) {
		if (name == null)
			return "unnamed";
		return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
	}

	private String getExtension(String filename, DBItem item) {
		// First try: original filename
		if (filename != null && filename.contains(".")) {
			return filename.substring(filename.lastIndexOf("."));
		}
		// Fallback: derive from mime type stored in DB file info
		if (item.getFile() != null && item.getFile().getMimetype() != null) {
			String mime = item.getFile().getMimetype();
			return switch (mime) {
				case "application/pdf" -> ".pdf";
				case "message/rfc822" -> ".eml";
				case "application/msword" -> ".doc";
				case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
				case "application/vnd.ms-excel" -> ".xls";
				case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
				case "application/vnd.ms-powerpoint" -> ".ppt";
				case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
				case "application/zip" -> ".zip";
				case "application/xml", "text/xml" -> ".xml";
				case "application/json" -> ".json";
				case "text/plain" -> ".txt";
				case "text/html" -> ".html";
				case "text/csv" -> ".csv";
				case "image/jpeg" -> ".jpg";
				case "image/png" -> ".png";
				case "image/gif" -> ".gif";
				case "image/tiff" -> ".tiff";
				case "image/webp" -> ".webp";
				case "image/bmp" -> ".bmp";
				default -> mime.startsWith("image/") ? "." + mime.substring(6) : ".bin";
			};
		}
		return ".bin";
	}

	private StoreElement getLocalStore() {
		return appConfig.getServer().getStore().stream()
				.filter(e -> e.getType() == EStorageLocation.LOCAL)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("No 'LOCAL' store defined"));
	}
}
