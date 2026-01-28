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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import net.schwehla.matrosdms.domain.core.MStore;
import net.schwehla.matrosdms.entity.DBStore;
import net.schwehla.matrosdms.repository.StoreRepository;
import net.schwehla.matrosdms.service.mapper.MStoreMapper;
import net.schwehla.matrosdms.util.UUIDProvider;

@Service
@Transactional
public class StoreService {

	@Autowired
	StoreRepository storeRepository;
	@Autowired
	MStoreMapper storeMapper;
	@Autowired
	UUIDProvider uuidProvider;

	public Integer getNextStoreItemNumber(String storeUuid) {
		long count = storeRepository.countItemsByStoreUuid(storeUuid);
		return (int) (count + 1);
	}

	@CacheEvict(value = "storeList", allEntries = true)
	public MStore createStore(MStore element) {
		DBStore entity = storeMapper.modelToEntity(element);
		entity.setUuid(uuidProvider.getTimeBasedUUID());
		DBStore savedEntity = storeRepository.save(entity);
		return storeMapper.entityToModel(savedEntity);
	}

	@Caching(evict = {
			@CacheEvict(value = "storeList", allEntries = true),
			@CacheEvict(value = "stores", key = "#uuid")
	})
	public MStore updateStore(String uuid, MStore storeModel) {
		DBStore existing = storeRepository
				.findByUuid(uuid)
				.orElseThrow(
						() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + uuid));

		storeMapper.updateEntity(storeModel, existing);

		DBStore saved = storeRepository.save(existing);
		return storeMapper.entityToModel(saved);
	}

	@Caching(evict = {
			@CacheEvict(value = "storeList", allEntries = true),
			@CacheEvict(value = "stores", allEntries = true)
	})
	public void deleteAllStore() {
		storeRepository.deleteAll();
	}

	@Caching(evict = {
			@CacheEvict(value = "storeList", allEntries = true),
			@CacheEvict(value = "stores", key = "#uuid")
	})
	public void deleteStore(String uuid) {
		storeRepository.delete(
				storeRepository
						.findByUuid(uuid)
						.orElseThrow(
								() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + uuid)));
	}

	@Cacheable(value = "storeList")
	public List<MStore> loadStoreList() {
		return storeMapper.map(storeRepository.findAll());
	}

	@Cacheable(value = "stores", key = "#uuid")
	public MStore loadStoreDetail(String uuid) {
		DBStore item = storeRepository
				.findByUuid(uuid)
				.orElseThrow(
						() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + uuid));
		return storeMapper.entityToModel(item);
	}
}
