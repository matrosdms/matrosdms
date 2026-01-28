/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.domain.attribute.EAttributeType;
import net.schwehla.matrosdms.entity.DBAttributeType;
import net.schwehla.matrosdms.repository.AttributeTypeRepository;

@Service
public class AttributeLookupService {

	@Autowired
	AttributeTypeRepository repository;

	private final Map<String, String> uuidToName = new ConcurrentHashMap<>();
	private final Map<String, String> nameToUuid = new ConcurrentHashMap<>();

	// Fixed: Now stores the Enum, not a String
	private final Map<String, EAttributeType> uuidToType = new ConcurrentHashMap<>();

	@PostConstruct
	@Transactional(readOnly = true)
	public void init() {
		try {
			// refresh();
		} catch (Exception e) {
			// FIX: Catch DB errors so the server starts successfully.
			// This allows Hibernate to write the DDL file even if the current DB is
			// invalid.
			System.err.println(
					"WARN: AttributeLookupService skipped init (Schema mismatch?): " + e.getMessage());
		}
	}

	public void refresh() {
		uuidToName.clear();
		nameToUuid.clear();
		uuidToType.clear();

		for (DBAttributeType type : repository.findAll()) {
			uuidToName.put(type.getUuid(), type.getName());
			nameToUuid.put(type.getName(), type.getUuid());
			// Fixed: getDataType() now returns EAttributeType, so we store it directly
			uuidToType.put(type.getUuid(), type.getDataType());
		}
	}

	public String getName(String uuid) {
		return uuidToName.get(uuid);
	}

	public String getUuid(String name) {
		return nameToUuid.get(name);
	}

	// Fixed: Returns Enum type-safely. Defaults to TEXT if not found.
	public EAttributeType getType(String uuid) {
		return uuidToType.getOrDefault(uuid, EAttributeType.TEXT);
	}
}
