/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import net.schwehla.matrosdms.domain.api.EPipelineStatus;
import net.schwehla.matrosdms.domain.inbox.InboxFile;
import net.schwehla.matrosdms.manager.InboxFileManager;
import net.schwehla.matrosdms.service.InboxPipelineService;
import net.schwehla.matrosdms.service.message.PipelineStatusMessage;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class InboxController {

	@Autowired
	InboxFileManager inboxManager;
	@Autowired
	InboxPipelineService pipelineService;

	Logger log = LoggerFactory.getLogger(InboxController.class);

	@PostMapping("/upload")
	public ResponseEntity<InboxFile> uploadFile(@RequestParam("file") MultipartFile file) {
		InboxFile inboxFile = inboxManager.uploadFile(file);
		return new ResponseEntity<>(inboxFile, HttpStatus.CREATED);
	}

	@GetMapping("/inbox")
	@Operation(summary = "Get all Inbox-Files (Live State)")
	public ResponseEntity<List<InboxFile>> loadInboxList() {
		return new ResponseEntity<>(inboxManager.loadInboxList(), HttpStatus.OK);
	}

	@GetMapping("/inbox/{hash}/status")
	public ResponseEntity<InboxFile> getFileStatus(@PathVariable("hash") String hash) {
		InboxFile file = inboxManager.getInboxFileDto(hash);
		return file != null ? ResponseEntity.ok(file) : ResponseEntity.notFound().build();
	}

	@PostMapping("/inbox/{hash}/digest")
	public ResponseEntity<Object> startDigest(@PathVariable("hash") String hash) {
		InboxFile file = inboxManager.getInboxFileDto(hash);
		if (file == null)
			return ResponseEntity.notFound().build();

		if (file.getStatus() == EPipelineStatus.READY) {
			PipelineStatusMessage result = pipelineService.getOrWaitForResult(hash);
			if (result != null)
				return ResponseEntity.ok(result);
		}

		if (file.getStatus() == EPipelineStatus.PROCESSING) {
			return ResponseEntity.status(HttpStatus.ACCEPTED).body("Processing...");
		}

		try {
			pipelineService.triggerPipeline(hash);
			return ResponseEntity.accepted().build();
		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/inbox/{hash}/ignore")
	public ResponseEntity<Void> ignoreFile(@PathVariable("hash") String hash) {
		inboxManager.ignoreFile(hash);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@GetMapping("/inbox/{hash}/content")
	public ResponseEntity<Resource> viewInboxFile(@PathVariable("hash") String hash) {
		try {
			Path path = inboxManager.getInboxFile(hash);
			Resource resource = new UrlResource(path.toUri());

			if (resource.exists() && resource.isReadable()) {
				InboxFile inboxFile = inboxManager.getInboxFileDto(hash);
				String filename = (inboxFile != null && inboxFile.getFileInfo() != null
						&& inboxFile.getFileInfo().getOriginalFilename() != null)
								? inboxFile.getFileInfo().getOriginalFilename()
								: resource.getFilename();

				String contentType = (inboxFile != null && inboxFile.getFileInfo() != null
						&& inboxFile.getFileInfo().getContentType() != null)
								? inboxFile.getFileInfo().getContentType()
								: "application/octet-stream";

				if (filename.toLowerCase().endsWith(".pdf"))
					contentType = "application/pdf";
				else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg"))
					contentType = "image/jpeg";
				else if (filename.toLowerCase().endsWith(".eml"))
					contentType = "message/rfc822";

				org.springframework.http.ContentDisposition contentDisposition = org.springframework.http.ContentDisposition
						.builder("inline")
						.filename(filename, java.nio.charset.StandardCharsets.UTF_8)
						.build();

				return ResponseEntity.ok()
						.contentType(MediaType.parseMediaType(contentType))
						.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
						.body(resource);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}