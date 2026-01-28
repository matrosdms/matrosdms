/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.mail.imap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class ImapServer {

	private static final Logger log = LoggerFactory.getLogger(ImapServer.class);

	// Regex handles: APPEND "Name" (\Flags) "Date" {size}
	private static final Pattern APPEND_PATTERN = Pattern
			.compile("(?i)APPEND\\s+(?:\"([^\"]+)\"|([^\\s]+)).*?\\{(\\d+)\\}");

	// COPY <sequence> <destination>
	private static final Pattern COPY_PATTERN = Pattern
			.compile("(?i)COPY\\s+(\\d+(?:,\\d+)?)\\s+(?:\"([^\"]+)\"|([^\\s]+))");

	@Value("${app.mail.imap.port:1143}")
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
			log.info("🚀 IMAP Server listening on port {}", port);
			while (running) {
				try {
					Socket client = serverSocket.accept();
					taskExecutor.submit(() -> handleClient(client));
				} catch (IOException e) {
					if (running)
						log.error("IMAP Accept Error", e);
				}
			}
		} catch (IOException e) {
			log.error("Failed to bind IMAP port {}", port, e);
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
		try (socket;
				// FIX: Use UTF-8 for Reader
				var reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
				// FIX: Use UTF-8 for Writer
				var writer = new PrintWriter(
						new OutputStreamWriter(
								new BufferedOutputStream(socket.getOutputStream()), StandardCharsets.UTF_8),
						true)) {

			send(
					writer,
					"* OK [CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN NAMESPACE ID CHILDREN] MatrosDMS Ready");

			boolean authenticated = false;
			MailboxFolder currentFolder = null;
			List<Path> messageCache = new ArrayList<>();

			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(" ", 3);
				if (parts.length < 2)
					continue;

				String tag = parts[0];
				String cmd = parts[1].toUpperCase();
				String args = parts.length > 2 ? parts[2] : "";

				try {
					if (cmd.equals("UID")) {
						String[] subParts = args.split(" ", 2);
						cmd = subParts[0].toUpperCase();
						args = subParts.length > 1 ? subParts[1] : "";
					}

					switch (cmd) {
						case "CAPABILITY" -> {
							send(writer, "* CAPABILITY IMAP4rev1 AUTH=PLAIN NAMESPACE ID CHILDREN");
							send(writer, tag + " OK CAPABILITY completed");
						}
						case "LOGIN", "AUTHENTICATE" -> {
							authenticated = true;
							if (cmd.equals("AUTHENTICATE")
									&& args.contains("PLAIN")
									&& !args.trim().endsWith("=")) {
								send(writer, "+");
								reader.readLine();
							}
							send(writer, tag + " OK LOGIN completed");
						}
						case "LOGOUT" -> {
							send(writer, "* BYE");
							send(writer, tag + " OK LOGOUT completed");
							return;
						}
						case "NAMESPACE" -> {
							if (!checkAuth(writer, tag, authenticated))
								break;
							send(writer, "* NAMESPACE ((\"\" \"/\")) NIL NIL");
							send(writer, tag + " OK NAMESPACE completed");
						}
						case "ID" -> {
							send(writer, "* ID (\"name\" \"MatrosDMS\" \"version\" \"2.0\")");
							send(writer, tag + " OK ID completed");
						}
						case "LIST", "LSUB" -> {
							if (!checkAuth(writer, tag, authenticated))
								break;
							send(writer, "* LIST (\\HasNoChildren) \"/\" \"INBOX\"");
							send(writer, "* LIST (\\HasNoChildren) \"/\" \"Staging\"");
							send(writer, tag + " OK LIST completed");
						}
						case "SELECT", "EXAMINE" -> {
							if (!checkAuth(writer, tag, authenticated))
								break;
							// Remove quotes for UTF-8 folder names
							String folderName = args.replace("\"", "");
							currentFolder = mailboxManager.resolve(folderName);
							if (currentFolder != null) {
								messageCache = mailboxManager.listMessages(currentFolder);
								send(writer, "* " + messageCache.size() + " EXISTS");
								send(writer, "* " + messageCache.size() + " RECENT");
								send(writer, "* FLAGS (\\Seen \\Answered \\Flagged \\Deleted \\Draft)");
								send(writer, "* OK [UIDVALIDITY 1] UIDs valid");
								send(writer, tag + " OK [READ-WRITE] SELECT completed");
							} else {
								send(writer, tag + " NO Mailbox not found");
							}
						}
						case "COPY" -> {
							if (!checkAuth(writer, tag, authenticated))
								break;
							if (currentFolder == null) {
								send(writer, tag + " NO Select folder first");
								break;
							}

							Matcher m = COPY_PATTERN.matcher(cmd + " " + args);
							if (m.find()) {
								int msgSeq = Integer.parseInt(m.group(1));
								String destName = m.group(2) != null ? m.group(2) : m.group(3);

								MailboxFolder dest = mailboxManager.resolve(destName);
								if (dest != null && msgSeq > 0 && msgSeq <= messageCache.size()) {
									Path sourceMsg = messageCache.get(msgSeq - 1);
									Path targetMsg = dest.path().resolve(sourceMsg.getFileName());
									Files.copy(sourceMsg, targetMsg, StandardCopyOption.REPLACE_EXISTING);
									send(writer, tag + " OK COPY completed");
								} else {
									send(writer, tag + " NO Invalid sequence or destination");
								}
							} else {
								send(writer, tag + " BAD Invalid COPY args");
							}
						}
						case "EXPUNGE" -> send(writer, tag + " OK EXPUNGE completed");
						case "STORE" -> {
							send(writer, "* 1 FETCH (FLAGS (\\Deleted \\Seen))");
							send(writer, tag + " OK STORE completed");
						}
						case "NOOP" -> send(writer, tag + " OK NOOP");
						case "APPEND" -> {
							if (!checkAuth(writer, tag, authenticated))
								break;
							handleAppend(reader, writer, line, tag);
						}
						default -> send(writer, tag + " OK " + cmd + " completed");
					}
				} catch (Exception e) {
					log.error("IMAP Error: " + line, e);
					send(writer, tag + " BAD Server Error");
				}
			}
		} catch (IOException e) {
		}
	}

	private boolean checkAuth(PrintWriter writer, String tag, boolean authenticated) {
		if (!authenticated) {
			send(writer, tag + " NO Auth required");
			return false;
		}
		return true;
	}

	private void handleAppend(BufferedReader reader, PrintWriter writer, String line, String tag)
			throws IOException {
		Matcher m = APPEND_PATTERN.matcher(line);
		if (m.find()) {
			String targetName = m.group(1) != null ? m.group(1) : m.group(2);
			int size = Integer.parseInt(m.group(3));

			send(writer, "+ Ready for literal data");

			// Read char-by-char to handle multi-byte UTF-8 chars correctly if size is in
			// bytes
			// IMAP size is strictly OCTETS (Bytes).
			// BufferedReader reads CHARACTERS. This is tricky.
			// Correct way for strict IMAP is reading raw InputStream for body, but sticking
			// to Reader for
			// commands.
			// Simplified: We read 'size' characters for now, assuming 1 char = 1 byte for
			// base64/quoted-printable.
			// If raw 8bit UTF-8 is sent, size might mismatch.

			char[] buffer = new char[size];
			int read = 0;
			while (read < size) {
				int count = reader.read(buffer, read, size - read);
				if (count == -1)
					break;
				read += count;
			}

			// FIX: Write as UTF-8
			byte[] data = new String(buffer).getBytes(StandardCharsets.UTF_8);

			MailboxFolder target = mailboxManager.resolve(targetName);
			if (target != null) {
				mailboxManager.storeMessage(target, data);
				send(writer, tag + " OK [APPENDUID 1 1] APPEND completed");
			} else {
				send(writer, tag + " NO Folder not found");
			}
		} else {
			send(writer, tag + " BAD Invalid APPEND arguments");
		}
	}

	private void send(PrintWriter writer, String msg) {
		log.debug("IMAP >> {}", msg);
		writer.println(msg);
	}
}
