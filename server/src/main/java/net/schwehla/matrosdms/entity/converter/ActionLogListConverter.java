/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.converter;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.schwehla.matrosdms.domain.action.ActionLog;

@Converter(autoApply = false)
public class ActionLogListConverter implements AttributeConverter<List<ActionLog>, String> {

	private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Override
	public String convertToDatabaseColumn(List<ActionLog> attribute) {
		if (attribute == null)
			return "[]";
		try {
			return mapper.writeValueAsString(attribute);
		} catch (Exception e) {
			throw new RuntimeException("JSON writing error", e);
		}
	}

	@Override
	public List<ActionLog> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty())
			return new ArrayList<>();
		try {
			return mapper.readValue(dbData, new TypeReference<List<ActionLog>>() {
			});
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}
}
