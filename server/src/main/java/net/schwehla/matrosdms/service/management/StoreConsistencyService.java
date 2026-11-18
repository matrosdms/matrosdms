/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.management;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.storage.EStorageLocation;
import net.schwehla.matrosdms.repository.ItemRepository;

/**
 * Compares the document store with the database by id: every document in the store must belong to
 * an item, and every item must have its document.
 *
 * <p>
 * Documents in the store that the database does not know are what a database that fell back to an
 * older state leaves behind, and what restoring an older backup leaves: the documents added since
 * are still on disk. That is how the household database lost 2026-03-02..09 without anybody
 * noticing until November (H2 #4247). The save point next to the database refuses a start that
 * fell back beyond the last checkpoint; this check also sees the minutes before it, and a restore
 * whose save point was deleted on purpose.
 *
 * <p>
 * Cheap - one listing of the store and one query, no hashing - so it runs after every start. The
 * Integrity Check job runs it too and lists what it found.
 */
@Service
public class StoreConsistencyService {

	private static final Logger log = LoggerFactory.getLogger(StoreConsistencyService.class);

	/** Files the operating system or a sync client drops into folders. */
	private static final Set<String> IGNORED_FILES = Set.of("desktop.ini", "thumbs.db");

	private static final int LOGGED_PER_KIND = 50;

	public record UnknownDocument(String id, List<String> files, Instant lastModified) {
	}

	public record Result(
			Instant checkedAt,
			int databaseItems,
			int storeDocuments,
			List<UnknownDocument> unknownInStore,
			List<String> missingInStore,
			List<String> strayFiles) {

		public boolean consistent() {
			return unknownInStore.isEmpty() && missingInStore.isEmpty() && strayFiles.isEmpty();
		}

		/** One sentence per problem, written for the person who has to act on it. */
		public List<String> warnings() {
			List<String> warnings = new ArrayList<>();
			if (!unknownInStore.isEmpty()) {
				Instant newest = unknownInStore.get(unknownInStore.size() - 1).lastModified();
				warnings.add(count(unknownInStore.size(), "document in the document store has",
						"documents in the document store have")
						+ " no database entry, the newest from "
						+ LocalDate.ofInstant(newest, ZoneId.systemDefault())
						+ ". The database may have been set back to an older state, or an older backup was"
						+ " restored. The Integrity Check under Settings > System Jobs lists them.");
			}
			if (!missingInStore.isEmpty()) {
				warnings.add(count(missingInStore.size(), "document in the database has",
						"documents in the database have")
						+ " no file in the document store. Is the store folder available? The Integrity"
						+ " Check under Settings > System Jobs lists them.");
			}
			if (!strayFiles.isEmpty()) {
				warnings.add(count(strayFiles.size(), "file in the document store does",
						"files in the document store do")
						+ " not belong to any document. The Integrity Check under Settings > System Jobs"
						+ " lists them.");
			}
			return warnings;
		}

		private static String count(int n, String one, String many) {
			return n == 1 ? "1 " + one : n + " " + many;
		}
	}

	private final ItemRepository itemRepository;
	private final AppServerSpringConfig appConfig;

	private volatile Result latest;

	public StoreConsistencyService(ItemRepository itemRepository, AppServerSpringConfig appConfig) {
		this.itemRepository = itemRepository;
		this.appConfig = appConfig;
	}

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
		try {
			check();
		} catch (RuntimeException e) {
			log.warn("[Store check] Could not compare the document store with the database: {}", e.toString());
		}
	}

	/** The result of the last check, running one if there was none yet. */
	public Result latest() {
		Result result = latest;
		return result != null ? result : check();
	}

	public synchronized Result check() {
		Result result = compare(storeRoot(), new HashSet<>(itemRepository.findAllUuids()));
		latest = result;
		if (result.consistent()) {
			log.info("[Store check] {} documents in the store, {} in the database: they match.",
					result.storeDocuments(), result.databaseItems());
		} else {
			result.warnings().forEach(w -> log.warn("[Store check] {}", w));
			result.unknownInStore().stream().limit(LOGGED_PER_KIND)
					.forEach(d -> log.warn("[Store check] In the store, not in the database: {} {} (changed {})",
							d.id(), d.files(), d.lastModified()));
			result.missingInStore().stream().limit(LOGGED_PER_KIND)
					.forEach(id -> log.warn("[Store check] In the database, no file in the store: {}", id));
			result.strayFiles().stream().limit(LOGGED_PER_KIND)
					.forEach(f -> log.warn("[Store check] Stray file in the store: {}", f));
		}
		return result;
	}

	/**
	 * The store keeps each document as {@code <root>/<first 3 characters of id>/<id>.<suffix>}, plus
	 * {@code <id>.txt.enc} and {@code <id>.thumb.jpg.enc} next to it.
	 */
	static Result compare(Path storeRoot, Set<String> databaseIds) {
		Map<String, List<Path>> filesById = new TreeMap<>();
		List<String> stray = new ArrayList<>();
		if (Files.isDirectory(storeRoot)) {
			for (Path entry : list(storeRoot)) {
				String name = entry.getFileName().toString();
				if (ignored(name)) {
					continue;
				}
				if (!Files.isDirectory(entry)) {
					stray.add(name);
					continue;
				}
				for (Path file : list(entry)) {
					String fileName = file.getFileName().toString();
					if (ignored(fileName)) {
						continue;
					}
					String id = idOf(fileName);
					if (Files.isRegularFile(file) && id.startsWith(name)) {
						filesById.computeIfAbsent(id, k -> new ArrayList<>()).add(file);
					} else {
						stray.add(name + "/" + fileName);
					}
				}
			}
		}

		List<UnknownDocument> unknown = new ArrayList<>();
		filesById.forEach((id, files) -> {
			if (!databaseIds.contains(id)) {
				unknown.add(new UnknownDocument(id,
						files.stream().map(f -> f.getFileName().toString()).sorted().toList(),
						files.stream().map(StoreConsistencyService::lastModified).max(Comparator.naturalOrder())
								.orElse(Instant.EPOCH)));
			}
		});
		unknown.sort(Comparator.comparing(UnknownDocument::lastModified).thenComparing(UnknownDocument::id));

		List<String> missing = databaseIds.stream().filter(id -> !filesById.containsKey(id)).sorted().toList();

		return new Result(Instant.now(), databaseIds.size(), filesById.size(), List.copyOf(unknown), missing,
				List.copyOf(stray));
	}

	private static String idOf(String fileName) {
		int dot = fileName.indexOf('.');
		return dot > 0 ? fileName.substring(0, dot) : fileName;
	}

	private static boolean ignored(String name) {
		return name.startsWith(".") || IGNORED_FILES.contains(name.toLowerCase(Locale.ROOT));
	}

	private static List<Path> list(Path dir) {
		try (Stream<Path> entries = Files.list(dir)) {
			return entries.sorted().toList();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot list " + dir, e);
		}
	}

	private static Instant lastModified(Path file) {
		try {
			return Files.getLastModifiedTime(file).toInstant();
		} catch (IOException e) {
			return Instant.EPOCH;
		}
	}

	private Path storeRoot() {
		return appConfig.getServer().getStore().stream()
				.filter(e -> e.getType() == EStorageLocation.LOCAL)
				.findFirst()
				.map(e -> Path.of(e.getPath()))
				.orElseThrow(() -> new IllegalStateException("No LOCAL store configured"));
	}
}
