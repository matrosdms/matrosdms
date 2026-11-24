/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.command;

import java.io.Console;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.cli.rekey.StoreRekeyer;
import net.schwehla.matrosdms.store.encryption.StoreCrypto;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code matros rekey}: gives the document store a new password. Passwords are only read from the
 * console (or, without one, from MATROS_REKEY_CURRENT_PASSWORD / MATROS_REKEY_NEW_PASSWORD), never
 * from the command line, so they do not end up in the shell history.
 */
@Component
@Command(name = "rekey", description = {
		"Re-encrypts the document store with a new password and salt.",
		"Stop MatrosDMS first. Without --apply every file is only checked with the current password.",
		"Afterwards start MatrosDMS with MATROS_STORE_PASSWORD and MATROS_STORE_SALT set to the new values." })
public class RekeyCommand implements Callable<Integer> {

	static final int MIN_PASSWORD_LENGTH = 12;

	@Parameters(index = "0", description = "Repository folder, the one that contains documents/")
	Path repository;

	@Option(names = "--apply", description = "Re-encrypt. Without it nothing is changed.")
	boolean apply;

	@Option(names = "--backup-dir", description = "Where the old files go (required with --apply). "
			+ "Outside the repository and outside any cloud folder: they open with the old password.")
	Path backupDir;

	@Option(names = "--current-default", description = "The store still uses the shipped default password and salt.")
	boolean currentDefault;

	@Option(names = "--current-salt", description = "Current salt (default: the shipped one).")
	String currentSalt = StoreCrypto.DEFAULT_SALT;

	@Option(names = "--new-salt", description = "New salt (default: a new random one). "
			+ "Repositories started with the same MATROS_STORE_SALT need the same salt.")
	String newSalt;

	@Override
	public Integer call() {
		try {
			if (apply && backupDir == null) {
				System.err.println("[ERROR] --apply needs --backup-dir");
				return 2;
			}
			String currentPassword = currentDefault ? StoreCrypto.DEFAULT_PASSWORD
					: readPassword("Current store password: ", "MATROS_REKEY_CURRENT_PASSWORD");
			String salt = currentDefault ? StoreCrypto.DEFAULT_SALT : currentSalt;
			System.err.println("Deriving the current key ...");
			byte[] currentKey = StoreCrypto.deriveKey(currentPassword, salt);

			if (!apply) {
				StoreRekeyer.Report r = new StoreRekeyer(repository, currentKey, null, System.err::println).check();
				System.err.println((r.ok() ? "[OK] " : "[ERROR] ") + r.withCurrentKey() + " files open with the current password"
						+ (r.ok() ? "." : ", " + r.unreadable().size() + " do not:"));
				r.unreadable().stream().limit(20).forEach(f -> System.err.println("   " + f));
				System.err.println(r.ok() ? "   Nothing was changed. Run again with --apply --backup-dir <folder>." : "");
				return r.ok() ? 0 : 1;
			}

			String newPassword = readNewPassword(currentPassword);
			String targetSalt = newSalt != null ? newSalt : randomSalt();
			if (targetSalt.equals(salt) && newPassword.equals(currentPassword)) {
				System.err.println("[ERROR] New password and salt are the current ones");
				return 2;
			}
			System.err.println("Deriving the new key ...");
			byte[] newKey = StoreCrypto.deriveKey(newPassword, targetSalt);

			System.err.println("Re-encrypting " + repository + " ...");
			StoreRekeyer.Report r = new StoreRekeyer(repository, currentKey, newKey, System.err::println).apply(backupDir);
			if (!r.ok()) {
				System.err.println("[ERROR] " + r.unreadable().size()
						+ " files open with neither password. Nothing was changed:");
				r.unreadable().stream().limit(20).forEach(f -> System.err.println("   " + f));
				return 1;
			}
			System.err.println("[OK] " + r.rekeyed() + " files re-encrypted and verified"
					+ (r.withNewKey() > 0 ? ", " + r.withNewKey() + " already had the new password" : "") + ".");
			System.err.println();
			System.err.println("Next steps:");
			System.err.println("  1. Set the Windows environment variables (Settings > System > About >");
			System.err.println("     Advanced system settings > Environment Variables, user variables):");
			System.err.println("       MATROS_STORE_PASSWORD = <the new password>");
			System.err.println("       MATROS_STORE_SALT     = " + targetSalt);
			System.err.println("  2. Start MatrosDMS and run the Integrity Check (Settings > System Jobs).");
			System.err.println("  3. Then delete " + backupDir.toAbsolutePath() + " (it opens with the old password)");
			System.err.println("     and, for a repository in a cloud folder, empty the cloud's trash.");
			System.err.println("  Keep the password and the salt in your password manager: without them the");
			System.err.println("  documents cannot be opened by anyone.");
			System.out.println(targetSalt);
			return 0;
		} catch (IllegalArgumentException | IllegalStateException e) {
			System.err.println("[ERROR] " + e.getMessage());
			return 2;
		} catch (Exception e) {
			System.err.println("[ERROR] " + e.getMessage());
			return 1;
		}
	}

	private String readNewPassword(String currentPassword) {
		String first = readPassword("New store password (at least " + MIN_PASSWORD_LENGTH + " characters): ",
				"MATROS_REKEY_NEW_PASSWORD");
		if (System.console() != null) {
			String again = readPassword("Repeat the new password: ", null);
			if (!first.equals(again))
				throw new IllegalArgumentException("The two new passwords differ");
		}
		if (first.length() < MIN_PASSWORD_LENGTH)
			throw new IllegalArgumentException("The new password needs at least " + MIN_PASSWORD_LENGTH + " characters");
		if (first.equals(StoreCrypto.DEFAULT_PASSWORD))
			throw new IllegalArgumentException("The new password is the public default");
		return first;
	}

	/** From the console without echo; without a console from the environment variable. */
	static String readPassword(String prompt, String envVar) {
		Console console = System.console();
		if (console != null) {
			char[] chars = console.readPassword(prompt);
			if (chars == null)
				throw new IllegalArgumentException("No password entered");
			String s = new String(chars);
			Arrays.fill(chars, '\0');
			return s;
		}
		String fromEnv = envVar == null ? null : System.getenv(envVar);
		if (fromEnv == null || fromEnv.isEmpty())
			throw new IllegalArgumentException("No console to ask for the password; run in a terminal"
					+ (envVar == null ? "" : " or set " + envVar));
		return fromEnv;
	}

	static String randomSalt() {
		byte[] b = new byte[24];
		new SecureRandom().nextBytes(b);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
	}
}
