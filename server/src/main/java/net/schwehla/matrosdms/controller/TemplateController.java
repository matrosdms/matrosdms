/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.template.TemplateProposal;
import net.schwehla.matrosdms.service.domain.TemplateService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/templates")
public class TemplateController {

	@Autowired
	TemplateService templateService;

	@GetMapping
	@Operation(summary = "Get list of available templates")
	public ResponseEntity<List<TemplateProposal>> getAllTemplates() {
		return ResponseEntity.ok(templateService.getAllProposals());
	}

	@GetMapping(value = "/{templateId}", produces = "application/yaml")
	@Operation(summary = "Get the raw template YAML content")
	public ResponseEntity<String> getTemplateContent(
			@PathVariable String templateId,
			@RequestParam(name = "lang", required = false) String language) {

		String content = templateService.getTemplateContent(templateId, language);
		if (content.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		// Return as raw string. The frontend code editor can highlight this as YAML.
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/yaml"))
				.body(content);
	}
}
