/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.schwehla.matrosdms.domain.content.MDocumentStream;
import net.schwehla.matrosdms.domain.core.EArchiveFilter;
import net.schwehla.matrosdms.domain.core.MFileMetadata;
import net.schwehla.matrosdms.domain.core.MItem;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.service.domain.ItemService;
import net.schwehla.matrosdms.service.domain.ThumbnailService;
import net.schwehla.matrosdms.service.facade.ItemIngestionFacade;
import net.schwehla.matrosdms.service.message.CreateItemMessage;
import net.schwehla.matrosdms.service.message.UpdateItemMessage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/items")
public class ItemController {

	@Autowired
	private ItemService itemService;
	@Autowired
	private ItemIngestionFacade ingestionFacade;
	@Autowired
	private ThumbnailService thumbnailService;

	@GetMapping(value = "/{uuid}/thumbnail", produces = MediaType.IMAGE_JPEG_VALUE)
	@Operation(summary = "Get preview thumbnail")
	public ResponseEntity<byte[]> getThumbnail(@PathVariable("uuid") String uuid) {
		byte[] data = thumbnailService.getThumbnail(uuid);
		if (data == null)
			return ResponseEntity.notFound().build();

		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(30, java.util.concurrent.TimeUnit.DAYS))
				.body(data);
	}

	@GetMapping
	@Operation(summary = "Get items in context (Paged)")
	public ResponseEntity<Page<MItem>> loadInfoItemList(
			@RequestParam(name = "context", required = false) String contextIdentifier,
			@RequestParam(name = "q", required = false) String query,
			@RequestParam(name = "archiveState", defaultValue = "ACTIVE_ONLY") EArchiveFilter archiveState,
			@PageableDefault(size = 20, sort = "issueDate", direction = Sort.Direction.DESC) Pageable pageable) {

		Sort mappedSort = Sort.unsorted();
		for (Sort.Order order : pageable.getSort()) {
			String prop = order.getProperty();

			if ("storeIdentifier".equals(prop)) {
				prop = "store.shortname";
			} else if ("storeItemNumber".equals(prop)) {
				prop = "storageItemIdentifier";
			} else if ("contextIdentifier".equals(prop)) {
				prop = "infoContext.name";
			}
			mappedSort = mappedSort.and(Sort.by(order.getDirection(), prop));
		}
		Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);

		Page<MItem> page = itemService.loadItemPage(contextIdentifier, query, archiveState, safePageable);
		return new ResponseEntity<>(page, HttpStatus.OK);
	}

	@PostMapping
	@Operation(summary = "Append a new Item (User is auto-detected from Token)")
	public ResponseEntity<MItem> addItemToContext(
			@Parameter(hidden = true) @AuthenticationPrincipal DBUser user,
			@Valid @RequestBody CreateItemMessage itemMessage) {

		itemMessage.setUserIdentifier(user.getUuid());
		MItem item = ingestionFacade.ingestItem(itemMessage);

		return new ResponseEntity<>(item, HttpStatus.CREATED);
	}

	@GetMapping("/{uuid}")
	@Operation(summary = "Get item by id")
	public ResponseEntity<MItem> loadItemByIdentifier(@PathVariable("uuid") String uuid) {
		MItem item = itemService.loadItem(uuid);
		return new ResponseEntity<>(item, HttpStatus.OK);
	}

	@GetMapping("/{uuid}/metadata")
	@Operation(summary = "Get file metadata for an item (hashes, mimetype, filesize, source)")
	public ResponseEntity<MFileMetadata> loadItemMetadata(@PathVariable("uuid") String uuid) {
		MItem item = itemService.loadItem(uuid);
		MFileMetadata meta = item.getMetadata();
		if (meta == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok(meta);
	}

	@GetMapping("/{uuid}/content")
	public ResponseEntity<Resource> loadItemContent(
			@PathVariable("uuid") String uuid,
			@RequestParam(name = "download", defaultValue = "false") boolean download) {

		MDocumentStream streamHolder = itemService.loadItemContentStream(uuid);
		InputStreamResource resource = new InputStreamResource(streamHolder.getInputStream());

		org.springframework.http.ContentDisposition contentDisposition = org.springframework.http.ContentDisposition
				.builder(download ? "attachment" : "inline")
				.filename(streamHolder.getFilename(), java.nio.charset.StandardCharsets.UTF_8)
				.build();

		return ResponseEntity.ok()
				.contentLength(streamHolder.getLength())
				.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
				.contentType(MediaType.parseMediaType(streamHolder.getContentType()))
				.body(resource);
	}

	@PutMapping("/{uuid}")
	@Operation(summary = "Update item by id")
	public ResponseEntity<MItem> updateItem(
			@PathVariable("uuid") String uuid, @Valid @RequestBody UpdateItemMessage itemMessage) {
		MItem item = itemService.updateItem(uuid, itemMessage);
		return new ResponseEntity<>(item, HttpStatus.OK);
	}

	// 1. SOFT DELETE (Archive)
	@DeleteMapping("/{uuid}")
	@Operation(summary = "Archive item (Soft Delete)")
	public ResponseEntity<HttpStatus> archiveItem(@PathVariable("uuid") String uuid) {
		itemService.archiveItem(uuid);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	// 2. RESTORE
	@PostMapping("/{uuid}/restore")
	@Operation(summary = "Restore archived item")
	public ResponseEntity<Void> restoreItem(@PathVariable("uuid") String uuid) {
		itemService.restoreItem(uuid);
		return ResponseEntity.ok().build();
	}

	// 3. HARD DELETE (Destroy)
	@DeleteMapping("/{uuid}/permanent")
	@Operation(summary = "Permanently destroy item")
	public ResponseEntity<Void> destroyItem(@PathVariable("uuid") String uuid) {
		itemService.hardDeleteItem(uuid);
		return ResponseEntity.noContent().build();
	}
}