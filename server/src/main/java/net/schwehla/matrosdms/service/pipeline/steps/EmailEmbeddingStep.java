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
import net.schwehla.matrosdms.store.util.FileExtensionService;

/**
 * Embeds external resources into emails.
 * IMPROVED: Handles srcset, background attributes, and uses browser User-Agent.
 */
@Component
@Order(1) // Runs early
public class EmailEmbeddingStep implements PipelineStep {

    private static final Logger log = LoggerFactory.getLogger(EmailEmbeddingStep.class);
    private static final int MAX_RESOURCE_SIZE = 15 * 1024 * 1024; // 15MB

    @Autowired
    TikaService tikaService;
    
    @Autowired
    FileExtensionService extensionService;  

    // --- Regex Patterns ---
    private static final Pattern IMG_SRC = Pattern.compile(
            "<img[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    
    // NEW: Handle srcset="url 1x, url 2x"
    private static final Pattern IMG_SRCSET = Pattern.compile(
            "srcset\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            
    private static final Pattern CSS_URL = Pattern.compile(
            "url\\s*\\(\\s*[\"']?([^\"')]+)[\"']?\\s*\\)", Pattern.CASE_INSENSITIVE);
    
    // NEW: Handle legacy background="..."
    private static final Pattern TAG_BACKGROUND = Pattern.compile(
            "background\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            
    private static final Pattern LINK_HREF = Pattern.compile(
            "<link[^>]+href\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;

    public EmailEmbeddingStep(RestClient.Builder builder) {
        this.restClient = builder
                // FIX: Use a real browser User-Agent to avoid blocking by Temu/Amazon/etc.
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .build();
    }

    @Override
    public void execute(PipelineContext ctx) throws Exception {
        String filename = ctx.getOriginalFile().getFileName().toString().toLowerCase();

        if (!filename.endsWith(".eml")) {
            return;
        }

        ctx.log("Optimizing Email Resources...");

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

            Set<String> externalUrls = findExternalUrls(htmlContent);

            if (externalUrls.isEmpty()) {
                return;
            }

            ctx.log("Downloading " + externalUrls.size() + " resources...");
            Map<String, ResourceData> resourceMap = downloadResources(externalUrls, ctx);

            if (resourceMap.isEmpty()) {
                ctx.addWarning("Resources found but failed to download");
                return;
            }

            log.info("Embedding {}/{} resources into email {}", resourceMap.size(), externalUrls.size(), ctx.getHash());

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

    private Set<String> findExternalUrls(String html) {
        Set<String> urls = new HashSet<>();

        extractUrls(IMG_SRC, html, urls);
        extractUrls(CSS_URL, html, urls);
        extractUrls(LINK_HREF, html, urls);
        extractUrls(TAG_BACKGROUND, html, urls); // Table backgrounds
        
        // Complex handling for SRCSET
        Matcher m = IMG_SRCSET.matcher(html);
        while (m.find()) {
            String srcset = m.group(1);
            // Split "url1 1x, url2 2x" -> url1, url2
            String[] parts = srcset.split(",");
            for (String part : parts) {
                String url = part.trim().split("\\s+")[0]; // Take first part (url) before space (size)
                if (isValidUrl(url)) {
                    urls.add(url);
                }
            }
        }

        return urls;
    }

    private void extractUrls(Pattern pattern, String html, Set<String> urls) {
        Matcher m = pattern.matcher(html);
        while (m.find()) {
            String url = m.group(1).trim();
            if (isValidUrl(url)) {
                urls.add(url);
            }
        }
    }
    
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        if (url.startsWith("data:") || url.startsWith("cid:")) return false;
        
        String lower = url.toLowerCase();
        // Skip obvious tracking pixels to save time, but be permissive
        if (lower.contains("/track/open") || lower.contains("/pixel")) return false;
        
        return lower.startsWith("http");
    }

    private Map<String, ResourceData> downloadResources(Set<String> urls, PipelineContext ctx) {
        Map<String, ResourceData> resources = new HashMap<>();

        for (String url : urls) {
            try {
                // Decode HTML entities in URL (e.g. &amp; -> &)
                String cleanUrl = url.replace("&amp;", "&");
                
                ResponseEntity<byte[]> response = restClient.get()
                        .uri(URI.create(cleanUrl))
                        .retrieve()
                        .toEntity(byte[].class);

                byte[] data = response.getBody();
                if (data == null || data.length == 0) continue;

                if (data.length > MAX_RESOURCE_SIZE) continue;

                String mimeType = tikaService.detectMimeType(data);
                String hash = hashBytes(data);
                String contentId = hash.substring(0, 12);
                String extension = extensionService.getExtensionForMimeType(mimeType);
                String filename = "_embed_" + contentId + extension;

                // Map BOTH the original raw string (for replacement) AND the cleaned URL
                resources.put(url, new ResourceData(contentId, filename, data, mimeType));
                
                log.debug("Downloaded: {}", filename);

            } catch (Exception e) {
                log.debug("Download failed: {} ({})", url, e.getMessage());
            }
        }
        return resources;
    }

    private Message embedResourcesInEmail(
            Message originalMessage,
            String htmlContent,
            Map<String, ResourceData> resourceMap,
            BasicBodyFactory bodyFactory) throws IOException {

        // Replace URLs with CID references
        String updatedHtml = htmlContent;
        for (var entry : resourceMap.entrySet()) {
            // Replace exact string match from HTML
            updatedHtml = updatedHtml.replace(entry.getKey(), "cid:" + entry.getValue().contentId());
        }

        Body newBody = rebuildBodyWithUpdatedHtml(originalMessage.getBody(), updatedHtml, resourceMap, bodyFactory);

        MessageImpl newMessage = new MessageImpl();
        newMessage.setHeader(originalMessage.getHeader());
        newMessage.setBody(newBody);

        return newMessage;
    }

    // --- Boilerplate MIME reconstruction (Logic unchanged, just robustified) ---

    private Body rebuildBodyWithUpdatedHtml(Body originalBody, String updatedHtml, Map<String, ResourceData> resourceMap, BasicBodyFactory bodyFactory) throws IOException {
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

    private Body addResourcesToRelated(Multipart originalRelated, String updatedHtml, Map<String, ResourceData> resourceMap, BasicBodyFactory bodyFactory) throws IOException {
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

    private Body wrapInRelatedWithResources(Multipart originalMultipart, String updatedHtml, Map<String, ResourceData> resourceMap, BasicBodyFactory bodyFactory) throws IOException {
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

    private Body createRelatedWithResources(String updatedHtml, Map<String, ResourceData> resourceMap, BasicBodyFactory bodyFactory) throws IOException {
        MultipartBuilder related = MultipartBuilder.create("related");
        BodyPartBuilder htmlPart = BodyPartBuilder.create();
        htmlPart.setBody(updatedHtml, "html", StandardCharsets.UTF_8);
        related.addBodyPart(htmlPart);
        addResourceParts(related, resourceMap, bodyFactory);
        return related.build();
    }

    private void addResourceParts(MultipartBuilder related, Map<String, ResourceData> resourceMap, BasicBodyFactory bodyFactory) {
        Set<String> addedCids = new HashSet<>();
        
        for (ResourceData res : resourceMap.values()) {
            if (addedCids.contains(res.contentId())) continue;
            
            BodyPartBuilder part = BodyPartBuilder.create();
            part.setBody(bodyFactory.binaryBody(res.data()));
            part.setContentTransferEncoding("base64");
            part.setField(new RawField("Content-Type", res.mimeType()));
            part.setContentDisposition("inline; filename=\"" + res.filename() + "\"");
            part.setField(new RawField("Content-ID", "<" + res.contentId() + ">"));
            part.setField(new RawField("X-MatrosDMS-Embedded", "true"));
            
            related.addBodyPart(part);
            addedCids.add(res.contentId());
        }
    }

    private boolean isHtmlPart(Entity part) {
        return "text/html".equalsIgnoreCase(part.getMimeType());
    }

    private void copyHeaders(Entity source, BodyPartBuilder target) {
        if (source.getContentTransferEncoding() != null) target.setContentTransferEncoding(source.getContentTransferEncoding());
        if (source.getCharset() != null) target.setField(new RawField("Content-Type", source.getMimeType() + "; charset=" + source.getCharset()));
    }

    private String extractHtmlContent(Entity entity) throws IOException {
        Body body = entity.getBody();
        if (body instanceof TextBody textBody) {
            if ("text/html".equalsIgnoreCase(entity.getMimeType())) {
                try (Reader reader = textBody.getReader()) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[4096];
                    int bytesRead;
                    while ((bytesRead = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, bytesRead);
                    }
                    return sb.toString();
                }
            }
        } else if (body instanceof Multipart multipart) {
            for (Entity part : multipart.getBodyParts()) {
                String html = extractHtmlContent(part);
                if (html != null) return html;
            }
        }
        return null;
    }

    private String hashBytes(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            return String.valueOf(System.nanoTime());
        }
    }

    record ResourceData(String contentId, String filename, byte[] data, String mimeType) {}
}