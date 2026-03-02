/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.session;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Persisted session token stored at ~/.matros/session.json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionData {

    public String serverUrl;
    public String token;
    public String refreshToken;
    public String username;
    public String savedAt;

    public SessionData() {
    }

    public SessionData(String serverUrl, String token, String refreshToken, String username) {
        this.serverUrl = serverUrl;
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
        this.savedAt = Instant.now().toString();
    }
}
