/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.mail.smtp;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.service.mail.common.MailboxManager;
import net.schwehla.matrosdms.service.mail.common.MailboxManager.MailboxFolder;

@Component
public class SmtpServer {

	private static final Logger log = LoggerFactory.getLogger(SmtpServer.class);

	@Value("${app.mail.smtp.port:2525}")
	private int port;

	@Autowired
	MailboxManager mailboxManager;

	@Autowired
	@Qualifier("taskExecutor")
	AsyncTaskExecutor taskExecutor;

	private volatile boolean running;
	private ServerSocket serverSocket;

	@PostConstruct
	public void start() {
		taskExecutor.submit(this::serverLoop);
	}

	private void serverLoop() {
		try {
			serverSocket = new ServerSocket(port);
			running = true;
			log.info("🚀 SMTP Server listening on port {}", port);
			while (running) {
				try {
					Socket client = serverSocket.accept();
					taskExecutor.submit(() -> handleClient(client));
				} catch (IOException e) {
					if (running)
						log.error("SMTP Accept Error", e);
				}
			}
		} catch (IOException e) {
			log.error("Failed to bind SMTP port {}", port, e);
		}
	}

	@PreDestroy
	public void stop() {
		running = false;
		try {
			if (serverSocket != null)
				serverSocket.close();
		} catch (Exception ignored) {
		}
	}

	private void handleClient(Socket socket) {
		Path tempFile = null;
		OutputStream fileOut = null;

		try (socket;
				var reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
				var writer = new PrintWriter(
						new BufferedWriter(
								new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII)),
						true)) {

			writer.println("220 MatrosDMS SMTP Ready");

			boolean dataMode = false;
			String line;
			String from = "unknown";

			// State for AUTH LOGIN sequence
			boolean authLoginUsername = false;
			boolean authLoginPassword = false;

			while ((line = reader.readLine()) != null) {

				// 1. DATA STREAMING MODE
				if (dataMode) {
					if (line.equals(".")) {
						dataMode = false;
						if (fileOut != null)
							fileOut.close();
						finalizeEmail(tempFile, from);
						tempFile = null;
						writer.println("250 OK Message accepted");
					} else {
						if (line.startsWith(".."))
							line = line.substring(1);
						if (fileOut != null) {
							fileOut.write(line.getBytes(StandardCharsets.UTF_8));
							fileOut.write("\r\n".getBytes(StandardCharsets.UTF_8));
						}
					}
					continue;
				}

				// 2. AUTH LOGIN SEQUENCE (Outlook specific)
				if (authLoginUsername) {
					// Client sent username (Base64), ask for password
					writer.println("334 UGFzc3dvcmQ6"); // "Password:" in Base64
					authLoginUsername = false;
					authLoginPassword = true;
					continue;
				}
				if (authLoginPassword) {
					// Client sent password, authenticate
					writer.println("235 2.7.0 Authentication successful");
					authLoginPassword = false;
					continue;
				}

				// 3. COMMAND MODE
				String cmd = line.toUpperCase();

				if (cmd.startsWith("HELO") || cmd.startsWith("EHLO")) {
					writer.println("250-MatrosDMS");
					writer.println("250-8BITMIME");
					writer.println("250-SIZE 52428800"); // Advertise 50MB limit
					writer.println("250 AUTH LOGIN PLAIN"); // Crucial for Outlook/Apple
				} else if (cmd.startsWith("AUTH PLAIN")) {
					// One-line authentication (Thunderbird/Apple)
					// If the line is just "AUTH PLAIN", client waits for 334. If it has data, it's
					// done.
					if (cmd.equals("AUTH PLAIN")) {
						writer.println("334 "); // Send empty challenge
						// Next line will be the credentials, handled in next loop or we can just read
						// it now if
						// we blocked.
						// But strictly, we just say "Success" to whatever they send next.
						authLoginPassword = true; // Hack: reuse logic to accept next line as success
					} else {
						writer.println("235 2.7.0 Authentication successful");
					}
				} else if (cmd.startsWith("AUTH LOGIN")) {
					// Multi-step authentication (Outlook)
					writer.println("334 VXNlcm5hbWU6"); // "Username:" in Base64
					authLoginUsername = true;
				} else if (cmd.startsWith("MAIL FROM:")) {
					from = cmd.substring(10).trim();
					writer.println("250 OK");
				} else if (cmd.startsWith("RCPT TO:")) {
					writer.println("250 OK");
				} else if (cmd.equals("DATA")) {
					dataMode = true;
					MailboxFolder inbox = mailboxManager.resolve("INBOX");
					tempFile = Files.createTempFile(inbox.path(), "smtp-", ".tmp");
					fileOut = new BufferedOutputStream(Files.newOutputStream(tempFile));
					writer.println("354 Start mail input; end with <CRLF>.<CRLF>");
				} else if (cmd.equals("QUIT")) {
					writer.println("221 Bye");
					break;
				} else if (cmd.equals("RSET") || cmd.equals("NOOP")) {
					writer.println("250 OK");
				} else {
					log.debug("Unknown SMTP command: {}", line);
					writer.println("502 Command not implemented");
				}
			}
		} catch (IOException e) {
			log.error("SMTP Connection Error", e);
		} finally {
			if (fileOut != null)
				try {
					fileOut.close();
				} catch (IOException e) {
				}
			if (tempFile != null)
				try {
					Files.deleteIfExists(tempFile);
				} catch (IOException e) {
				}
		}
	}

	private void finalizeEmail(Path tempFile, String from) {
		try {
			MailboxFolder inbox = mailboxManager.resolve("INBOX");
			if (inbox != null && tempFile != null) {
				String newName = UUID.randomUUID().toString() + ".eml";
				Path target = inbox.path().resolve(newName);
				Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
				log.info("📧 SMTP Received from {} -> {}", from, newName);
			}
		} catch (Exception e) {
			log.error("Failed to save email", e);
		}
	}
}
