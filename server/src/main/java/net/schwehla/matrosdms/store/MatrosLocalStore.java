/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.Security;
import java.util.stream.Stream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
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
	private static final String ALGO_AES = "AES/CTR/NoPadding";

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
		// Register Bouncy Castle for Argon2
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		StoreElement store = appServerSpringConfig.getServer().getStore().stream()
				.filter(e -> e.getType() == EStorageLocation.LOCAL)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("No LOCAL store configured"));

		this.rootFolder = Path.of(store.getPath());
		this.useEncryption = "AES_CTR".equalsIgnoreCase(store.getCryptor());

		if (useEncryption) {
			log.info("🔐 Store Encryption ENABLED (AES-CTR). Deriving key with Argon2id...");

			String pass = store.getPassword();
			String saltStr = store.getSalt();

			if (pass == null || pass.length() < 8)
				throw new RuntimeException("Store password weak");
			if (saltStr == null || saltStr.isEmpty())
				throw new RuntimeException("Salt missing");

			byte[] salt = saltStr.getBytes(StandardCharsets.UTF_8);

			Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
					.withVersion(Argon2Parameters.ARGON2_VERSION_13)
					.withIterations(3)
					.withMemoryAsKB(65536)
					.withParallelism(1)
					.withSalt(salt);

			Argon2BytesGenerator gen = new Argon2BytesGenerator();
			gen.init(builder.build());

			this.keyBytes = new byte[32];
			gen.generateBytes(pass.toCharArray(), this.keyBytes);
		}

		StoreContext.init(rootFolder, keyBytes, useEncryption);
	}

	@Override
	public StoreResult persist(Path sourceFile, Path textFile, String uuid, String originalFilename) {
		StoreResult result = new StoreResult();
		try {
			String extension = getExtension(sourceFile);
			Path targetFile = pathStrategy.getPhysicalPath(rootFolder, uuid, extension + (useEncryption ? ".enc" : ""));
			Path sidecarText = pathStrategy.getPhysicalPath(rootFolder, uuid, ".txt" + (useEncryption ? ".enc" : ""));

			Files.createDirectories(targetFile.getParent());

			if (useEncryption) {
				encryptPath(sourceFile, targetFile);
				result.setCryptSettings("AES-CTR-256");
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
			Path folder = pathStrategy.getPhysicalPath(rootFolder, uuid, "").getParent();

			Path foundFile;
			try (Stream<Path> pathStream = Files.list(folder)) {
				foundFile = pathStream
						.filter(p -> p.getFileName().toString().startsWith(uuid))
						.filter(p -> !p.toString().contains(".txt"))
						.findFirst()
						.orElseThrow(() -> new EntityNotFoundException("File not found: " + uuid));
			}

			long size = Files.size(foundFile);
			is = new BufferedInputStream(Files.newInputStream(foundFile));

			if (useEncryption && foundFile.toString().endsWith(".enc")) {
				byte[] iv = new byte[16];
				int bytesRead = is.read(iv);
				if (bytesRead != 16)
					throw new MatrosServiceException("Corrupt IV in encrypted file");

				Cipher cipher = Cipher.getInstance(ALGO_AES);
				cipher.init(
						Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(iv));

				is = new CipherInputStream(is, cipher);
				size -= 16;
			}

			MDocumentStream ms = new MDocumentStream(is, size);
			String fileName = foundFile.getFileName().toString().replace(".enc", "");
			ms.setFilename(fileName);

			// FIX: Do NOT force MIME type here.
			// It will be set by the Service based on DB metadata.
			ms.setContentType(null);

			return ms;

		} catch (Exception e) {
			if (is != null)
				try {
					is.close();
				} catch (IOException ignored) {
				}
			if (e instanceof EntityNotFoundException)
				throw (EntityNotFoundException) e;
			throw new MatrosServiceException("Load failed: " + e.getMessage(), e);
		}
	}

	private void encryptPath(Path source, Path target) throws Exception {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);

		Cipher cipher = Cipher.getInstance(ALGO_AES);
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(iv));

		try (OutputStream fos = new BufferedOutputStream(Files.newOutputStream(target))) {
			fos.write(iv);
			try (OutputStream cos = new CipherOutputStream(fos, cipher)) {
				Files.copy(source, cos);
			}
		}
	}

	private String getExtension(Path path) {
		String name = path.getFileName().toString();
		int i = name.lastIndexOf('.');
		return i > 0 ? name.substring(i) : ".bin";
	}
}
