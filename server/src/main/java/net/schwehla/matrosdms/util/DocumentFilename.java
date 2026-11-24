/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The name a filed document's file goes by when it leaves the archive (download, export): its title,
 * which the user confirmed when filing it, with the extension of the original file. A scanner names a
 * file after the date and the first line it read ("2026-11-17_Lohnart Grundentgelt Tarif….pdf"), a
 * mail client after a hash; the user should not have to rename it. The original name stays in the
 * item's metadata.
 */
public final class DocumentFilename {

	static final int MAX_BASE = 150;

	/** ".pdf", ".mp3", ".7z", but not the year in "Abrechnung 06.2023". */
	private static final Pattern EXTENSION = Pattern.compile("\\.(?=[A-Za-z0-9]*[A-Za-z])[A-Za-z0-9]{1,5}$");
	private static final Pattern SLASH = Pattern.compile("[\\\\/]");
	private static final Pattern NOT_ALLOWED = Pattern.compile("[:*?\"<>|\\p{Cntrl}]");
	private static final Set<String> RESERVED = Set.of("con", "prn", "aux", "nul", "com1", "com2", "com3", "com4",
			"com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8",
			"lpt9");

	private DocumentFilename() {
	}

	/**
	 * @param title the document's title
	 * @param originalFilename the name the file came with, for its extension and as the fallback
	 * @param fallbackExtension the extension when the original name has none (".pdf"), may be null
	 * @return "Bezügemitteilung 2026-11.pdf"; the original name when there is no usable title
	 */
	public static String of(String title, String originalFilename, String fallbackExtension) {
		String extension = extensionOf(originalFilename);
		if (extension.isEmpty() && fallbackExtension != null && EXTENSION.matcher(fallbackExtension).matches())
			extension = fallbackExtension;

		String base = title == null ? "" : title;
		base = SLASH.matcher(base).replaceAll("-"); // "Steuer 2013/2014" → "Steuer 2013-2014"
		base = NOT_ALLOWED.matcher(base).replaceAll(" ");
		base = base.replaceAll("\\s+", " ").trim();
		if (base.length() > MAX_BASE)
			base = base.substring(0, MAX_BASE).trim();
		base = base.replaceAll("[\\s.]+$", ""); // Windows drops trailing dots and blanks
		if (RESERVED.contains(base.toLowerCase(Locale.ROOT)))
			base = base + "_";

		if (base.isEmpty()) {
			if (originalFilename == null || originalFilename.isBlank())
				return "document" + extension;
			return extensionOf(originalFilename).isEmpty() ? originalFilename + extension : originalFilename;
		}
		// a title that already ends in the extension ("Rechnung.pdf") keeps it once
		if (!extension.isEmpty() && base.toLowerCase(Locale.ROOT).endsWith(extension.toLowerCase(Locale.ROOT)))
			return base;
		return base + extension;
	}

	/** ".pdf" for "scan.pdf", "" when there is none. */
	public static String extensionOf(String filename) {
		if (filename == null)
			return "";
		var m = EXTENSION.matcher(filename.trim());
		return m.find() ? m.group() : "";
	}
}
