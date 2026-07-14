/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * The MatrosDMS mark — four rounded squares — drawn with Java2D.
 *
 * <p>Deliberately has no dependencies beyond the JDK: the build compiles this class and
 * {@link IconGenerator} with a bare {@code javac} to render the packaging icons, so there is no
 * binary icon checked in that can drift from the brand.
 *
 * <p>Two renderings, because a tray icon and a desktop icon have opposite problems:
 * <ul>
 *   <li>{@link #image(int)} — bare squares on transparency, for the system tray and window icons.
 *       A background tile would vanish into a taskbar of the same colour.
 *   <li>{@link #tile(int)} — the squares on a dark rounded tile, for the launcher/menu icon, where
 *       the icon sits on the user's wallpaper and needs its own backdrop.
 * </ul>
 */
final class MatrosBadge {

	/** Brand accent, matching the web UI and matrosdms.github.io. */
	private static final Color ACCENT = new Color(0x3B, 0x9E, 0xFF);

	/** Near-black backdrop for the tile rendering. */
	private static final Color TILE_BG = new Color(0x0B, 0x12, 0x20);

	/** The mark is authored on a 28x28 grid (same geometry as the SVG favicon). */
	private static final double GRID = 28.0;

	private MatrosBadge() {
	}

	/** The four-square mark on transparency — system tray, window icons. */
	static BufferedImage image(int size) {
		return render(size, false);
	}

	/** The mark on a dark rounded tile — launcher, menu and splash. */
	static BufferedImage tile(int size) {
		return render(size, true);
	}

	private static BufferedImage render(int size, boolean withTile) {
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		paint(g, size, withTile);
		g.dispose();
		return img;
	}

	/**
	 * Paints the mark into {@code size} pixels at the graphics origin. Used directly by the splash,
	 * which paints onto an existing surface rather than into an image.
	 */
	static void paint(Graphics2D g, double size, boolean withTile) {
		Graphics2D gg = (Graphics2D) g.create();
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gg.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		gg.scale(size / GRID, size / GRID);

		if (withTile) {
			gg.setColor(TILE_BG);
			gg.fill(new RoundRectangle2D.Double(0, 0, 28, 28, 12, 12));
		}

		// Four squares, clockwise from top-left, fading out — the MatrosDMS mark.
		square(gg, 2, 2, 1.0f);
		square(gg, 16, 2, 0.5f);
		square(gg, 2, 16, 0.5f);
		square(gg, 16, 16, 0.25f);

		gg.dispose();
	}

	private static void square(Graphics2D g, double x, double y, float alpha) {
		g.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), Math.round(alpha * 255f)));
		g.fill(new RoundRectangle2D.Double(x, y, 10, 10, 4, 4));
	}
}
