/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.mail.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.service.domain.UserService;

/**
 * Verifies SMTP/IMAP credentials against the real DMS user database.
 * The embedded mail servers previously accepted ANY credentials.
 */
@Component
public class MailAuthenticator {

	private static final Logger log = LoggerFactory.getLogger(MailAuthenticator.class);

	@Autowired
	UserService userService;

	public boolean authenticate(String username, String rawPassword) {
		if (username == null || username.isBlank() || rawPassword == null) {
			return false;
		}
		try {
			userService.login(username, rawPassword);
			return true;
		} catch (Exception e) {
			log.warn("Mail authentication failed for user '{}'", username);
			return false;
		}
	}

	/** SASL PLAIN: base64("authzid \0 authcid \0 password"). */
	public boolean authenticatePlain(String base64Blob) {
		String decoded = decodeBase64(base64Blob);
		if (decoded == null) {
			return false;
		}
		String[] parts = decoded.split("\0");
		if (parts.length < 2) {
			return false;
		}
		return authenticate(parts[parts.length - 2], parts[parts.length - 1]);
	}

	/** Returns null instead of throwing on malformed base64. */
	public String decodeBase64(String value) {
		if (value == null) {
			return null;
		}
		try {
			return new String(Base64.getDecoder().decode(value.trim()), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
