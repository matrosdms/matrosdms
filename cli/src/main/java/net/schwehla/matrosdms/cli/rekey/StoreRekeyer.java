/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.rekey;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import net.schwehla.matrosdms.store.encryption.StoreCrypto;

/**
 * Re-encrypts every {@code .enc} file of a MatrosDMS repository with a new key.
 *
 * <p>
 * The repository is not touched until every file has been re-encrypted into a staging folder and
 * each staged file has been decrypted again with the new key to the same SHA-256 as before. Then
 * each original is moved to the backup folder and its replacement moved into place. A file that
 * already opens with the new key (a run that was interrupted during the swap) is left as it is, so
 * running again finishes the job. A file neither key opens stops everything before any change.
 */
public final class StoreRekeyer {

	/** Inside the repository, so the final moves are renames on the same volume. */
	static final String STAGING = ".rekey-staging";
	static final String MANIFEST = "rekey-manifest.txt";

	/** What a check or a run found. */
	public record Report(int withCurrentKey, int withNewKey, List<String> unreadable, int rekeyed) {
		public boolean ok() {
			return unreadable.isEmpty();
		}
	}

	private final Path repository;
	private final byte[] currentKey;
	private final byte[] newKey;
	private final Consumer<String> log;

	/**
	 * @param newKey null for a check only
	 */
	public StoreRekeyer(Path repository, byte[] currentKey, byte[] newKey, Consumer<String> log) {
		this.repository = repository.toAbsolutePath().normalize();
		this.currentKey = currentKey;
		this.newKey = newKey;
		this.log = log;
	}

	/** Opens every file with the current key (or, if given, the new one). Changes nothing. */
	public Report check() throws IOException {
		int current = 0, renewed = 0;
		List<String> unreadable = new ArrayList<>();
		for (Path file : storeFiles()) {
			if (StoreCrypto.opens(file, currentKey))
				current++;
			else if (newKey != null && StoreCrypto.opens(file, newKey))
				renewed++;
			else
				unreadable.add(rel(file));
		}
		return new Report(current, renewed, unreadable, 0);
	}

	/**
	 * Re-encrypts with the new key; the originals go to {@code backupDir}, which must lie outside the
	 * repository and be empty. Throws, leaving the repository as it was, when a file cannot be read or
	 * the repository changes during the run.
	 */
	public Report apply(Path backupDir) throws IOException {
		if (newKey == null)
			throw new IllegalStateException("No new key");
		Path backup = backupDir.toAbsolutePath().normalize();
		if (backup.startsWith(repository))
			throw new IllegalArgumentException("The backup folder must lie outside the repository: " + backup);
		if (Files.exists(backup) && !isEmptyDir(backup))
			throw new IllegalArgumentException("The backup folder must be new or empty: " + backup);

		Path staging = repository.resolve(STAGING);
		deleteTree(staging); // leftovers of a run that stopped before the swap

		List<Path> files = storeFiles();
		Map<Path, String> snapshot = snapshot(files);
		Map<Path, String> plainHash = new LinkedHashMap<>();
		int renewed = 0;
		List<String> unreadable = new ArrayList<>();

		// 1. re-encrypt into the staging folder
		int done = 0;
		for (Path file : files) {
			byte[] encrypted = Files.readAllBytes(file);
			byte[] plain;
			try {
				plain = StoreCrypto.decrypt(encrypted, currentKey);
			} catch (GeneralSecurityException e) {
				if (StoreCrypto.opens(file, newKey))
					renewed++;
				else
					unreadable.add(rel(file));
				continue;
			}
			Path staged = staging.resolve(repository.relativize(file));
			Files.createDirectories(staged.getParent());
			try {
				Files.write(staged, StoreCrypto.encrypt(plain, newKey));
			} catch (GeneralSecurityException e) {
				throw new IOException("Encryption failed for " + rel(file), e);
			}
			plainHash.put(file, sha256(plain));
			if (++done % 100 == 0)
				log.accept("  " + done + " / " + files.size() + " re-encrypted");
		}
		if (!unreadable.isEmpty()) {
			deleteTree(staging);
			return new Report(0, renewed, unreadable, 0);
		}

		// 2. every staged file must decrypt with the new key to what the original held
		for (Map.Entry<Path, String> e : plainHash.entrySet()) {
			Path staged = staging.resolve(repository.relativize(e.getKey()));
			String again;
			try {
				again = sha256(StoreCrypto.decrypt(Files.readAllBytes(staged), newKey));
			} catch (GeneralSecurityException ex) {
				again = "unreadable";
			}
			if (!again.equals(e.getValue())) {
				deleteTree(staging);
				throw new IOException("Verification failed for " + rel(e.getKey()) + "; nothing was changed");
			}
		}

		// 3. nobody may have written meanwhile (MatrosDMS still running?)
		if (!snapshot(storeFiles()).equals(snapshot)) {
			deleteTree(staging);
			throw new IOException("The repository changed during the run. Is MatrosDMS still running? "
					+ "Nothing was changed.");
		}

		if (plainHash.isEmpty()) { // everything had the new key already
			deleteTree(staging);
			return new Report(0, renewed, unreadable, 0);
		}

		// 4. swap: original to the backup, replacement into place
		Files.createDirectories(backup);
		List<String> manifest = new ArrayList<>();
		manifest.add("# matros rekey: originals of " + repository + " (still readable with the OLD key)");
		manifest.add("# relative path<TAB>SHA-256 of the decrypted content");
		plainHash.forEach((file, hash) -> manifest.add(rel(file) + "\t" + hash));
		Files.write(backup.resolve(MANIFEST), manifest, StandardCharsets.UTF_8);

		int swapped = 0;
		for (Path file : plainHash.keySet()) {
			Path rel = repository.relativize(file);
			Path old = backup.resolve(rel);
			Files.createDirectories(old.getParent());
			Files.move(file, old);
			try {
				moveIntoPlace(staging.resolve(rel), file);
			} catch (IOException e) {
				Files.move(old, file); // never leave a document missing; run again to finish
				throw new IOException("Could not put the re-encrypted " + rel(file) + " in place after "
						+ swapped + " files. Those have the new key, the rest still the old one; "
						+ "run again with a new backup folder to finish.", e);
			}
			swapped++;
		}
		deleteTree(staging);
		return new Report(0, renewed, unreadable, swapped);
	}

	/** Every .enc file of the repository, except the staging folder. */
	List<Path> storeFiles() throws IOException {
		if (!Files.isDirectory(repository.resolve("documents")))
			throw new IllegalArgumentException("Not a MatrosDMS repository (no documents folder): " + repository);
		Path staging = repository.resolve(STAGING);
		try (Stream<Path> all = Files.walk(repository)) {
			return all.filter(Files::isRegularFile)
					.filter(p -> !p.startsWith(staging))
					.filter(p -> p.getFileName().toString().endsWith(".enc"))
					.sorted()
					.toList();
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	private Map<Path, String> snapshot(List<Path> files) throws IOException {
		Map<Path, String> m = new LinkedHashMap<>();
		for (Path f : files)
			m.put(f, Files.size(f) + "@" + Files.getLastModifiedTime(f).toMillis());
		return m;
	}

	private String rel(Path file) {
		return repository.relativize(file).toString().replace('\\', '/');
	}

	private static void moveIntoPlace(Path staged, Path target) throws IOException {
		try {
			Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(staged, target);
		}
	}

	private static String sha256(byte[] data) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean isEmptyDir(Path dir) throws IOException {
		if (!Files.isDirectory(dir))
			return false;
		try (Stream<Path> s = Files.list(dir)) {
			return s.findAny().isEmpty();
		}
	}

	static void deleteTree(Path dir) throws IOException {
		if (!Files.exists(dir))
			return;
		Files.walkFileTree(dir, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
				Files.delete(d);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
