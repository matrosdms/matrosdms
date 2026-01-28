/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.mail.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

public class MimeHelper {

	private static final SimpleDateFormat IMAP_DATE_FMT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z", Locale.US);

	public static String getInternalDate(Path path) {
		try {
			BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
			return IMAP_DATE_FMT.format(new Date(attr.creationTime().toMillis()));
		} catch (Exception e) {
			return IMAP_DATE_FMT.format(new Date());
		}
	}

	public static String getBodyStructure(Path path) {
		try (InputStream is = new FileInputStream(path.toFile())) {
			Session session = Session.getDefaultInstance(new Properties());
			MimeMessage msg = new MimeMessage(session, is);
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			parsePart(msg, sb);
			sb.append(")");
			return "BODYSTRUCTURE " + sb.toString();
		} catch (Exception e) {
			// Fallback for non-MIME or corrupt files
			return "BODYSTRUCTURE (\"TEXT\" \"PLAIN\" NIL NIL NIL \"7BIT\" "
					+ path.toFile().length()
					+ " 0 NIL NIL NIL NIL)";
		}
	}

	public static byte[] getPartContent(Path path, String partId) {
		try (InputStream is = new FileInputStream(path.toFile())) {
			Session session = Session.getDefaultInstance(new Properties());
			MimeMessage msg = new MimeMessage(session, is);

			if (partId == null || partId.isEmpty() || partId.equals("TEXT"))
				return Files.readAllBytes(path);

			Object content = msg.getContent();
			if (content instanceof Multipart) {
				Multipart mp = (Multipart) content;
				try {
					// IMAP parts are 1-based, Jakarta Mail is 0-based
					int index = Integer.parseInt(partId) - 1;
					if (index >= 0 && index < mp.getCount()) {
						return mp.getBodyPart(index).getInputStream().readAllBytes();
					}
				} catch (NumberFormatException e) {
				}
			} else if ("1".equals(partId)) {
				return msg.getInputStream().readAllBytes();
			}
		} catch (Exception e) {
		}
		return new byte[0];
	}

	private static void parsePart(Part part, StringBuilder sb)
			throws MessagingException, IOException {
		if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				if (i > 0)
					sb.append(" ");
				if (mp.getBodyPart(i).isMimeType("multipart/*")) {
					sb.append("(");
					parsePart(mp.getBodyPart(i), sb);
					sb.append(")");
				} else {
					parsePart(mp.getBodyPart(i), sb);
				}
			}
			sb.append(" \"")
					.append(part.getContentType().split("/")[1].split(";")[0].toUpperCase())
					.append("\"");
			sb.append(" NIL NIL NIL");
		} else {
			String type = "TEXT";
			String subtype = "PLAIN";
			String contentType = part.getContentType();

			if (contentType.contains("/")) {
				String[] split = contentType.split("/");
				type = split[0].toUpperCase();
				String[] subSplit = split[1].split(";");
				subtype = subSplit[0].toUpperCase();
			}

			sb.append("(\"").append(type).append("\" \"").append(subtype).append("\"");
			sb.append(" NIL NIL NIL \"7BIT\" ").append(part.getSize());
			if (type.equals("TEXT"))
				sb.append(" 0");

			String disposition = part.getDisposition();
			if (disposition != null) {
				sb.append(" (\"").append(disposition.toUpperCase()).append("\"");
				String fileName = part.getFileName();
				if (fileName != null)
					sb.append(" (\"FILENAME\" \"").append(fileName).append("\")");
				else
					sb.append(" NIL");
				sb.append(")");
			} else {
				sb.append(" NIL");
			}
			sb.append(" NIL NIL)");
		}
	}
}
