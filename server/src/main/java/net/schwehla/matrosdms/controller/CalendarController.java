/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.action.MAction;
import net.schwehla.matrosdms.repository.ActionRepository;
import net.schwehla.matrosdms.service.mapper.MActionMapper;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/cal")
public class CalendarController {

	@Autowired
	ActionRepository actionRepository;
	@Autowired
	MActionMapper actionMapper;

	// iCal Date Format: 20260117T093000Z
	private static final DateTimeFormatter ICAL_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

	@GetMapping(value = "/actions.ics", produces = "text/calendar")
	@Operation(summary = "Export Actions as iCal feed")
	public ResponseEntity<String> getIcal(@RequestParam(required = false) String user) {

		List<MAction> actions = actionMapper.map(actionRepository.findAll());

		StringBuilder ical = new StringBuilder();
		ical.append("BEGIN:VCALENDAR\n");
		ical.append("VERSION:2.0\n");
		ical.append("PRODID:-//MatrosDMS//Actions//EN\n");
		ical.append("CALSCALE:GREGORIAN\n");
		ical.append("X-WR-CALNAME:MatrosDMS Actions\n");

		for (MAction action : actions) {
			if (action.getStatus().isCompleted())
				continue;
			if (action.getDueDate() == null)
				continue;

			// Filter by user if requested
			if (user != null && !user.equals(action.getAssignee().getUuid()))
				continue;

			ical.append("BEGIN:VEVENT\n");
			ical.append("UID:").append(action.getUuid()).append("\n");
			ical.append("DTSTAMP:").append(formatDate(LocalDateTime.now())).append("Z\n");

			// Assuming 1 hour duration for tasks
			ical.append("DTSTART:").append(formatDate(action.getDueDate())).append("\n");
			ical.append("DTEND:").append(formatDate(action.getDueDate().plusHours(1))).append("\n");

			ical.append("SUMMARY:").append(escape(action.getName())).append("\n");
			if (action.getDescription() != null) {
				ical.append("DESCRIPTION:").append(escape(action.getDescription())).append("\n");
			}

			ical.append("STATUS:CONFIRMED\n");
			ical.append("END:VEVENT\n");
		}

		ical.append("END:VCALENDAR");

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"matros_actions.ics\"")
				.contentType(MediaType.parseMediaType("text/calendar"))
				.body(ical.toString());
	}

	private String formatDate(LocalDateTime dt) {
		return dt.format(ICAL_FMT);
	}

	private String escape(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
	}
}
