/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Window;
import java.io.InputStream;
import java.util.Properties;

/**
 * A splash window shown from {@code main()} <em>before</em> Spring boots.
 *
 * <p>Two reasons it cannot wait for Spring: the desktop build has no console window, so without this
 * the user double-clicks and stares at nothing for the whole boot; and Spring Boot forces
 * {@code java.awt.headless=true} early in startup, after which no window can be created at all.
 *
 * <p>Pure sugar — every failure path is swallowed. {@link TrayLauncher} dismisses it when the
 * application is ready.
 */
public final class DesktopSplash {

	private static volatile Window window;
	private static volatile Frame owner;

	/**
	 * Read once at class-load: the splash paints before Spring exists, so a {@code BuildProperties}
	 * bean is not an option — but the same file that backs it is a plain classpath resource, and the
	 * {@code build-info} goal writes it for both the fat jar and {@code target/classes} in dev.
	 * Empty when absent (e.g. an IDE run that skipped the goal), in which case no version is drawn.
	 */
	private static final String VERSION = readVersion();

	private DesktopSplash() {
	}

	private static String readVersion() {
		try (InputStream in = DesktopSplash.class.getResourceAsStream("/META-INF/build-info.properties")) {
			if (in == null) {
				return "";
			}
			Properties p = new Properties();
			p.load(in);
			String v = p.getProperty("build.version", "").trim();
			return v.isEmpty() ? "" : "v" + v;
		} catch (Exception ignored) {
			return "";
		}
	}

	/** Shows the splash, or does nothing at all when there is no desktop (jar, container, CI). */
	public static void showIfDesktop() {
		try {
			if (GraphicsEnvironment.isHeadless()) {
				return;
			}
			EventQueue.invokeAndWait(DesktopSplash::build);
		} catch (Throwable ignored) {
			// A missing or broken display throws AWTError — an Error, not an Exception.
		}
	}

	/** Dismisses the splash. Safe to call when it was never shown. */
	static void close() {
		Window w = window;
		Frame o = owner;
		window = null;
		owner = null;
		if (w == null) {
			return;
		}
		EventQueue.invokeLater(() -> {
			w.dispose();
			if (o != null) {
				o.dispose();
			}
		});
	}

	private static void build() {
		// An undecorated utility owner keeps the splash out of the taskbar.
		Frame o = new Frame();
		o.setUndecorated(true);
		o.setType(Window.Type.UTILITY);

		Window w = new SplashWindow(o);
		w.setSize(400, 230);
		w.setLocationRelativeTo(null);
		w.setVisible(true);

		owner = o;
		window = w;
	}

	private static final class SplashWindow extends Window {

		private static final long serialVersionUID = 1L;

		private SplashWindow(Frame owner) {
			super(owner);
		}

		@Override
		public void paint(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int w = getWidth();
			int h = getHeight();

			// Card: the app's near-black background with a subtle border.
			g2.setColor(new Color(0x07, 0x09, 0x0D));
			g2.fillRect(0, 0, w, h);
			g2.setColor(new Color(0x1E, 0x2A, 0x3A));
			g2.drawRect(0, 0, w - 1, h - 1);

			int badge = 84;
			Graphics2D badgeG = (Graphics2D) g2.create((w - badge) / 2, 32, badge, badge);
			MatrosBadge.paint(badgeG, badge, false);
			badgeG.dispose();

			centered(g2, "MatrosDMS", new Font(Font.SANS_SERIF, Font.BOLD, 24), new Color(0xDE, 0xEA, 0xF5), w, 154);
			centered(g2, "Starting…", new Font(Font.SANS_SERIF, Font.PLAIN, 13), new Color(0x6E, 0x94, 0xB0), w, 182);
			if (!VERSION.isEmpty()) {
				centered(g2, VERSION, new Font(Font.SANS_SERIF, Font.PLAIN, 11), new Color(0x45, 0x5E, 0x74), w, 206);
			}
		}

		private void centered(Graphics2D g2, String text, Font font, Color color, int width, int y) {
			g2.setFont(font);
			g2.setColor(color);
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(text, (width - fm.stringWidth(text)) / 2, y);
		}
	}
}
