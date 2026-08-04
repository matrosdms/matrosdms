/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multi-tenant profiles for the desktop builds: named data directories in a small config file in
 * the user's home, so one installed MatrosDMS can serve several tenants (household, invest, …)
 * without a batch file per tenant.
 *
 * <p>{@code ~/.matrosdms/profiles.properties}:
 *
 * <pre>
 * last-active=household
 * profile.household.data-dir=D:/cloud/matrosdms/household
 * profile.invest.data-dir=D:/cloud/matrosdms/invest
 * profile.invest.repository-path=D:/cloud/matrosdms/repository/invest
 * profile.invest.port=9091
 * </pre>
 *
 * <p>The file is optional and never written unless the user switches profiles from the tray. The
 * explicit ways of picking a data directory keep precedence (see {@link #resolveDataDir}), so
 * every existing setup — {@code startMatros.cmd}, Docker, plain {@code java -jar} — behaves
 * exactly as before.
 */
public final class ProfileManager {

	/** System property carrying the active profile name, for the tray tooltip and menu checkmark. */
	public static final String ACTIVE_PROFILE_PROPERTY = "matros.profile.active";

	/** System property carrying the active profile's display label, for the splash and tooltip. */
	public static final String ACTIVE_PROFILE_LABEL_PROPERTY = "matros.profile.label";

	private static final String ENV_DATA_DIR = "MATROS_DATA_DIR";
	private static final String ENV_REPOSITORY_PATH = "MATROS_REPOSITORY_PATH";

	private static final Logger log = LoggerFactory.getLogger(ProfileManager.class);

	public record Profile(String name, String label, String dataDir, String repositoryPath, Integer port) {
	}

	public record Config(String lastActive, List<Profile> profiles) {
		static final Config EMPTY = new Config(null, List.of());
	}

	private ProfileManager() {
	}

	/**
	 * The config is looked up next to the installation first — a {@code profiles.properties} in
	 * the working directory, e.g. the {@code D:\cloud\matrosdms} folder {@code startMatros.cmd}
	 * lives in, so a cloud-synced install carries its tenant list with it — with
	 * {@code ~/.matrosdms/profiles.properties} as the per-user fallback.
	 */
	public static Path configFile() {
		return resolveConfigFile(
				Path.of(System.getProperty("user.dir")),
				Path.of(System.getProperty("user.home")));
	}

	static Path resolveConfigFile(Path workingDir, Path home) {
		Path local = workingDir.resolve("profiles.properties");
		if (Files.isRegularFile(local)) {
			return local;
		}
		return home.resolve(".matrosdms").resolve("profiles.properties");
	}

	/**
	 * Decides the data directory before Spring boots. Precedence, highest first:
	 * <ol>
	 *   <li>a bare program argument — a desktop shortcut on the packaged exe:
	 *       {@code MatrosDMS.exe D:\matrosdms\household},
	 *   <li>the {@code MATROS_DATA_DIR} environment variable or system property — the classic
	 *       {@code startMatros.cmd} way,
	 *   <li>the last active profile from {@link #configFile()}, if that file exists.
	 * </ol>
	 * With none of those the application keeps its {@code ./data} default. Must run before
	 * {@code SpringApplication.run}: the winner is published as system properties, which Spring
	 * resolves ahead of the environment and the yaml defaults.
	 */
	public static void resolveDataDir(String[] args) {
		for (String arg : args) {
			if (!arg.startsWith("-")) {
				System.setProperty(ENV_DATA_DIR, arg);
				return;
			}
		}
		if (System.getProperty(ENV_DATA_DIR) != null) {
			return;
		}
		String env = System.getenv(ENV_DATA_DIR);
		if (env != null && !env.isBlank()) {
			return;
		}

		Config config = load();
		if (config.profiles().isEmpty()) {
			return;
		}
		Profile profile = config.profiles().stream()
				.filter(p -> p.name().equals(config.lastActive()))
				.findFirst()
				.orElse(config.profiles().get(0));

		System.setProperty(ENV_DATA_DIR, profile.dataDir());
		if (profile.repositoryPath() != null) {
			System.setProperty(ENV_REPOSITORY_PATH, profile.repositoryPath());
		}
		if (profile.port() != null) {
			System.setProperty("server.port", String.valueOf(profile.port()));
		}
		System.setProperty(ACTIVE_PROFILE_PROPERTY, profile.name());
		System.setProperty(ACTIVE_PROFILE_LABEL_PROPERTY, profile.label());
		log.info("Using profile '{}' from {} (data dir {}).", profile.name(), configFile(), profile.dataDir());
	}

	/** Reads the config; an absent or unreadable file is an empty config, never an error. */
	public static Config load() {
		return load(configFile());
	}

	static Config load(Path file) {
		if (!Files.isRegularFile(file)) {
			return Config.EMPTY;
		}
		Properties props = new Properties();
		try (InputStream in = Files.newInputStream(file)) {
			props.load(in);
		} catch (IOException e) {
			log.warn("Cannot read {}: {}", file, e.getMessage());
			return Config.EMPTY;
		}

		// TreeMap so the tray menu lists profiles in a stable alphabetical order.
		TreeMap<String, Profile> profiles = new TreeMap<>();
		for (String key : props.stringPropertyNames()) {
			if (!key.startsWith("profile.") || !key.endsWith(".data-dir")) {
				continue;
			}
			String name = key.substring("profile.".length(), key.length() - ".data-dir".length());
			String dataDir = props.getProperty(key).trim();
			if (name.isEmpty() || dataDir.isEmpty()) {
				continue;
			}
			String label = trimToNull(props.getProperty("profile." + name + ".label"));
			profiles.put(name, new Profile(name,
					label != null ? label : prettyLabel(name),
					dataDir,
					trimToNull(props.getProperty("profile." + name + ".repository-path")),
					parsePort(props.getProperty("profile." + name + ".port"), name)));
		}
		return new Config(trimToNull(props.getProperty("last-active")), assignPorts(profiles.values()));
	}

	/**
	 * Every profile gets its own port: tenants must not share a browser origin, or the JWT and
	 * localStorage of one tenant leak into the next after a switch — and distinct ports also allow
	 * running profiles side by side. An explicit {@code profile.<name>.port} is honored (pin it if
	 * you bookmark the URL); the rest are filled with the next free port from 9090 upward, in
	 * alphabetical order.
	 */
	private static List<Profile> assignPorts(Iterable<Profile> sorted) {
		Set<Integer> taken = new HashSet<>();
		for (Profile p : sorted) {
			if (p.port() != null) {
				taken.add(p.port());
			}
		}
		List<Profile> result = new ArrayList<>();
		int candidate = 9090;
		for (Profile p : sorted) {
			if (p.port() != null) {
				result.add(p);
				continue;
			}
			while (taken.contains(candidate)) {
				candidate++;
			}
			taken.add(candidate);
			result.add(new Profile(p.name(), p.label(), p.dataDir(), p.repositoryPath(), candidate));
		}
		return List.copyOf(result);
	}

	/**
	 * Turns a config key like {@code demo-a} or {@code schwehla_household} into a display label
	 * ("Demo A", "Schwehla Household"). An explicit {@code profile.<name>.label} wins.
	 */
	static String prettyLabel(String name) {
		StringBuilder label = new StringBuilder();
		for (String part : name.split("[-_\\s]+")) {
			if (part.isEmpty()) {
				continue;
			}
			if (label.length() > 0) {
				label.append(' ');
			}
			label.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return label.length() == 0 ? name : label.toString();
	}

	/**
	 * Records the profile the next start should use. Edits the file line-based instead of
	 * {@link Properties#store} so the user's comments and ordering survive.
	 */
	public static void saveLastActive(String name) throws IOException {
		saveLastActive(configFile(), name);
	}

	static void saveLastActive(Path file, String name) throws IOException {
		List<String> lines = Files.isRegularFile(file)
				? new ArrayList<>(Files.readAllLines(file))
				: new ArrayList<>();
		boolean replaced = false;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).strip().matches("last-active\\s*[=:].*")) {
				lines.set(i, "last-active=" + name);
				replaced = true;
				break;
			}
		}
		if (!replaced) {
			lines.add(0, "last-active=" + name);
		}
		Files.createDirectories(file.getParent());
		Files.write(file, lines);
	}

	/**
	 * Starts a fresh MatrosDMS that resolves its profile from the home config again. The
	 * {@code MATROS_*} variables are stripped from the child environment — they may pin the data
	 * directory of the <em>current</em> instance, and the point of relaunching is to let the new
	 * last-active profile win. Call only after the Spring context is closed, so the port is free.
	 *
	 * @param extraArgs program arguments for the successor, e.g. {@code --app.start-browser=true}
	 *                  so a tray-initiated switch surfaces the new tenant in the browser
	 */
	public static void relaunch(String... extraArgs) throws IOException {
		List<String> cmd = new ArrayList<>(relaunchCommand());
		cmd.addAll(List.of(extraArgs));
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.environment().remove(ENV_DATA_DIR);
		pb.environment().remove(ENV_REPOSITORY_PATH);
		pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		pb.redirectError(ProcessBuilder.Redirect.DISCARD);
		pb.start();
	}

	private static List<String> relaunchCommand() throws IOException {
		String command = ProcessHandle.current().info().command().orElse("");
		Path executablePath = command.isEmpty() ? null : Path.of(command).getFileName();
		String executable = executablePath == null ? "" : executablePath.toString().toLowerCase(Locale.ROOT);

		// jpackage app-image: the native launcher carries its own JVM options (.cfg), so
		// relaunching it plain is the whole job. Program arguments are dropped on purpose in both
		// branches — they may carry the old data directory.
		if (!executable.isEmpty() && !executable.startsWith("java")) {
			return List.of(command);
		}

		// Plain java (dev run, fat jar): rebuild the command line from the running JVM.
		List<String> cmd = new ArrayList<>();
		cmd.add(command.isEmpty()
				? Path.of(System.getProperty("java.home"), "bin", "java").toString()
				: command);
		for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			// Debug agents hold server sockets the child could not bind again.
			if (!jvmArg.startsWith("-agentlib:") && !jvmArg.startsWith("-javaagent:")) {
				cmd.add(jvmArg);
			}
		}
		String sunJavaCommand = System.getProperty("sun.java.command", "").strip();
		if (sunJavaCommand.isEmpty()) {
			throw new IOException("cannot reconstruct the launch command (sun.java.command is empty)");
		}
		String jarOrMain = sunJavaCommand.split("\\s+")[0];
		if (jarOrMain.toLowerCase(Locale.ROOT).endsWith(".jar")) {
			cmd.add("-jar");
			cmd.add(jarOrMain);
		} else {
			cmd.add("-cp");
			cmd.add(System.getProperty("java.class.path"));
			cmd.add(jarOrMain);
		}
		return cmd;
	}

	private static Integer parsePort(String value, String profileName) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			log.warn("Profile '{}': ignoring invalid port '{}'.", profileName, value.trim());
			return null;
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
