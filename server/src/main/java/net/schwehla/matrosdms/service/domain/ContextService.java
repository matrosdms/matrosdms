/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.util.List;

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
import net.schwehla.matrosdms.entity.view.VW_CONTEXT;
import net.schwehla.matrosdms.repository.ContextRepository;
import net.schwehla.matrosdms.repository.ContextViewRepository;
import net.schwehla.matrosdms.repository.ItemRepository;
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

		Sort.Direction dir = "name".equalsIgnoreCase(sortStr) ? Sort.Direction.ASC : Sort.Direction.DESC;
		Sort sort = Sort.by(dir, sortStr);

		// NOTE: Currently VW_CONTEXT only supports ACTIVE items.
		// If ARCHIVED_ONLY is requested for Contexts, you might need a different
		// view/query.
		// For now, this just passes through the standard view.
		PageRequest pageRequest = PageRequest.of(0, limit, sort);
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
	public void deleteContext(String uuid) {
		DBContext context = contextRepository.findByUuid(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Context not found"));
		contextRepository.delete(context);
	}
}