/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Puts MatrosDMS in the system tray on the desktop builds, so a server with no console window still
 * has a way to be opened, inspected and shut down.
 *
 * <p>Three gates, in order, decide whether a tray appears at all:
 * <ol>
 *   <li>the {@code app.system-tray} property,
 *   <li>{@link GraphicsEnvironment#isHeadless()} — Spring Boot forces {@code java.awt.headless=true}
 *       during startup, so only a launcher that passes {@code -Djava.awt.headless=false} (which is
 *       to say: the jpackage app-images, not {@code java -jar} and not Docker) gets this far,
 *   <li>{@link SystemTray#isSupported()} — false on GNOME and on any display-less session.
 * </ol>
 * Everything else — the jar, the container, a server over SSH — runs exactly as before and simply
 * has no tray.
 */
@Component
public class TrayLauncher {

	private static final Logger log = LoggerFactory.getLogger(TrayLauncher.class);

	private final ConfigurableApplicationContext context;

	@Value("${app.system-tray:true}")
	private boolean enableTray;

	@Value("${app.base-path:./data}")
	private String basePath;

	public TrayLauncher(ConfigurableApplicationContext context) {
		this.context = context;
	}

	private TrayIcon trayIcon;

	@EventListener(ApplicationReadyEvent.class)
	public void onReady(ApplicationReadyEvent event) {
		try {
			// Whatever happens next, the splash must go away.
			DesktopSplash.close();

			if (!enableTray) {
				return;
			}
			if (isContainer()) {
				log.debug("System tray skipped (container).");
				return;
			}
			if (GraphicsEnvironment.isHeadless()) {
				log.info("System tray skipped (headless JVM). Start with -Djava.awt.headless=false to enable it.");
				return;
			}
			if (!SystemTray.isSupported()) {
				log.info("System tray is not supported on this desktop — open the URL manually.");
				return;
			}
			installTray("http://localhost:" + resolvePort(event.getApplicationContext()));
			log.info("System tray ready.");
		} catch (Throwable t) {
			// A broken display throws AWTError, an Error — and an ApplicationReadyEvent listener that
			// throws would take the whole server down over a decoration.
			log.warn("Desktop tray unavailable ({}); MatrosDMS is running normally.", t.toString());
		}
	}

	private void installTray(String url) throws Exception {
		SystemTray tray = SystemTray.getSystemTray();

		PopupMenu menu = new PopupMenu();

		MenuItem open = new MenuItem("Open MatrosDMS");
		open.addActionListener(e -> openBrowser(url));

		MenuItem logs = new MenuItem("Show Logs");
		logs.addActionListener(e -> LogWindow.showOrFocus(logFile()));

		MenuItem quit = new MenuItem("Quit MatrosDMS");
		quit.addActionListener(e -> quit());

		menu.add(open);
		menu.add(logs);
		menu.addSeparator();
		menu.add(quit);

		// Render at the size this desktop actually wants, so the icon stays crisp.
		trayIcon = new TrayIcon(MatrosBadge.image(tray.getTrayIconSize().width), "MatrosDMS", menu);
		trayIcon.setImageAutoSize(true);
		trayIcon.addActionListener(e -> openBrowser(url));

		tray.add(trayIcon);
	}

	private void quit() {
		// Drop the icon first: a graceful shutdown waits for in-flight requests, and a tray icon that
		// lingers through it looks like a hang.
		if (trayIcon != null) {
			SystemTray.getSystemTray().remove(trayIcon);
		}
		new Thread(() -> {
			int code = SpringApplication.exit(context, () -> 0);
			System.exit(code);
		}, "matrosdms-shutdown").start();
	}

	private void openBrowser(String url) {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(URI.create(url));
			}
		} catch (Exception e) {
			log.warn("Could not open a browser for {}: {}", url, e.getMessage());
		}
	}

	private Path logFile() {
		return Path.of(basePath, "workspace", "log", "matrosdms.log");
	}

	private int resolvePort(ApplicationContext ctx) {
		if (ctx instanceof WebServerApplicationContext web && web.getWebServer() != null) {
			return web.getWebServer().getPort();
		}
		return 9090;
	}

	private boolean isContainer() {
		return Files.exists(Path.of("/.dockerenv"))
				|| System.getenv("CONTAINER") != null
				|| System.getenv("KUBERNETES_SERVICE_HOST") != null;
	}
}
