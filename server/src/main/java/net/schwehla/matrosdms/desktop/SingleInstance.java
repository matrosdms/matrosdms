/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import java.awt.Desktop;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-boot single-instance guard: when the port this start would bind is already served by a
 * MatrosDMS, the double-clicked launcher must not boot a second server — the user just wants the
 * app, so the existing instance's URL is opened in the browser and this process quits.
 *
 * <p>The guard is deliberately keyed on the <em>port</em>, not on "any MatrosDMS process": every
 * profile owns its own port (see {@link ProfileManager}), so household, invest and friends still
 * run side by side — only a second start of the <em>same</em> profile is folded into the running
 * one.
 *
 * <p>Identity is verified, not assumed: the probe requires {@code /actuator/info} to answer with
 * the MatrosDMS build info. A foreign application squatting on the port fails the probe, and the
 * start proceeds into Spring's own port-in-use error as before. Runs before the splash, before
 * Spring, so a folded start costs one localhost round-trip instead of a full boot-and-crash.
 */
public final class SingleInstance {

	private static final Logger log = LoggerFactory.getLogger(SingleInstance.class);

	private SingleInstance() {
	}

	/**
	 * Returns {@code true} when a MatrosDMS already serves this start's port — the browser has then
	 * been pointed at it and the caller should exit instead of booting. Any probe failure
	 * (connection refused, timeout, foreign server) returns {@code false}: booting normally is
	 * always the safe answer.
	 */
	public static boolean handleExistingInstance(String[] args) {
		int port = resolvePort(args);
		if (!isMatrosOnPort(port)) {
			return false;
		}
		String url = "http://localhost:" + port;
		log.info("MatrosDMS is already running on {} — opening the browser instead of a second server.", url);
		openBrowser(url);
		return true;
	}

	/**
	 * The port this start would bind, mirroring Spring's precedence for the sources that exist
	 * before boot: a {@code --server.port} program argument, the {@code server.port} system
	 * property (set by {@link ProfileManager#resolveDataDir}), the {@code SERVER_PORT} environment
	 * variable, then the yaml default 9090.
	 */
	static int resolvePort(String[] args) {
		for (String arg : args) {
			if (arg.startsWith("--server.port=")) {
				Integer port = parsePort(arg.substring("--server.port=".length()));
				if (port != null) {
					return port;
				}
			}
		}
		Integer port = parsePort(System.getProperty("server.port"));
		if (port == null) {
			port = parsePort(System.getenv("SERVER_PORT"));
		}
		return port == null ? 9090 : port;
	}

	static boolean isMatrosOnPort(int port) {
		return isMatros(URI.create("http://localhost:" + port + "/actuator/info"));
	}

	static boolean isMatros(URI infoEndpoint) {
		try (HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(500))
				.build()) {
			HttpRequest request = HttpRequest.newBuilder(infoEndpoint)
					.timeout(Duration.ofSeconds(2))
					.GET()
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			// The build-info group is net.schwehla.matrosdms; a foreign app's info page won't have it.
			return response.statusCode() == 200
					&& response.body().toLowerCase(Locale.ROOT).contains("matrosdms");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	private static void openBrowser(String url) {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(URI.create(url));
			} else {
				log.info("No desktop browser available — open {} yourself.", url);
			}
		} catch (Exception e) {
			log.warn("Could not open a browser for {}: {}", url, e.getMessage());
		}
	}

	private static Integer parsePort(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
