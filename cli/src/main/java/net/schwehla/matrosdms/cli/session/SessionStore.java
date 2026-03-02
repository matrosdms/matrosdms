/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.session;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Reads and writes the CLI session to ~/.matros/session.json.
 * A session holds the JWT access token, refresh token, and server URL
 * so subsequent commands do not require re-authentication.
 */
@Component
public class SessionStore {

    private static final Logger log = LoggerFactory.getLogger(SessionStore.class);

    public static final Path SESSION_FILE =
            Paths.get(System.getProperty("user.home"), ".matros", "session.json");

    private final ObjectMapper objectMapper =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** Persist a session to disk. */
    public void save(SessionData data) {
        try {
            Files.createDirectories(SESSION_FILE.getParent());
            objectMapper.writeValue(SESSION_FILE.toFile(), data);
            log.debug("Session written to {}", SESSION_FILE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save session to " + SESSION_FILE + ": " + e.getMessage(), e);
        }
    }

    /** Load the persisted session, or null if none exists. */
    public SessionData load() {
        if (!Files.exists(SESSION_FILE)) {
            return null;
        }
        try {
            return objectMapper.readValue(SESSION_FILE.toFile(), SessionData.class);
        } catch (Exception e) {
            log.warn("Unable to read session file {}: {}", SESSION_FILE, e.getMessage());
            return null;
        }
    }

    /** Delete the session file (logout). */
    public void clear() {
        try {
            Files.deleteIfExists(SESSION_FILE);
            log.info("Session cleared ({})", SESSION_FILE);
        } catch (Exception e) {
            log.warn("Could not remove session file: {}", e.getMessage());
        }
    }

    public boolean hasSession() {
        return Files.exists(SESSION_FILE);
    }
}
