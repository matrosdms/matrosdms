/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.cli.session.SessionData;
import net.schwehla.matrosdms.cli.session.SessionStore;

/**
 * Thin HTTP client that wraps the Matrosdms REST API.
 * All calls that require authentication automatically read the JWT from the
 * persisted session.  If the session is missing or expired the user is
 * prompted to run {@code matros login} again.
 */
@Component
public class MatrosApiClient {

    private static final Logger log = LoggerFactory.getLogger(MatrosApiClient.class);

    @Autowired
    private SessionStore sessionStore;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    /**
     * Authenticates against the server and returns a populated SessionData.
     * Does NOT persist the session — that is the caller's responsibility.
     */
    public SessionData login(String serverUrl, String username, String password) throws Exception {
        String url = normalise(serverUrl) + "/api/auth/login";
        String passwordHash = sha256Hex(password);
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", passwordHash));

        log.debug("POST {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new RuntimeException("Invalid username or password.");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Login failed (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonNode node = objectMapper.readTree(response.body());
        String token = node.path("accessToken").asText(null);
        String refreshToken = node.path("refreshToken").asText(null);

        if (token == null || token.isBlank()) {
            throw new RuntimeException("Server did not return an access token. Response: " + response.body());
        }

        return new SessionData(serverUrl, token, refreshToken, username);
    }

    // -------------------------------------------------------------------------
    // Items
    // -------------------------------------------------------------------------

    /**
     * Checks whether a file with the given SHA-256 hash already exists in the DMS.
     *
     * @param hash hex SHA-256 of the file
     * @return true when the file is already stored in the DMS
     */
    public boolean existsByHash(String hash) throws Exception {
        SessionData session = requireSession();
        String url = normalise(session.serverUrl) + "/api/items/exists/" + hash;

        log.debug("GET {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + session.token)
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new RuntimeException(
                    "Authentication error (HTTP " + response.statusCode() + ")."
                    + " Please run 'matros login' again.");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Unexpected API response (HTTP " + response.statusCode()
                    + ") for hash " + hash + ": " + response.body());
        }

        return objectMapper.readTree(response.body()).path("exists").asBoolean();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SessionData requireSession() {
        SessionData session = sessionStore.load();
        if (session == null || session.token == null || session.token.isBlank()) {
            throw new RuntimeException(
                    "No active session found. Run 'matros login --server <url> --user <user>' first.");
        }
        return session;
    }

    /** Strips trailing slashes from a base URL. */
    private static String normalise(String url) {
        return url == null ? "" : url.replaceAll("/+$", "");
    }

    /** Computes the lowercase hex SHA-256 digest of a UTF-8 string. */
    private static String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
