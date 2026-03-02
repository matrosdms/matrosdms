/*
 * Copyright (c) 2026 Matrosdms
 */
package net.schwehla.matrosdms.cli.command;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.cli.client.MatrosApiClient;
import net.schwehla.matrosdms.cli.session.SessionData;
import net.schwehla.matrosdms.cli.session.SessionStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Authenticates against a Matrosdms server and saves the session to
 * {@code ~/.matros/session.json} so subsequent commands do not need to log in
 * again.
 *
 * <pre>
 *   matros login --server http://localhost:8080 --user admin --password secret
 * </pre>
 */
@Component
@Command(
        name = "login",
        description = "Login to a Matrosdms server and persist the session.",
        mixinStandardHelpOptions = true)
public class LoginCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(LoginCommand.class);

    @Option(names = {"--server", "-s"}, required = true,
            description = "Server base URL (e.g. http://localhost:8080)")
    private String serverUrl;

    @Option(names = {"--user", "-u"}, required = true,
            description = "Username / login")
    private String username;

    @Option(names = {"--password", "-p"}, required = true, interactive = true, arity = "0..1",
            description = "Password (will be prompted interactively if not supplied)")
    private String password;

    @Autowired
    private MatrosApiClient apiClient;

    @Autowired
    private SessionStore sessionStore;

    @Override
    public Integer call() {
        log.info("Authenticating as '{}' @ {}", username, serverUrl);
        try {
            SessionData session = apiClient.login(serverUrl, username, password);
            sessionStore.save(session);
            log.info("Login successful — session stored at {}", SessionStore.SESSION_FILE);
            System.out.println("✔  Logged in as '" + username + "' @ " + serverUrl);
            System.out.println("   Session saved to: " + SessionStore.SESSION_FILE);
            return 0;
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            System.err.println("✘  Login failed: " + e.getMessage());
            return 1;
        }
    }
}
