/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.inbox;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Specific metadata for Email sources")
public class EmailMetadata implements Serializable {

	private String sender;
	private List<String> recipients;
	private String subject;
	private LocalDateTime sentDate;
	private String messageId;

	public EmailMetadata() {
	}

	public EmailMetadata(String sender, String subject, LocalDateTime sentDate) {
		this.sender = sender;
		this.subject = subject;
		this.sentDate = sentDate;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public List<String> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<String> recipients) {
		this.recipients = recipients;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public LocalDateTime getSentDate() {
		return sentDate;
	}

	public void setSentDate(LocalDateTime sentDate) {
		this.sentDate = sentDate;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
}
