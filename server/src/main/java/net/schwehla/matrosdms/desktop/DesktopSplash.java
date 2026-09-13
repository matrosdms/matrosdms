/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Label;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.TextArea;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A splash window shown from {@code main()} <em>before</em> Spring boots.
 *
 * <p>Two reasons it cannot wait for Spring: the desktop build has no console window, so without this
 * the user double-clicks and stares at nothing for the whole boot; and Spring Boot forces
 * {@code java.awt.headless=true} early in startup, after which no window can be created at all.
 *
 * <p>Pure sugar — every failure path is swallowed. {@link TrayLauncher} dismisses it when the
 * application is ready, {@link #fail} when the boot dies instead.
 */
public final class DesktopSplash {

	/** Cap on the failure notice: a start nobody is watching must not trade one hang for another. */
	private static final Duration FAILURE_NOTICE_TIMEOUT = Duration.ofMinutes(5);

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

	/**
	 * Reports a boot failure: dismisses the splash and, on a desktop, shows the reason until the
	 * user closes it. Returns so the caller can exit.
	 *
	 * <p>Every failure path out of {@code SpringApplication.run} must come through here. The splash
	 * owns AWT's <em>non-daemon</em> threads, so a start that dies inside Spring otherwise leaves a
	 * "Starting…" window on screen and a JVM that never exits — and the desktop build has no console,
	 * so the stack trace Spring printed goes nowhere and the app simply appears to hang forever.
	 */
	public static void fail(Throwable failure) {
		close();
		try {
			if (GraphicsEnvironment.isHeadless()) {
				return;
			}
			CountDownLatch dismissed = new CountDownLatch(1);
			EventQueue.invokeLater(() -> showFailureNotice(describe(failure), dismissed));
			dismissed.await(FAILURE_NOTICE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Throwable ignored) {
			// Same contract as the splash itself: a broken display must not shadow the real failure.
		}
	}

	/**
	 * The innermost cause carries the sentence a user can act on ("Database may be already in use"),
	 * where the outermost is Spring's bean-creation wrapper. The log file is named too: it is the
	 * only place the full stack trace survives a console-less start.
	 */
	static String describe(Throwable failure) {
		Throwable cause = failure;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		String message = cause.getMessage();
		StringBuilder text = new StringBuilder(
				message == null || message.isBlank() ? cause.toString() : message.strip());

		String dataDir = System.getProperty("MATROS_DATA_DIR");
		if (dataDir != null && !dataDir.isBlank()) {
			text.append("\n\nFull details: ").append(Path.of(dataDir, "workspace", "log", "matrosdms.log"));
		}
		return text.toString();
	}

	private static void showFailureNotice(String message, CountDownLatch dismissed) {
		Frame frame = new Frame("MatrosDMS");
		frame.setLayout(new BorderLayout(12, 12));
		frame.add(new Label("MatrosDMS could not start."), BorderLayout.NORTH);

		TextArea details = new TextArea(message, 8, 70, TextArea.SCROLLBARS_VERTICAL_ONLY);
		details.setEditable(false);
		frame.add(details, BorderLayout.CENTER);

		Button close = new Button("Close");
		Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(close);
		frame.add(buttons, BorderLayout.SOUTH);

		Runnable dismiss = () -> {
			frame.dispose();
			dismissed.countDown();
		};
		close.addActionListener(e -> dismiss.run());
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dismiss.run();
			}
		});

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.toFront();
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

			centered(g2, "MatrosDMS", new Font(Font.SANS_SERIF, Font.BOLD, 24), new Color(0xDE, 0xEA, 0xF5), w, 150);
			if (!VERSION.isEmpty()) {
				centered(g2, VERSION, new Font(Font.SANS_SERIF, Font.PLAIN, 12), new Color(0x8F, 0xAD, 0xC6), w, 170);
			}
			// The profile is resolved in main() before the splash shows, so the label is already set.
			String profile = System.getProperty(ProfileManager.ACTIVE_PROFILE_LABEL_PROPERTY, "");
			String starting = profile.isEmpty() ? "Starting…" : "Starting " + profile + "…";
			centered(g2, starting, new Font(Font.SANS_SERIF, Font.PLAIN, 13), new Color(0x6E, 0x94, 0xB0), w, 200);
		}

		private void centered(Graphics2D g2, String text, Font font, Color color, int width, int y) {
			g2.setFont(font);
			g2.setColor(color);
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(text, (width - fm.stringWidth(text)) / 2, y);
		}
	}
}
