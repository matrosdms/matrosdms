/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import net.schwehla.matrosdms.domain.core.EArchiveFilter;
import net.schwehla.matrosdms.domain.core.MContext;
import net.schwehla.matrosdms.entity.DBContext;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.entity.view.VW_CONTEXT;
import net.schwehla.matrosdms.repository.ContextRepository;
import net.schwehla.matrosdms.repository.ContextViewRepository;
import net.schwehla.matrosdms.repository.ItemRepository;
import net.schwehla.matrosdms.service.SearchService;
import net.schwehla.matrosdms.service.mapper.MContextMapper;
import net.schwehla.matrosdms.service.message.CreateContextMessage;
import net.schwehla.matrosdms.service.message.UpdateContextMessage;
import net.schwehla.matrosdms.util.UUIDProvider;

@Service
@Transactional
public class ContextService {

	@Autowired
	ContextRepository contextRepository;
	@Autowired
	ContextViewRepository contextViewRepository;
	@Autowired
	ItemRepository itemRepository;
	@Autowired
	MContextMapper contextMapper;
	@Autowired
	UUIDProvider uuidProvider;
	@Autowired
	SearchService searchService;

	@CacheEvict(value = "contextList", allEntries = true)
	public MContext createContext(CreateContextMessage message) {
		DBContext dbContext = contextMapper.modelToEntity(message);
		dbContext.setUuid(uuidProvider.getTimeBasedUUID());
		contextRepository.save(dbContext);
		return contextMapper.entityToModel(dbContext);
	}

	@Caching(evict = {
			@CacheEvict(value = "contextList", allEntries = true),
			@CacheEvict(value = "contexts", key = "#uuid")
	})
	public MContext updateContext(String uuid, UpdateContextMessage message) {
		DBContext dbContext = contextRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Context not found: " + uuid));

		contextMapper.updateEntity(message, dbContext);
		DBContext saved = contextRepository.save(dbContext);
		return contextMapper.entityToModel(saved);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "contextList", key = "#archiveState.name() + '-' + #limit + '-' + #sortStr")
	public List<MContext> loadContextList(EArchiveFilter archiveState, int limit, String sortStr) {

		PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "name"));

		if (archiveState == EArchiveFilter.ARCHIVED_ONLY) {
			List<DBContext> archived = contextRepository.findAllArchived(pageRequest);
			List<MContext> result = archived.stream().map(contextMapper::entityToModel).collect(Collectors.toList());
			result.forEach(ctx -> {
				long count = itemRepository.countByInfoContext_UuidAndDateArchivedIsNull(ctx.getUuid());
				ctx.setItemCount(count);
			});
			return result;
		}

		if (archiveState == EArchiveFilter.ALL) {
			List<DBContext> all = contextRepository.findAllContexts(pageRequest);
			List<MContext> result = all.stream().map(contextMapper::entityToModel).collect(Collectors.toList());
			result.forEach(ctx -> {
				long count = itemRepository.countByInfoContext_UuidAndDateArchivedIsNull(ctx.getUuid());
				ctx.setItemCount(count);
			});
			return result;
		}

		// ACTIVE_ONLY: use optimized VW_CONTEXT view
		List<VW_CONTEXT> views = contextViewRepository.findAll(pageRequest).getContent();
		return contextMapper.mapViews(views);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "contexts", key = "#tsid")
	public MContext loadContext(String tsid) {
		var context = contextRepository.findByUuid(tsid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Context not found: " + tsid));

		MContext result = contextMapper.entityToModel(context);
		long count = itemRepository.countByInfoContext_UuidAndDateArchivedIsNull(tsid);
		result.setItemCount(count);

		return result;
	}

	@Caching(evict = {
			@CacheEvict(value = "contextList", allEntries = true),
			@CacheEvict(value = "contexts", key = "#uuid")
	})
	public void archiveContext(String uuid) {
		DBContext context = contextRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Context not found"));
		// Capture ONE timestamp for both context and all cascade-archived items.
		// Items already archived (dateArchived != null) are left untouched so their
		// individual archive date is preserved and they are NOT restored on context
		// restore.
		LocalDateTime ts = LocalDateTime.now();
		context.setDateArchived(ts);
		contextRepository.save(context);
		List<DBItem> itemsToArchive = itemRepository.findAllNotArchivedByContextid(context.getId());
		for (DBItem item : itemsToArchive) {
			item.setDateArchived(ts);
		}
		itemRepository.saveAll(itemsToArchive);
		reindexItemsInContext(context);
	}

	@Caching(evict = {
			@CacheEvict(value = "contextList", allEntries = true),
			@CacheEvict(value = "contexts", key = "#uuid")
	})
	public void restoreContext(String uuid) {
		DBContext context = contextRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Context not found"));
		// Only restore items that were archived as part of THIS context-archive
		// operation
		// (i.e. their dateArchived equals the context's archive timestamp).
		// Items that were already archived before the context was archived keep their
		// date.
		LocalDateTime contextArchivedAt = context.getDateArchived();
		if (contextArchivedAt != null) {
			List<DBItem> cascadedItems = itemRepository.findArchivedAtTimestampByContextId(context.getId(),
					contextArchivedAt);
			for (DBItem item : cascadedItems) {
				item.setDateArchived(null);
			}
			itemRepository.saveAll(cascadedItems);
		}
		context.setDateArchived(null);
		contextRepository.save(context);
		reindexItemsInContext(context);
	}

	private void reindexItemsInContext(DBContext context) {
		List<DBItem> items = itemRepository.findAllByContextid(context.getId());
		for (DBItem item : items) {
			try {
				searchService.indexSingleItem(item.getId());
			} catch (Exception e) {
				// Non-fatal: search index update is best-effort
			}
		}
	}

	@Caching(evict = {
			@CacheEvict(value = "contextList", allEntries = true),
			@CacheEvict(value = "contexts", key = "#uuid")
	})
	public void deleteContext(String uuid) {
		DBContext context = contextRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Context not found"));
		contextRepository.delete(context);
	}
}