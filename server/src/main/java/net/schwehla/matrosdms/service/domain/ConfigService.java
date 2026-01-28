/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.domain.admin.EConfigKey;
import net.schwehla.matrosdms.entity.management.DBConfig;
import net.schwehla.matrosdms.repository.ConfigRepository;

@Service
public class ConfigService {

	@Autowired
	ConfigRepository configRepository;

	// READ: Cache the result. The key is generated automatically from EConfigKey
	@Transactional(readOnly = true)
	@Cacheable(value = "config", key = "#key.name()")
	public Optional<String> getValue(EConfigKey key) {
		return configRepository.findByKey(key.name()).map(DBConfig::getConfig_value);
	}

	// Helper method (reuses the cached method above)
	@Transactional(readOnly = true)
	public String getValue(EConfigKey key, String defaultValue) {
		return getValue(key).orElse(defaultValue);
	}

	// WRITE: Clear the cache when value changes
	@Transactional
	@CacheEvict(value = "config", key = "#key.name()")
	public void setValue(EConfigKey key, String value) {
		DBConfig config = configRepository.findByKey(key.name()).orElse(new DBConfig());
		if (config.getId() == null) {
			config.setConfig_key(key.name());
		}
		config.setConfig_value(value);
		configRepository.save(config);
	}

	@Transactional(readOnly = true)
	public boolean isSetupRequired() {
		return getValue(EConfigKey.PREFERED_LANGUAGE).isEmpty();
	}
}
