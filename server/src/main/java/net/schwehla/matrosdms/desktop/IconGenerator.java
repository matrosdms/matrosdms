/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Build-time only: renders {@link MatrosBadge} to the icon file jpackage wants.
 *
 * <pre>
 *   javac -d icon-classes MatrosBadge.java IconGenerator.java
 *   java -cp icon-classes net.schwehla.matrosdms.desktop.IconGenerator icon/matrosdms.ico
 *   java -cp icon-classes net.schwehla.matrosdms.desktop.IconGenerator icon/matrosdms.png
 * </pre>
 *
 * <p>Both classes are JDK-only on purpose, so CI can compile just these two files without Maven or
 * the application classpath. Java2D renders fine on a headless runner.
 *
 * <p>Not a Spring bean and never referenced by the running application.
 */
public final class IconGenerator {

	/** Sizes embedded in the Windows .ico. 256 is what modern Explorer actually shows. */
	private static final int[] ICO_SIZES = { 16, 32, 48, 64, 128, 256 };

	/** Linux/macOS take a single PNG; jpackage scales it down as needed. */
	private static final int PNG_SIZE = 256;

	private IconGenerator() {
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("usage: IconGenerator <out.ico|out.png>");
			System.exit(2);
		}
		Path out = Path.of(args[0]);
		if (out.getParent() != null) {
			Files.createDirectories(out.getParent());
		}

		if (args[0].toLowerCase().endsWith(".png")) {
			ImageIO.write(MatrosBadge.tile(PNG_SIZE), "png", out.toFile());
		} else {
			writeIco(out.toFile());
		}
		System.out.println("wrote " + out.toAbsolutePath());
	}

	/**
	 * Writes a multi-resolution .ico whose entries are PNG-compressed — allowed since Vista, and the
	 * only sane way to carry a 256px entry.
	 */
	private static void writeIco(File out) throws IOException {
		List<byte[]> pngs = new ArrayList<>();
		for (int size : ICO_SIZES) {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			ImageIO.write(MatrosBadge.tile(size), "png", buf);
			pngs.add(buf.toByteArray());
		}

		try (OutputStream fileOut = Files.newOutputStream(out.toPath());
				DataOutputStream data = new DataOutputStream(fileOut)) {

			// ICONDIR
			writeLe16(data, 0); // reserved
			writeLe16(data, 1); // type: icon
			writeLe16(data, ICO_SIZES.length);

			// ICONDIRENTRY[] — image data follows the directory, so offsets start after it.
			int offset = 6 + (16 * ICO_SIZES.length);
			for (int i = 0; i < ICO_SIZES.length; i++) {
				int size = ICO_SIZES[i];
				byte[] png = pngs.get(i);
				data.writeByte(size >= 256 ? 0 : size); // width  (0 means 256)
				data.writeByte(size >= 256 ? 0 : size); // height (0 means 256)
				data.writeByte(0); // palette size: none, true colour
				data.writeByte(0); // reserved
				writeLe16(data, 1); // colour planes
				writeLe16(data, 32); // bits per pixel
				writeLe32(data, png.length);
				writeLe32(data, offset);
				offset += png.length;
			}

			for (byte[] png : pngs) {
				data.write(png);
			}
		}
	}

	private static void writeLe16(DataOutputStream out, int value) throws IOException {
		out.writeByte(value & 0xFF);
		out.writeByte((value >>> 8) & 0xFF);
	}

	private static void writeLe32(DataOutputStream out, int value) throws IOException {
		out.writeByte(value & 0xFF);
		out.writeByte((value >>> 8) & 0xFF);
		out.writeByte((value >>> 16) & 0xFF);
		out.writeByte((value >>> 24) & 0xFF);
	}
}
