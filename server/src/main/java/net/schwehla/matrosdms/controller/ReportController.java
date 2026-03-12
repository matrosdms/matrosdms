/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.core.EArchiveFilter;
import net.schwehla.matrosdms.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class ReportController {

	@Autowired
	ReportService reportService;

	/**
	 * GET /api/report → HTML (default)
	 * GET /api/report?format=csv → CSV download
	 * GET /api/report?archiveState=ARCHIVED_ONLY → archived items only (appends
	 * -archived suffix)
	 */
	@GetMapping("/report")
	@Operation(summary = "ReportController")
	public ResponseEntity<String> report(
			@RequestParam(name = "format", defaultValue = "html") String format,
			@RequestParam(name = "archiveState", defaultValue = "ACTIVE_ONLY") EArchiveFilter archiveState) {

		String suffix = archiveState == EArchiveFilter.ARCHIVED_ONLY ? "-archived" : "";

		return switch (format.toLowerCase()) {
			case "html" -> ResponseEntity.ok()
					.contentType(new MediaType("text", "html",
							java.nio.charset.StandardCharsets.UTF_8))
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=\"report" + suffix + ".html\"")
					.body(reportService.generateHtmlReport(archiveState));

			default -> ResponseEntity.ok()
					.contentType(new MediaType("text", "csv",
							java.nio.charset.StandardCharsets.UTF_8))
					.header(HttpHeaders.CONTENT_DISPOSITION,
							"attachment; filename=\"report" + suffix + ".csv\"")
					.body(reportService.generateCsvReport(archiveState));
		};
	}
}
