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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextLayerUtils {

	private static final Logger log = LoggerFactory.getLogger(TextLayerUtils.class);

	// Regex to extract content inside CDATA
	private static final Pattern CONTENT_PATTERN = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);

	// Fallback: Strip all XML-like tags
	private static final Pattern TAG_STRIP_PATTERN = Pattern.compile("<[^>]+>");

	public static String extractCleanText(String xmlTextLayer) {
		if (xmlTextLayer == null || xmlTextLayer.isEmpty())
			return "";

		// SAFETY 1: Reject binary content (PDF bytes masquerading as text)
		if (isBinaryContent(xmlTextLayer)) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		// STRATEGY A: Try to find structured CDATA content
		Matcher m = CONTENT_PATTERN.matcher(xmlTextLayer);
		boolean foundCdata = false;
		while (m.find()) {
			sb.append(m.group(1).trim()).append("\n\n");
			foundCdata = true;
		}

		if (foundCdata) {
			return sb.toString().trim();
		}

		// STRATEGY B (Fallback): If no CDATA found, it might be raw text or malformed
		// XML.
		// Just strip any <tags> and return the rest. This ensures we index *something*.
		try {
			String stripped = TAG_STRIP_PATTERN.matcher(xmlTextLayer).replaceAll(" ");
			String clean = stripped.replaceAll("\\s+", " ").trim();

			if (!clean.isEmpty()) {
				log.debug("Fallback extraction used (XML tags stripped)");
				return clean;
			}
		} catch (Exception e) {
			log.warn("Text extraction error", e);
		}

		// STRATEGY C: Return raw (Last resort)
		return xmlTextLayer.trim();
	}

	private static boolean isBinaryContent(String content) {
		// Check first 512 chars for excessive nulls or control chars
		int checkLen = Math.min(content.length(), 512);
		int nonPrintable = 0;
		for (int i = 0; i < checkLen; i++) {
			char c = content.charAt(i);
			if (c < 0x09 || (c > 0x0D && c < 0x20)) { // Strict control chars
				nonPrintable++;
			}
		}
		return nonPrintable > checkLen * 0.10; // >10% garbage = binary
	}
}