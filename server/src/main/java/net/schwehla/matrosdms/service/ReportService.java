/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.domain.core.EArchiveFilter;
import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.repository.ItemRepository;

@Service
public class ReportService {

	@Autowired
	ItemRepository itemRepository;

	private static final String SEP = ";";
	private static final String LINE_END = "\n";

	// German/ISO date format for Excel compatibility
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Transactional(readOnly = true)
	public String generateCsvReport(EArchiveFilter archiveState) {
		List<DBItem> items = resolveItems(archiveState);
		StringBuilder csv = new StringBuilder();

		// 1. BOM for Excel UTF-8 recognition
		csv.append("\uFEFF");

		// 2. Header
		csv.append("ID").append(SEP)
				.append("Context").append(SEP)
				.append("Name").append(SEP)
				.append("Description").append(SEP)
				.append("Issue Date").append(SEP)
				.append("Store").append(SEP)
				.append("LfdNr").append(SEP) // Binder Number
				.append("Tags").append(SEP)
				.append("UUID").append(SEP)
				.append("Filename")
				.append(LINE_END);

		// 3. Data Rows
		for (DBItem item : items) {
			csv.append(escape(item.getId())).append(SEP);
			String context = item.getInfoContext() != null ? item.getInfoContext().getName() : "";
			csv.append(escape(context)).append(SEP);
			csv.append(escape(item.getName())).append(SEP);
			csv.append(escape(item.getDescription())).append(SEP);
			String date = item.getIssueDate() != null ? item.getIssueDate().format(DATE_FMT) : "";
			csv.append(date).append(SEP);
			String store = item.getStore() != null ? item.getStore().getShortname() : "";
			csv.append(escape(store)).append(SEP);
			csv.append(escape(item.getStorageItemIdentifier())).append(SEP);
			String tags = item.getKindList().stream()
					.map(DBCategory::getName)
					.collect(Collectors.joining(", "));
			csv.append(escape(tags)).append(SEP);
			csv.append(item.getUuid()).append(SEP);
			String filename = item.getFile() != null ? item.getFile().getFilename() : "";
			csv.append(escape(filename)).append(LINE_END);
		}

		return csv.toString();
	}

	@Transactional(readOnly = true)
	public String generateHtmlReport(EArchiveFilter archiveState) {
		List<DBItem> items = resolveItems(archiveState);
		return new HtmlReportExporter().export(items);
	}

	private List<DBItem> resolveItems(EArchiveFilter archiveState) {
		return switch (archiveState) {
			case ARCHIVED_ONLY -> itemRepository.findAllArchivedForReport();
			case ALL -> itemRepository.findAllForReport();
			default -> itemRepository.findAllActiveForReport();
		};
	}

	/**
	 * CSV Escaping:
	 * 1. Wraps content in quotes if it contains separator, newline or quotes.
	 * 2. Escapes existing quotes by doubling them (" -> "").
	 */
	/* package-private for testing */ String escape(Object raw) {
		if (raw == null)
			return "";
		String val = String.valueOf(raw);
		// Formula injection guard: spreadsheet apps execute cells starting with = + - @
		// (real numbers are exempt so negative amounts stay sortable)
		if (!(raw instanceof Number) && !val.isEmpty() && "=+-@".indexOf(val.charAt(0)) >= 0) {
			val = "'" + val;
		}
		if (val.contains(SEP) || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
			val = val.replace("\"", "\"\"");
			return "\"" + val + "\"";
		}
		return val;
	}
}
