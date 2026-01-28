/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.ai.strategy;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.ai.IPredictionStrategy;
import net.schwehla.matrosdms.domain.ai.ClassificationCandidates;
import net.schwehla.matrosdms.domain.inbox.Prediction;
import net.schwehla.matrosdms.service.message.DigestResultMessage;

@Component
public class HeuristicPredictionStrategy implements IPredictionStrategy {

	private static final Logger log = LoggerFactory.getLogger(HeuristicPredictionStrategy.class);
	private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2,4})");

	@Override
	public String getId() {
		return "heuristic";
	}

	@Override
	public void analyze(
			String fullText,
			String filename,
			ClassificationCandidates candidates,
			DigestResultMessage result) {
		long start = System.currentTimeMillis();

		if (fullText == null)
			return;

		Prediction p = result.getPrediction();

		// 1. Context Matching (Folder Name in Filename?)
		for (var context : candidates.contexts()) {
			if (filename.toLowerCase().contains(context.name().toLowerCase())) {
				p.setContext(context.uuid());
				p.setSummary("Matched folder name '" + context.name() + "' in filename");
				break;
			}
		}

		// 2. Date Extraction (Regex)
		p.setDocumentDate(findBestDate(fullText));

		long duration = System.currentTimeMillis() - start;
		if (duration > 100) {
			log.warn("Heuristic analysis took {}ms for {}", duration, filename);
		} else {
			log.debug("Heuristic analysis took {}ms", duration);
		}
	}

	private LocalDate findBestDate(String text) {
		Matcher m = DATE_PATTERN.matcher(text);
		if (m.find()) {
			try {
				int day = Integer.parseInt(m.group(1));
				int month = Integer.parseInt(m.group(2));
				int year = Integer.parseInt(m.group(3));
				if (year < 100)
					year += 2000;
				return LocalDate.of(year, month, day);
			} catch (Exception e) {
			}
		}
		return null;
	}
}
