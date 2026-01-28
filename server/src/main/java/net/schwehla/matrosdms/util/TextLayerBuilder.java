/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.util;

/**
 * Utility to generate standardized XML Text Layers. Ensures consistent CDATA
 * wrapping and escaping.
 */
public class TextLayerBuilder {

	private final StringBuilder xml = new StringBuilder();

	public TextLayerBuilder(String source) {
		xml.append("<root source=\"").append(source).append("\">\n");
		xml.append("  <meta>\n");
	}

	public TextLayerBuilder addMeta(String key, String value) {
		if (value != null && !value.isBlank()) {
			xml.append("    <")
					.append(key)
					.append(">")
					.append(escapeXml(value))
					.append("</")
					.append(key)
					.append(">\n");
		}
		return this;
	}

	// Call this before adding content
	public TextLayerBuilder closeMeta() {
		xml.append("  </meta>\n");
		return this;
	}

	public TextLayerBuilder addContent(String text, String mimeType) {
		if (text == null || text.isBlank())
			return this;
		xml.append("  <content type=\"").append(mimeType).append("\">\n");
		xml.append(wrapCdata(text));
		xml.append("  </content>\n");
		return this;
	}

	public TextLayerBuilder addAttachment(String filename, String text) {
		if (text == null || text.isBlank())
			return this;
		xml.append("  <attachment filename=\"").append(escapeXml(filename)).append("\">\n");
		xml.append(wrapCdata(text));
		xml.append("  </attachment>\n");
		return this;
	}

	@Override
	public String toString() {
		if (!xml.toString().endsWith("</root>")) {
			xml.append("</root>");
		}
		return xml.toString();
	}

	private String escapeXml(String input) {
		if (input == null)
			return "";
		return input
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	private String wrapCdata(String input) {
		// CDATA cannot contain "]]>", so we must escape it
		String safe = input.replace("]]>", "]]]]><![CDATA[>");
		return "    <![CDATA[\n" + safe + "\n    ]]>\n";
	}
}
