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

import net.schwehla.matrosdms.service.mail.common.MailAuthenticator;
import net.schwehla.matrosdms.service.mail.common.MailboxManager;
import net.schwehla.matrosdms.service.mail.common.MailboxManager.MailboxFolder;

@Component
public class SmtpServer {

	private static final Logger log = LoggerFactory.getLogger(SmtpServer.class);

	/** Advertised in EHLO and actually enforced during DATA. */
	static final long MAX_MESSAGE_BYTES = 52428800L; // 50 MB

	@Value("${app.mail.smtp.port:2525}")
	private int port;

	// Local-first: loopback by default (local Thunderbird/scanner-to-localhost).
	// Set MATROS_MAIL_BIND=0.0.0.0 to accept clients from other hosts - and if
	// you do, also set app.mail.require-auth=true.
	@Value("${app.mail.bind-address:${MATROS_MAIL_BIND:127.0.0.1}}")
	private String bindAddress;

	// Off by default: on a loopback-only server real credential checks add
	// little, and forcing them would break existing local mail client profiles
	@Value("${app.mail.require-auth:false}")
	private boolean requireAuth;

	@Autowired
	MailAuthenticator mailAuthenticator;

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
			serverSocket = new ServerSocket(port, 50, InetAddress.getByName(bindAddress));
			running = true;
			log.info("🚀 SMTP Server listening on {}:{} (auth required: {})", bindAddress, port, requireAuth);
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

			boolean authenticated = false;
			String pendingAuthUser = null;

			// States for the AUTH dialogues (next line is a base64 payload)
			boolean authLoginUsername = false;
			boolean authLoginPassword = false;
			boolean authPlainData = false;

			long dataBytes = 0;
			boolean dataOverflow = false;

			while ((line = reader.readLine()) != null) {

				// 1. DATA STREAMING MODE
				if (dataMode) {
					if (line.equals(".")) {
						dataMode = false;
						if (fileOut != null)
							fileOut.close();
						if (dataOverflow) {
							Files.deleteIfExists(tempFile);
							tempFile = null;
							log.warn("SMTP message from {} rejected: exceeds {} bytes", from, MAX_MESSAGE_BYTES);
							writer.println("552 Message size exceeds maximum of " + MAX_MESSAGE_BYTES + " bytes");
						} else {
							finalizeEmail(tempFile, from);
							tempFile = null;
							writer.println("250 OK Message accepted");
						}
					} else {
						if (line.startsWith(".."))
							line = line.substring(1);
						byte[] lineBytes = line.getBytes(StandardCharsets.UTF_8);
						dataBytes += lineBytes.length + 2;
						if (dataBytes > MAX_MESSAGE_BYTES) {
							// Keep consuming until the terminating "." but stop persisting
							dataOverflow = true;
						} else if (fileOut != null) {
							fileOut.write(lineBytes);
							fileOut.write("\r\n".getBytes(StandardCharsets.UTF_8));
						}
					}
					continue;
				}

				// 2. AUTH DIALOGUE STATES (payloads are case-sensitive base64 - use raw line)
				if (authLoginUsername) {
					pendingAuthUser = mailAuthenticator.decodeBase64(line);
					writer.println("334 UGFzc3dvcmQ6"); // "Password:" in Base64
					authLoginUsername = false;
					authLoginPassword = true;
					continue;
				}
				if (authLoginPassword) {
					authLoginPassword = false;
					String password = mailAuthenticator.decodeBase64(line);
					if (!requireAuth || mailAuthenticator.authenticate(pendingAuthUser, password)) {
						authenticated = true;
						writer.println("235 2.7.0 Authentication successful");
					} else {
						writer.println("535 5.7.8 Authentication credentials invalid");
					}
					pendingAuthUser = null;
					continue;
				}
				if (authPlainData) {
					authPlainData = false;
					if (!requireAuth || mailAuthenticator.authenticatePlain(line)) {
						authenticated = true;
						writer.println("235 2.7.0 Authentication successful");
					} else {
						writer.println("535 5.7.8 Authentication credentials invalid");
					}
					continue;
				}

				// 3. COMMAND MODE
				String cmd = line.toUpperCase();

				if (cmd.startsWith("HELO") || cmd.startsWith("EHLO")) {
					writer.println("250-MatrosDMS");
					writer.println("250-8BITMIME");
					writer.println("250-SIZE " + MAX_MESSAGE_BYTES);
					writer.println("250 AUTH LOGIN PLAIN"); // Crucial for Outlook/Apple
				} else if (cmd.startsWith("AUTH PLAIN")) {
					// One-line authentication (Thunderbird/Apple); base64 from RAW line
					String payload = line.length() > 10 ? line.substring(10).trim() : "";
					if (payload.isEmpty()) {
						writer.println("334 "); // Empty challenge, credentials on next line
						authPlainData = true;
					} else if (!requireAuth || mailAuthenticator.authenticatePlain(payload)) {
						authenticated = true;
						writer.println("235 2.7.0 Authentication successful");
					} else {
						writer.println("535 5.7.8 Authentication credentials invalid");
					}
				} else if (cmd.startsWith("AUTH LOGIN")) {
					// Multi-step authentication (Outlook)
					writer.println("334 VXNlcm5hbWU6"); // "Username:" in Base64
					authLoginUsername = true;
				} else if (cmd.startsWith("MAIL FROM:")) {
					if (requireAuth && !authenticated) {
						writer.println("530 5.7.0 Authentication required");
					} else {
						from = cmd.substring(10).trim();
						writer.println("250 OK");
					}
				} else if (cmd.startsWith("RCPT TO:")) {
					if (requireAuth && !authenticated) {
						writer.println("530 5.7.0 Authentication required");
					} else {
						writer.println("250 OK");
					}
				} else if (cmd.equals("DATA")) {
					if (requireAuth && !authenticated) {
						writer.println("530 5.7.0 Authentication required");
					} else {
						dataMode = true;
						dataBytes = 0;
						dataOverflow = false;
						MailboxFolder inbox = mailboxManager.resolve("INBOX");
						tempFile = Files.createTempFile(inbox.path(), "smtp-", ".tmp");
						fileOut = new BufferedOutputStream(Files.newOutputStream(tempFile));
						writer.println("354 Start mail input; end with <CRLF>.<CRLF>");
					}
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
