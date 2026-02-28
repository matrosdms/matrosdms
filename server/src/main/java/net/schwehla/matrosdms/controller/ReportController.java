/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/report")
@Tag(name = "Reports", description = "Export data for external use")
public class ReportController {

	@Autowired
	ReportService reportService;

	@GetMapping(value = "/items.csv")
	@Operation(summary = "Download complete inventory as CSV")
	public ResponseEntity<byte[]> getItemReport() {

		String csvContent = reportService.generateCsvReport();
		byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

		String filename = "matros_inventory_" + LocalDate.now() + ".csv";

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
				.contentLength(csvBytes.length)
				.body(csvBytes);
	}
}