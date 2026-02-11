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

	// Regex to extract content inside CDATA or raw tags
	private static final Pattern CONTENT_PATTERN = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);

	// Fallback if CDATA is missing but tags exist
	private static final Pattern TAG_STRIP_PATTERN = Pattern.compile("<[^>]+>");

	public static String extractCleanText(String xmlTextLayer) {
		if (xmlTextLayer == null || xmlTextLayer.isEmpty())
			return "";

		// Reject binary content (e.g. raw PDF bytes stored without text extraction)
		if (isBinaryContent(xmlTextLayer)) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		// 1. Try to extract CDATA content (The standard we implemented)
		Matcher m = CONTENT_PATTERN.matcher(xmlTextLayer);
		boolean foundCdata = false;
		while (m.find()) {
			sb.append(m.group(1).trim()).append("\n\n");
			foundCdata = true;
		}

		// 2. If no CDATA found (legacy files?), just strip tags
		if (!foundCdata) {
			String stripped = TAG_STRIP_PATTERN.matcher(xmlTextLayer).replaceAll(" ");
			return stripped.replaceAll("\\s+", " ").trim();
		}

		return sb.toString().trim();
	}

	/**
	 * Detect binary/non-text content by checking for non-printable characters.
	 * A valid text layer is XML or plain text — never raw PDF/image bytes.
	 */
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
