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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig.StoreElement;
import net.schwehla.matrosdms.domain.admin.ExportItemMetadata;
import net.schwehla.matrosdms.domain.content.MDocumentStream;
import net.schwehla.matrosdms.domain.storage.EStorageLocation;
import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.repository.ItemRepository;
import net.schwehla.matrosdms.service.TikaService;
import net.schwehla.matrosdms.service.message.IntegrityReport;
import net.schwehla.matrosdms.store.FileUtils;
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
	@Autowired
	FileUtils fileUtils;
	@Autowired
	AttributeLookupService attributeLookupService;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	TikaService tikaService;

	@Value("${app.base-path}/export")
	private String exportBasePath;

	@Transactional(readOnly = true)
	public IntegrityReport runIntegrityCheck() {
		log.info("Starting Integrity Check (Bit-Rot Detection)...");
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

			File foundFile = null;
			if (folderFile.exists()) {
				for (File f : folderFile.listFiles()) {
					if (f.getName().startsWith(uuid)
							&& !f.getName().endsWith(".txt")
							&& !f.getName().endsWith(".txt.enc")
							&& !f.getName().endsWith(".thumb.jpg")
							&& !f.getName().endsWith(".thumb.jpg.enc")) {
						foundFile = f;
						break;
					}
				}
			}

			if (foundFile == null) {
				report.addMissingItem(uuid, item.getName());
				log.warn("INTEGRITY FAIL: Missing file for item '{}' ({})", item.getName(), uuid);
				continue;
			}

			if (item.getFile() != null && item.getFile().getSha256Stored() != null) {
				try {
					String actualHash = fileUtils.getSHA256(foundFile.toPath());
					String expectedHash = item.getFile().getSha256Stored();

					if (!expectedHash.equalsIgnoreCase(actualHash)) {
						report.addCorruptItem(uuid, item.getName(), expectedHash, actualHash);
						log.error("CORRUPTION DETECTED: Item '{}' ({}). Expected {}, but disk has {}",
								item.getName(), uuid, expectedHash, actualHash);
					}
				} catch (Exception e) {
					log.error("Error hashing file for integrity check: " + uuid, e);
				}
			}
		}

		log.info(
				"Integrity Check Complete. Scanned {} items. Missing: {}, Corrupt: {}.",
				report.getTotalDbItems(),
				report.getMissingCount(),
				report.getCorruptCount());

		return report;
	}

	@Transactional(readOnly = true)
	public void createArchiveExport() {
		Path targetDir = Paths.get(exportBasePath);
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		Path exportDir = targetDir.resolve("export-" + timestamp);

		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		try {
			Files.createDirectories(exportDir);
			log.info("Starting decrypted export to: {}", exportDir);

			List<DBItem> allItems = itemRepository.findAll();
			List<ExportItemMetadata> globalManifest = new ArrayList<>();

			int exported = 0;
			int failed = 0;

			for (DBItem item : allItems) {
				try {
					String contextName = item.getInfoContext() != null
							? sanitizeFilename(item.getInfoContext().getName())
							: "_unsorted";

					Path contextDir = exportDir.resolve(contextName);
					if (!Files.exists(contextDir)) {
						Files.createDirectories(contextDir);
					}

					MDocumentStream stream = matrosStore.loadStream(item.getUuid());
					if (stream == null || stream.getInputStream() == null) {
						log.warn("Export: No content for item {} ({})", item.getName(), item.getUuid());
						failed++;
						continue;
					}

					String fileName;
					if (item.getFile() != null && item.getFile().getFilename() != null) {
						fileName = sanitizeFilename(item.getFile().getFilename());
					} else {
						String baseName = sanitizeFilename(item.getName());
						String extension = getExtension(stream.getFilename(), item);
						fileName = baseName + extension;
					}

					Path targetFile = contextDir.resolve(fileName);
					int counter = 1;
					while (Files.exists(targetFile)) {
						int dotIdx = fileName.lastIndexOf('.');
						String base = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
						String ext = dotIdx > 0 ? fileName.substring(dotIdx) : "";
						fileName = base + "_" + counter + ext;
						targetFile = contextDir.resolve(fileName);
						counter++;
					}

					try (InputStream is = stream.getInputStream();
							OutputStream os = Files.newOutputStream(targetFile)) {
						is.transferTo(os);
					}

					ExportItemMetadata meta = mapToExport(item, fileName);
					globalManifest.add(meta);

					Path sidecarFile = contextDir.resolve(fileName + ".json");
					objectMapper.writeValue(sidecarFile.toFile(), meta);

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

			Path globalFile = exportDir.resolve("global_index.json");
			objectMapper.writeValue(globalFile.toFile(), globalManifest);

			log.info("Export completed: {} items exported, {} failed, target: {}",
					exported, failed, exportDir);

		} catch (Exception e) {
			log.error("Export failed", e);
			throw new RuntimeException("Export failed", e);
		}
	}

	private ExportItemMetadata mapToExport(DBItem item, String filename) {
		ExportItemMetadata meta = new ExportItemMetadata();
		meta.uuid = item.getUuid();
		meta.name = item.getName();
		meta.description = item.getDescription();
		meta.originalFilename = item.getFile() != null ? item.getFile().getFilename() : null;
		meta.filename = filename;

		meta.context = item.getInfoContext() != null ? item.getInfoContext().getName() : null;
		meta.store = item.getStore() != null ? item.getStore().getShortname() : null;

		if (item.getIssueDate() != null)
			meta.dateIssued = item.getIssueDate().toString();
		if (item.getDateCreated() != null)
			meta.dateCreated = item.getDateCreated().toString();

		if (item.getSource() != null)
			meta.source = item.getSource().name();
		if (item.getFile() != null)
			meta.sha256 = item.getFile().getSha256Original();

		if (item.getKindList() != null) {
			for (DBCategory cat : item.getKindList()) {
				meta.tags.add(cat.getName());
			}
		}

		if (item.getAttributes() != null) {
			for (Map.Entry<String, Object> entry : item.getAttributes().entrySet()) {
				String attrName = attributeLookupService.getName(entry.getKey());
				if (attrName != null) {
					meta.attributes.put(attrName, entry.getValue());
				} else {
					meta.attributes.put("uuid_" + entry.getKey(), entry.getValue());
				}
			}
		}

		return meta;
	}

	private String sanitizeFilename(String name) {
		if (name == null)
			return "unnamed";
		return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
	}

	private String getExtension(String filename, DBItem item) {
		// 1. Trust filename if available
		if (filename != null && filename.contains(".")) {
			return filename.substring(filename.lastIndexOf("."));
		}
		// 2. Ask Tika based on stored DB MimeType
		if (item.getFile() != null && item.getFile().getMimetype() != null) {
			return tikaService.getExtensionForMimeType(item.getFile().getMimetype());
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