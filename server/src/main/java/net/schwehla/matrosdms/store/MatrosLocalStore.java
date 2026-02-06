/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.security.Security;
import java.util.stream.Stream;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.PostConstruct;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.config.model.AppServerSpringConfig.StoreElement;
import net.schwehla.matrosdms.domain.content.MDocumentStream;
import net.schwehla.matrosdms.domain.storage.EStorageLocation;
import net.schwehla.matrosdms.exception.EntityNotFoundException;
import net.schwehla.matrosdms.exception.MatrosServiceException;

@Component
@Primary
public class MatrosLocalStore implements IMatrosStore {

	private static final Logger log = LoggerFactory.getLogger(MatrosLocalStore.class);

	// UPGRADE: GCM Mode for Integrity
	private static final String ALGO_AES = "AES/GCM/NoPadding";
	private static final int IV_LENGTH = 12; // GCM Standard
	private static final int TAG_LENGTH_BIT = 128;

	@Autowired
	AppServerSpringConfig appServerSpringConfig;
	@Autowired
	FileUtils fileUtils;
	@Autowired
	StoragePathStrategy pathStrategy;

	private Path rootFolder;
	private boolean useEncryption;
	private byte[] keyBytes;

	@PostConstruct
	public void init() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		StoreElement store = appServerSpringConfig.getServer().getStore().stream()
				.filter(e -> e.getType() == EStorageLocation.LOCAL)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("No LOCAL store configured"));

		this.rootFolder = Path.of(store.getPath());
		this.useEncryption = store.getCryptor() != null && store.getCryptor().contains("AES");

		if (useEncryption) {
			log.info("🔐 Store Encryption ACTIVE (AES-GCM).");
			byte[] salt = store.getSalt().getBytes(StandardCharsets.UTF_8);
			Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
					.withVersion(Argon2Parameters.ARGON2_VERSION_13)
					.withIterations(3)
					.withMemoryAsKB(65536)
					.withParallelism(1)
					.withSalt(salt);
			Argon2BytesGenerator gen = new Argon2BytesGenerator();
			gen.init(builder.build());
			this.keyBytes = new byte[32];
			gen.generateBytes(store.getPassword().toCharArray(), this.keyBytes);
		}
		StoreContext.init(rootFolder, keyBytes, useEncryption);
	}

	@Override
	public StoreResult persist(Path sourceFile, Path textFile, String uuid, String originalFilename) {
		StoreResult result = new StoreResult();
		try {
			String extension = getExtension(sourceFile);
			String encSuffix = useEncryption ? ".enc" : "";
			Path targetFile = pathStrategy.getPhysicalPath(rootFolder, uuid, extension + encSuffix);
			Path sidecarText = pathStrategy.getPhysicalPath(rootFolder, uuid, ".txt" + encSuffix);

			Files.createDirectories(targetFile.getParent());

			if (useEncryption) {
				encryptPath(sourceFile, targetFile);
				result.setCryptSettings("AES-GCM-256");
			} else {
				Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
				result.setCryptSettings("NONE");
			}

			if (textFile != null && Files.exists(textFile)) {
				if (useEncryption)
					encryptPath(textFile, sidecarText);
				else
					Files.copy(textFile, sidecarText, StandardCopyOption.REPLACE_EXISTING);
			}
			result.setSHA256(fileUtils.getSHA256(targetFile));
		} catch (Exception e) {
			throw new MatrosServiceException("Storage failed: " + e.getMessage(), e);
		}
		return result;
	}

	@Override
	public MDocumentStream loadStream(String uuid) {
		InputStream is = null;
		try {
			Path file = findFile(uuid);
			long size = Files.size(file);
			is = new BufferedInputStream(Files.newInputStream(file));

			if (useEncryption && file.toString().endsWith(".enc")) {
				byte[] iv = new byte[IV_LENGTH];
				if (is.read(iv) != IV_LENGTH)
					throw new MatrosServiceException("Corrupt IV");

				Cipher cipher = Cipher.getInstance(ALGO_AES);
				GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
				cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);

				is = new CipherInputStream(is, cipher);

				// FIX: Calculate precise content length for GCM (File - IV - Tag)
				// Tag is 16 bytes (128 bits). IV is 12 bytes.
				long overhead = IV_LENGTH + (TAG_LENGTH_BIT / 8);
				if (size >= overhead) {
					size = size - overhead;
				} else {
					size = -1; // Should not happen unless file is corrupt
				}
			}

			MDocumentStream ms = new MDocumentStream(is, size);
			String fileName = file.getFileName().toString().replace(".enc", "");
			ms.setFilename(fileName);
			return ms;
		} catch (Exception e) {
			if (is != null)
				try {
					is.close();
				} catch (IOException ex) {
				}
			throw new MatrosServiceException("Load failed: " + e.getMessage(), e);
		}
	}

	// --- TRASH IMPLEMENTATION ---
	@Override
	public void moveToTrash(String uuid) {
		try {
			Path trashRoot = Path.of(appServerSpringConfig.getServer().getTrash().getPath());
			if (!Files.exists(trashRoot))
				Files.createDirectories(trashRoot);
			Path folder = pathStrategy.getPhysicalPath(rootFolder, uuid, "").getParent();
			if (Files.exists(folder)) {
				try (Stream<Path> s = Files.list(folder)) {
					s.filter(p -> p.getFileName().toString().startsWith(uuid))
							.forEach(p -> {
								try {
									Files.move(p, trashRoot.resolve(System.currentTimeMillis() + "_" + p.getFileName()),
											StandardCopyOption.REPLACE_EXISTING);
								} catch (IOException e) {
									log.error("Trash error", e);
								}
							});
				}
			}
		} catch (Exception e) {
			log.error("Trash failed for " + uuid, e);
		}
	}

	// --- THUMBNAIL IMPLEMENTATION ---
	@Override
	public boolean hasThumbnail(String uuid) {
		String suffix = ".thumb.jpg" + (useEncryption ? ".enc" : "");
		Path p = pathStrategy.getPhysicalPath(rootFolder, uuid, suffix);
		return Files.exists(p);
	}

	@Override
	public void storeThumbnail(String uuid, byte[] data) {
		try {
			String suffix = ".thumb.jpg" + (useEncryption ? ".enc" : "");
			Path target = pathStrategy.getPhysicalPath(rootFolder, uuid, suffix);

			if (useEncryption) {
				Path temp = Files.createTempFile("thumb", ".tmp");
				Files.write(temp, data);
				encryptPath(temp, target);
				Files.delete(temp);
			} else {
				Files.write(target, data);
			}
		} catch (Exception e) {
			log.error("Failed to store thumbnail", e);
		}
	}

	@Override
	public byte[] loadThumbnail(String uuid) {
		try {
			String suffix = ".thumb.jpg" + (useEncryption ? ".enc" : "");
			Path target = pathStrategy.getPhysicalPath(rootFolder, uuid, suffix);
			if (!Files.exists(target))
				return null;

			if (useEncryption) {
				try (InputStream is = new BufferedInputStream(Files.newInputStream(target))) {
					byte[] iv = new byte[IV_LENGTH];
					is.read(iv);
					Cipher cipher = Cipher.getInstance(ALGO_AES);
					cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
							new GCMParameterSpec(TAG_LENGTH_BIT, iv));
					try (CipherInputStream cis = new CipherInputStream(is, cipher)) {
						return cis.readAllBytes();
					}
				}
			} else {
				return Files.readAllBytes(target);
			}
		} catch (Exception e) {
			log.error("Failed to load thumbnail", e);
			return null;
		}
	}

	private void encryptPath(Path source, Path target) throws Exception {
		byte[] iv = new byte[IV_LENGTH];
		new SecureRandom().nextBytes(iv);
		Cipher cipher = Cipher.getInstance(ALGO_AES);
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH_BIT, iv));

		try (OutputStream fos = new BufferedOutputStream(Files.newOutputStream(target))) {
			fos.write(iv);
			try (OutputStream cos = new CipherOutputStream(fos, cipher)) {
				Files.copy(source, cos);
			}
		}
	}

	private Path findFile(String uuid) throws IOException {
		Path folder = pathStrategy.getPhysicalPath(rootFolder, uuid, "").getParent();
		try (Stream<Path> s = Files.list(folder)) {
			return s.filter(p -> p.getFileName().toString().startsWith(uuid))
					.filter(p -> !p.toString().contains(".txt") && !p.toString().contains(".thumb"))
					.findFirst()
					.orElseThrow(() -> new EntityNotFoundException("File not found"));
		}
	}

	private String getExtension(Path path) {
		String name = path.getFileName().toString();
		int i = name.lastIndexOf('.');
		return i > 0 ? name.substring(i) : ".bin";
	}
}