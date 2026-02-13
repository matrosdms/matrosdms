/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextLayerUtils {

	// Regex to extract content inside CDATA
    // Flag DOTALL is critical to capture newlines inside CDATA
	private static final Pattern CONTENT_PATTERN = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);

	// Fallback: Strip all XML tags
	private static final Pattern TAG_STRIP_PATTERN = Pattern.compile("<[^>]+>");

	public static String extractCleanText(String xmlTextLayer) {
		if (xmlTextLayer == null || xmlTextLayer.isEmpty())
			return "";

		// SAFETY 1: Reject binary content/raw PDF bytes masquerading as text layer
		if (isBinaryContent(xmlTextLayer)) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		// SAFETY 2: Strict CDATA extraction
        // This ensures we ONLY index content we explicitly extracted and wrapped.
        // It ignores attributes like <root source="SCAN"> which shouldn't be indexed as text.
		Matcher m = CONTENT_PATTERN.matcher(xmlTextLayer);
		boolean foundCdata = false;
		while (m.find()) {
			sb.append(m.group(1).trim()).append("\n\n");
			foundCdata = true;
		}

		// Fallback for legacy files without CDATA wrapping (if any exist)
		if (!foundCdata) {
            // Only runs if the file looks like XML but has no CDATA
            if (xmlTextLayer.trim().startsWith("<")) {
			    String stripped = TAG_STRIP_PATTERN.matcher(xmlTextLayer).replaceAll(" ");
			    return stripped.replaceAll("\\s+", " ").trim();
            } else {
                // It's just plain text
                return xmlTextLayer;
            }
		}

		return sb.toString().trim();
	}

	private static boolean isBinaryContent(String content) {
		// Check the first 512 chars (enough to detect binary headers like %PDF)
		int checkLen = Math.min(content.length(), 512);
		int nonPrintable = 0;
		for (int i = 0; i < checkLen; i++) {
			char c = content.charAt(i);
			// Allow tab, newline, carriage return + normal printable range
			if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
				nonPrintable++;
			} else if (c == 0xFFFD) { // Unicode replacement character
				nonPrintable++;
			}
		}
		// More than 5% non-printable → binary garbage
		return nonPrintable > checkLen * 0.05;
	}
}