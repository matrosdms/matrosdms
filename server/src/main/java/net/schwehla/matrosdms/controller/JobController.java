/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.admin.EJobStatus;
import net.schwehla.matrosdms.entity.admin.DBAdminJob;
import net.schwehla.matrosdms.repository.AdminJobRepository;
import net.schwehla.matrosdms.service.message.JobMessage;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/jobs")
public class JobController {

	@Autowired
	AdminJobRepository adminJobRepository;

	@GetMapping
	@Operation(summary = "Browse job history log")
	public ResponseEntity<Page<JobMessage>> getJobHistory(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {

		// FIX: Map DTO sort properties to Entity properties to prevent
		// "UnknownPathException".
		// The frontend sends 'executionTime', 'taskName', etc. but the DB Entity has
		// 'startTime', 'type'.
		Sort sort = pageable.getSort();
		Sort mappedSort = Sort.unsorted();

		for (Sort.Order order : sort) {
			String prop = order.getProperty();

			// Mapping Logic
			if ("executionTime".equals(prop)) {
				prop = "startTime";
			} else if ("taskName".equals(prop)) {
				prop = "type";
			} else if ("instanceId".equals(prop)) {
				prop = "id";
			}
			// 'status' maps 1:1, so we don't need an else-if for it,
			// but we fallback to the original prop if no match found.

			mappedSort = mappedSort.and(Sort.by(order.getDirection(), prop));
		}

		// Create a new Pageable with the translated Sort
		Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);

		Page<DBAdminJob> page = adminJobRepository.findHistory(from, to, safePageable);

		Page<JobMessage> dtoPage = page.map(this::mapDbEntity);
		return ResponseEntity.ok(dtoPage);
	}

	private JobMessage mapDbEntity(DBAdminJob e) {
		EJobStatus mappedStatus;
		try {
			mappedStatus = EJobStatus.valueOf(e.getStatus().name());
		} catch (Exception ex) {
			mappedStatus = EJobStatus.FAILED;
		}

		return new JobMessage(
				e.getType().name(),
				String.valueOf(e.getId()),
				e.getStartTime() != null
						? e.getStartTime().atZone(ZoneId.systemDefault()).toInstant()
						: null,
				mappedStatus);
	}
}
