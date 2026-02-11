/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline.steps;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.domain.attribute.ESystemAttribute;
import net.schwehla.matrosdms.domain.core.EItemSource;
import net.schwehla.matrosdms.domain.inbox.EmailMetadata;
import net.schwehla.matrosdms.domain.inbox.Prediction;
import net.schwehla.matrosdms.service.pipeline.PipelineContext;
import net.schwehla.matrosdms.service.pipeline.PipelineEvents.PipelineStatusEvent;
import net.schwehla.matrosdms.service.pipeline.PipelineStep;

@Component
@Order(3) // Runs after DuplicateCheckStep (Order 2)
public class MetadataExtractionStep implements PipelineStep {

	@Override
	public void execute(PipelineContext ctx) throws Exception {
		ctx.log("Analyzing Metadata...");

		String filename = ctx.getOriginalFile().getFileName().toString().toLowerCase();

		if (filename.endsWith(".eml")) {
			ctx.getCurrentState().setSource(EItemSource.EMAIL);
			ctx.setMimeType("message/rfc822");
			handleEmail(ctx);
		} else if (filename.endsWith(".pdf")) {
			ctx.setMimeType("application/pdf");
			handlePdf(ctx);
		}

		if (ctx.getPublisher() != null) {
			ctx.getPublisher().publishEvent(new PipelineStatusEvent(ctx.getCurrentState()));
		}
	}

	private void handleEmail(PipelineContext ctx) {
		try (InputStream is = new FileInputStream(ctx.getOriginalFile().toFile())) {
			Session session = Session.getDefaultInstance(new Properties());
			MimeMessage msg = new MimeMessage(session, is);

			String subject = cleanText(msg.getSubject());
			if (subject.isEmpty())
				subject = "No Subject";

			String from = "Unknown";
			if (msg.getFrom() != null && msg.getFrom().length > 0) {
				from = cleanText(msg.getFrom()[0].toString());
			}

			LocalDateTime sentDate = null;
			if (msg.getSentDate() != null) {
				sentDate = msg.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			}

			List<String> recipients = new ArrayList<>();
			if (msg.getAllRecipients() != null) {
				for (var a : msg.getAllRecipients())
					recipients.add(cleanText(a.toString()));
			}

			EmailMetadata meta = new EmailMetadata(from, subject, sentDate);
			meta.setRecipients(recipients);
			ctx.getCurrentState().setEmailInfo(meta);

			populateAttributes(ctx, from, String.join(", ", recipients));

			if (sentDate != null) {
				getOrCreatePrediction(ctx).setDocumentDate(sentDate.toLocalDate());
			}

		} catch (Exception e) {
			ctx.addWarning("Email metadata extraction failed: " + e.getMessage());
		}
	}

	private void handlePdf(PipelineContext ctx) {
		try (PDDocument doc = Loader.loadPDF(ctx.getOriginalFile().toFile())) {
			if (!doc.isEncrypted()) {
				PDDocumentInformation info = doc.getDocumentInformation();
				// PDF title handling
			}
		} catch (Exception e) {
		}
	}

	private void populateAttributes(PipelineContext ctx, String sender, String recipient) {
		Prediction pred = getOrCreatePrediction(ctx);
		if (pred.getAttributes() == null)
			pred.setAttributes(new HashMap<>());

		if (sender != null && !sender.isEmpty()) {
			pred.getAttributes().put(ESystemAttribute.SENDER.getUuid(), sender);
		}
		if (recipient != null && !recipient.isEmpty()) {
			pred.getAttributes().put(ESystemAttribute.RECIPIENT.getUuid(), recipient);
		}
	}

	private Prediction getOrCreatePrediction(PipelineContext ctx) {
		if (ctx.getAiResult().getPrediction() == null) {
			ctx.getAiResult().setPrediction(new Prediction());
		}
		return ctx.getAiResult().getPrediction();
	}

	private String cleanText(String input) {
		if (input == null)
			return "";
		try {
			return MimeUtility.decodeText(input).trim();
		} catch (Exception e) {
			return input.trim();
		}
	}
}
