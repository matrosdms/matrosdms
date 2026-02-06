/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline.steps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPartBuilder;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartBuilder;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import net.schwehla.matrosdms.service.TikaService;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;

/**
 * Embeds all external resources (images, CSS, fonts) into emails for archival.
 * Creates self-contained .eml files that render correctly offline.
 * 
 * Embedded resources are named "_embed_<hash>.<ext>" so the GUI can distinguish
 * them from user attachments.
 */
@Component
@Order(1)
public class EmailEmbeddingStep implements PipelineStep {

	private static final Logger log = LoggerFactory.getLogger(EmailEmbeddingStep.class);
	private static final int MAX_RESOURCE_SIZE = 10 * 1024 * 1024; // 10MB per resource

	@Autowired
	TikaService tikaService;

	// Patterns to find external URLs in HTML
	private static final Pattern IMG_SRC = Pattern.compile(
			"<img[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
	private static final Pattern CSS_URL = Pattern.compile(
			"url\\s*\\(\\s*[\"']?([^\"')]+)[\"']?\\s*\\)", Pattern.CASE_INSENSITIVE);
	private static final Pattern LINK_HREF = Pattern.compile(
			"<link[^>]+href\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
	private static final Pattern SOURCE_SRC = Pattern.compile(
			"<source[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
	private static final Pattern VIDEO_POSTER = Pattern.compile(
			"<video[^>]+poster\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

	private final RestClient restClient;

	public EmailEmbeddingStep(RestClient.Builder builder) {
		this.restClient = builder
				.defaultHeader("User-Agent", "MatrosDMS/Archiver (Thunderbird compatible)")
				.defaultHeader("Accept", "*/*")
				.build();
	}

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		String filename = ctx.getOriginalFile().getFileName().toString().toLowerCase();

		if (!filename.endsWith(".eml")) {
			return;
		}

		ctx.log("Scanning for external resources...");

		Path originalPath = ctx.getOriginalFile();
		Path tempOutput = ctx.getWorkingDir().resolve("embedded_temp.eml");

		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		DefaultMessageBuilder builder = new DefaultMessageBuilder();
		builder.setBodyFactory(bodyFactory);

		Message message;
		try (InputStream is = Files.newInputStream(originalPath)) {
			message = builder.parseMessage(is);
		}

		try {
			String htmlContent = extractHtmlContent(message);

			if (htmlContent == null) {
				return;
			}

			// Find ALL external URLs
			Set<String> externalUrls = findExternalUrls(htmlContent);

			if (externalUrls.isEmpty()) {
				return;
			}

			ctx.log("Downloading " + externalUrls.size() + " external resources...");

			// Download with proper MIME type detection
			Map<String, ResourceData> resourceMap = downloadResources(externalUrls, ctx);

			if (resourceMap.isEmpty()) {
				ctx.addWarning("No resources could be downloaded");
				return;
			}

			ctx.log("Embedding " + resourceMap.size() + " resources...");
			log.info("Embedding {} resources into email {}", resourceMap.size(), ctx.getHash());

			Message updatedMessage = embedResourcesInEmail(message, htmlContent, resourceMap, bodyFactory);

			DefaultMessageWriter writer = new DefaultMessageWriter();
			try (OutputStream out = Files.newOutputStream(tempOutput)) {
				writer.writeEntity(updatedMessage, out);
			}

			updatedMessage.dispose();
			Files.move(tempOutput, originalPath, StandardCopyOption.REPLACE_EXISTING);

		} finally {
			message.dispose();
		}
	}

	/**
	 * Find all external URLs in HTML content (images, stylesheets, fonts, etc.)
	 */
	private Set<String> findExternalUrls(String html) {
		Set<String> urls = new HashSet<>();

		extractUrls(IMG_SRC, html, urls);
		extractUrls(CSS_URL, html, urls);
		extractUrls(LINK_HREF, html, urls);
		extractUrls(SOURCE_SRC, html, urls);
		extractUrls(VIDEO_POSTER, html, urls);

		// Filter to only external http(s) URLs
		urls.removeIf(url -> !url.startsWith("http://") && !url.startsWith("https://"));

		// Skip tracking pixels and analytics
		urls.removeIf(url -> isTrackingUrl(url));

		return urls;
	}

	private void extractUrls(Pattern pattern, String html, Set<String> urls) {
		Matcher m = pattern.matcher(html);
		while (m.find()) {
			String url = m.group(1).trim();
			if (!url.isEmpty() && !url.startsWith("data:") && !url.startsWith("cid:")) {
				urls.add(url);
			}
		}
	}

	private boolean isTrackingUrl(String url) {
		String lower = url.toLowerCase();
		return lower.contains("/track") ||
				lower.contains("/pixel") ||
				lower.contains("/open.") ||
				lower.contains("mailchimp.com/track") ||
				lower.contains("google-analytics") ||
				lower.contains("/beacon");
	}

	/**
	 * Download resources with proper MIME type from HTTP Content-Type header
	 */
	private Map<String, ResourceData> downloadResources(Set<String> urls, PipelineContext ctx) {
		Map<String, ResourceData> resources = new HashMap<>();

		for (String url : urls) {
			try {
				ResponseEntity<byte[]> response = restClient.get()
						.uri(URI.create(url))
						.retrieve()
						.toEntity(byte[].class);

				byte[] data = response.getBody();
				if (data == null || data.length == 0) {
					continue;
				}

				if (data.length > MAX_RESOURCE_SIZE) {
					ctx.addWarning("Skipping large resource: " + url);
					continue;
				}

				// Use Tika for reliable MIME type detection from content
				String mimeType = tikaService.detectMimeType(data);

				// Generate stable content ID from hash
				String hash = hashBytes(data);
				String contentId = hash.substring(0, 12);
				String extension = extensionForMime(mimeType);
				String filename = "_embed_" + contentId + extension;

				resources.put(url, new ResourceData(contentId, filename, data, mimeType));

				log.debug("Downloaded: {} -> {} ({})", url, filename, mimeType);

			} catch (Exception e) {
				log.warn("Failed to download: {} - {}", url, e.getMessage());
			}
		}

		return resources;
	}

	private Message embedResourcesInEmail(
			Message originalMessage,
			String htmlContent,
			Map<String, ResourceData> resourceMap,
			BasicBodyFactory bodyFactory)
			throws IOException {

		// Replace external URLs with cid: references in HTML
		String updatedHtml = htmlContent;
		for (var entry : resourceMap.entrySet()) {
			updatedHtml = updatedHtml.replace(entry.getKey(), "cid:" + entry.getValue().contentId());
		}

		// Recreate the original body structure with updated HTML
		Body newBody = rebuildBodyWithUpdatedHtml(originalMessage.getBody(), updatedHtml, resourceMap, bodyFactory);

		MessageImpl newMessage = new MessageImpl();
		newMessage.setHeader(originalMessage.getHeader());
		newMessage.setBody(newBody);

		return newMessage;
	}

	private Body rebuildBodyWithUpdatedHtml(
			Body originalBody,
			String updatedHtml,
			Map<String, ResourceData> resourceMap,
			BasicBodyFactory bodyFactory)
			throws IOException {

		if (originalBody instanceof Multipart multipart) {
			if ("related".equalsIgnoreCase(multipart.getSubType())) {
				return addResourcesToRelated(multipart, updatedHtml, resourceMap, bodyFactory);
			} else {
				return wrapInRelatedWithResources(multipart, updatedHtml, resourceMap, bodyFactory);
			}
		} else {
			return createRelatedWithResources(updatedHtml, resourceMap, bodyFactory);
		}
	}

	private Body addResourcesToRelated(
			Multipart originalRelated,
			String updatedHtml,
			Map<String, ResourceData> resourceMap,
			BasicBodyFactory bodyFactory)
			throws IOException {

		MultipartBuilder related = MultipartBuilder.create("related");

		for (Entity part : originalRelated.getBodyParts()) {
			if (isHtmlPart(part)) {
				BodyPartBuilder htmlPart = BodyPartBuilder.create();
				htmlPart.setBody(updatedHtml, "html", StandardCharsets.UTF_8);
				copyHeaders(part, htmlPart);
				related.addBodyPart(htmlPart);
			} else {
				related.addBodyPart(part);
			}
		}

		addResourceParts(related, resourceMap, bodyFactory);
		return related.build();
	}

	private Body wrapInRelatedWithResources(
			Multipart originalMultipart,
			String updatedHtml,
			Map<String, ResourceData> resourceMap,
			BasicBodyFactory bodyFactory)
			throws IOException {

		MultipartBuilder wrapper = MultipartBuilder.create(originalMultipart.getSubType());

		for (Entity part : originalMultipart.getBodyParts()) {
			if (isHtmlPart(part)) {
				BodyPartBuilder htmlPart = BodyPartBuilder.create();
				htmlPart.setBody(updatedHtml, "html", StandardCharsets.UTF_8);
				copyHeaders(part, htmlPart);
				wrapper.addBodyPart(htmlPart);
			} else {
				wrapper.addBodyPart(part);
			}
		}

		MultipartBuilder related = MultipartBuilder.create("related");
		BodyPartBuilder contentPart = BodyPartBuilder.create();
		contentPart.setBody(wrapper.build());
		related.addBodyPart(contentPart);

		addResourceParts(related, resourceMap, bodyFactory);
		return related.build();
	}

	private Body createRelatedWithResources(
			String updatedHtml, Map<String, ResourceData> resourceMap, BasicBodyFactory bodyFactory)
			throws IOException {

		MultipartBuilder related = MultipartBuilder.create("related");

		BodyPartBuilder htmlPart = BodyPartBuilder.create();
		htmlPart.setBody(updatedHtml, "html", StandardCharsets.UTF_8);
		related.addBodyPart(htmlPart);

		addResourceParts(related, resourceMap, bodyFactory);
		return related.build();
	}

	private void addResourceParts(
			MultipartBuilder related, Map<String, ResourceData> resourceMap, BasicBodyFactory bodyFactory) {

		for (ResourceData res : resourceMap.values()) {
			BodyPartBuilder part = BodyPartBuilder.create();

			part.setBody(bodyFactory.binaryBody(res.data()));
			part.setContentTransferEncoding("base64");
			part.setField(new RawField("Content-Type", res.mimeType()));

			// Inline disposition for rendering, filename prefixed for GUI filtering
			part.setContentDisposition("inline; filename=\"" + res.filename() + "\"");
			part.setField(new RawField("Content-ID", "<" + res.contentId() + ">"));
			part.setField(new RawField("X-MatrosDMS-Embedded", "true"));

			related.addBodyPart(part);
		}
	}

	private boolean isHtmlPart(Entity part) {
		return "text/html".equalsIgnoreCase(part.getMimeType());
	}

	private void copyHeaders(Entity source, BodyPartBuilder target) {
		if (source.getContentTransferEncoding() != null) {
			target.setContentTransferEncoding(source.getContentTransferEncoding());
		}
		if (source.getCharset() != null) {
			target.setField(
					new RawField("Content-Type", source.getMimeType() + "; charset=" + source.getCharset()));
		}
	}

	private String extractHtmlContent(Entity entity) throws IOException {
		Body body = entity.getBody();
		if (body instanceof TextBody textBody) {
			if ("text/html".equalsIgnoreCase(entity.getMimeType())) {
				try (Reader reader = textBody.getReader()) {
					return readFully(reader);
				}
			}
		} else if (body instanceof Multipart multipart) {
			for (Entity part : multipart.getBodyParts()) {
				String html = extractHtmlContent(part);
				if (html != null)
					return html;
			}
		}
		return null;
	}

	private String readFully(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buffer = new char[4096];
		int bytesRead;
		while ((bytesRead = reader.read(buffer)) != -1) {
			sb.append(buffer, 0, bytesRead);
		}
		return sb.toString();
	}

	private String extensionForMime(String mime) {
		return switch (mime) {
			case "image/png" -> ".png";
			case "image/jpeg" -> ".jpg";
			case "image/gif" -> ".gif";
			case "image/webp" -> ".webp";
			case "image/svg+xml" -> ".svg";
			case "text/css" -> ".css";
			case "font/woff" -> ".woff";
			case "font/woff2" -> ".woff2";
			case "font/ttf" -> ".ttf";
			case "application/vnd.ms-fontobject" -> ".eot";
			default -> "";
		};
	}

	private String hashBytes(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(data);
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			return String.valueOf(System.nanoTime());
		}
	}

	/**
	 * Resource data with stable content-based ID for deduplication
	 */
	record ResourceData(String contentId, String filename, byte[] data, String mimeType) {
	}
}
