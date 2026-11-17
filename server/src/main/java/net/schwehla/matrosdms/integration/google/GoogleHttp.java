/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Minimal JSON-over-HTTPS client for the three Google endpoints used (OAuth, Tasks,
 * Calendar). Plain java.net.http instead of the Google client libraries: two small
 * REST APIs do not justify a dozen transitive jars.
 */
@Component
public class GoogleHttp {

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	// Own mapper: only trees are read and written here, and the application mapper's
	// field-visibility settings are irrelevant for that.
	private final ObjectMapper mapper = new ObjectMapper();

	public ObjectMapper mapper() {
		return mapper;
	}

	public ObjectNode object() {
		return mapper.createObjectNode();
	}

	public JsonNode get(String url, String accessToken) {
		return send(authorized(url, accessToken).GET().build());
	}

	public JsonNode send(String method, String url, String accessToken, JsonNode body) {
		HttpRequest request = authorized(url, accessToken)
				.header("Content-Type", "application/json; charset=UTF-8")
				.method(method, BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
				.build();
		return send(request);
	}

	/** DELETE that treats "already gone" (404/410) as success. */
	public void delete(String url, String accessToken) {
		try {
			send(authorized(url, accessToken).DELETE().build());
		} catch (GoogleApiException e) {
			if (!e.isNotFound()) {
				throw e;
			}
		}
	}

	public JsonNode postForm(String url, Map<String, String> form) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(REQUEST_TIMEOUT)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "application/json")
				.POST(BodyPublishers.ofString(formEncode(form)))
				.build();
		return send(request);
	}

	public static String formEncode(Map<String, String> form) {
		return form.entrySet().stream()
				.map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
				.collect(Collectors.joining("&"));
	}

	/** Encodes a value for a query string or a single path segment. */
	public static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private HttpRequest.Builder authorized(String url, String accessToken) {
		return HttpRequest.newBuilder(URI.create(url))
				.timeout(REQUEST_TIMEOUT)
				.header("Authorization", "Bearer " + accessToken)
				.header("Accept", "application/json");
	}

	private JsonNode send(HttpRequest request) {
		HttpResponse<String> response;
		try {
			response = http.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new GoogleApiException(0, "Google not reachable: " + e, true, null, e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new GoogleApiException(0, "Interrupted while calling Google", true, null, e);
		}

		int status = response.statusCode();
		String body = response.body();
		if (status < 200 || status >= 300) {
			throw GoogleApiException.fromResponse(status, body, mapper);
		}
		if (body == null || body.isBlank()) {
			return MissingNode.getInstance();
		}
		try {
			return mapper.readTree(body);
		} catch (IOException e) {
			throw new GoogleApiException(status, "Unreadable Google response: " + e.getMessage(), true, null, e);
		}
	}
}
