/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.journal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.schwehla.matrosdms.entity.management.DBUser;

/**
 * Writes every successful change made through the API to documents, contexts, categories,
 * attribute types and tasks as one JSON line to the change journal: a log next to the database
 * backups ({@code <repository>/journal}, see logback-spring.xml). It never touches the database, so
 * it survives whatever happens to it. After restoring a backup, the journal lines after the
 * backup's time are exactly what is missing.
 *
 * <p>
 * A line is written after the response, for 2xx answers only, so a change that failed or was
 * rolled back never shows up. It carries the request and the response: a create's response holds
 * the id the server gave the new object, which later lines refer to.
 */
@Component
public class ChangeJournalFilter extends OncePerRequestFilter {

	/** The logger logback-spring.xml routes into the journal file, and nowhere else. */
	public static final String JOURNAL_LOGGER = "matrosdms.journal";

	private static final Logger log = LoggerFactory.getLogger(ChangeJournalFilter.class);

	private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

	/** What is filed where. Not logins, users, stores, settings or inbox uploads. */
	private static final List<Pattern> JOURNALED = List.of(
			Pattern.compile("/api/items(/.*)?"),
			Pattern.compile("/api/contexts(/.*)?"),
			Pattern.compile("/api/category(/.*)?"),
			Pattern.compile("/api/attribute-type(/.*)?"),
			Pattern.compile("/api/actions(/.*)?"));

	/** POSTs below those paths that change nothing. */
	private static final List<Pattern> READ_ONLY = List.of(
			Pattern.compile("/api/items/[^/]+/ai/.*"));

	private static final Pattern SECRET_FIELD = Pattern.compile("(?i).*(password|secret|token|credential).*");

	/** Bodies beyond this are journaled cut off, flagged as truncated. */
	static final int MAX_BODY = 64 * 1024;

	/** Not the application's mapper: that one indents, and a journal entry must be one line. */
	private final ObjectMapper json = new ObjectMapper();

	private final boolean enabled;
	private final Consumer<String> journal;

	@Autowired
	public ChangeJournalFilter(@Value("${app.journal.enabled:true}") boolean enabled) {
		this(enabled, LoggerFactory.getLogger(JOURNAL_LOGGER)::info);
	}

	ChangeJournalFilter(boolean enabled, Consumer<String> journal) {
		this.enabled = enabled;
		this.journal = journal;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!enabled || !MUTATING.contains(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return READ_ONLY.stream().anyMatch(p -> p.matcher(path).matches())
				|| JOURNALED.stream().noneMatch(p -> p.matcher(path).matches());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request, MAX_BODY);
		ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);
		try {
			chain.doFilter(req, res);
			if (res.getStatus() >= 200 && res.getStatus() < 300) {
				write(req, res);
			}
		} finally {
			res.copyBodyToResponse();
		}
	}

	private void write(ContentCachingRequestWrapper req, ContentCachingResponseWrapper res) {
		try {
			ObjectNode line = json.createObjectNode();
			line.put("time", OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS).toString());
			line.put("user", user());
			line.put("method", req.getMethod());
			line.put("path", req.getRequestURI());
			if (req.getQueryString() != null) {
				line.put("query", req.getQueryString());
			}
			line.put("status", res.getStatus());
			byte[] request = req.getContentAsByteArray();
			byte[] response = res.getContentAsByteArray();
			// The request cache stops at MAX_BODY (and the length may be unknown); the response is all there
			body(line, "request", request, req.getContentLengthLong() > MAX_BODY || request.length >= MAX_BODY);
			body(line, "response", response, response.length > MAX_BODY);
			journal.accept(json.writeValueAsString(line));
		} catch (Exception e) {
			// The change itself went through; a missing journal line must not turn it into an error
			log.warn("[Journal] Could not record {} {}: {}", req.getMethod(), req.getRequestURI(), e.toString());
		}
	}

	private void body(ObjectNode line, String field, byte[] bytes, boolean truncated) {
		if (bytes.length == 0) {
			return;
		}
		if (!truncated) {
			try {
				line.set(field, redact(json.readTree(bytes)));
				return;
			} catch (IOException notJson) {
				// fall through: keep it as text
			}
		}
		String text = new String(bytes, 0, Math.min(bytes.length, MAX_BODY), StandardCharsets.UTF_8);
		line.put(field, text);
		if (truncated) {
			line.put(field + "Truncated", true);
		}
	}

	private JsonNode redact(JsonNode node) {
		if (node.isObject()) {
			ObjectNode object = (ObjectNode) node;
			for (Map.Entry<String, JsonNode> field : object.properties()) {
				if (SECRET_FIELD.matcher(field.getKey()).matches()) {
					object.put(field.getKey(), "***");
				} else {
					redact(field.getValue());
				}
			}
		} else if (node.isArray()) {
			node.forEach(this::redact);
		}
		return node;
	}

	private static String user() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return null;
		}
		return auth.getPrincipal() instanceof DBUser user ? user.getName() : auth.getName();
	}
}
