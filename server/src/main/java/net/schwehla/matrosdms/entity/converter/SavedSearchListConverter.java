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

import net.schwehla.matrosdms.service.message.SavedSearchMessage;

@Converter
public class SavedSearchListConverter
		implements AttributeConverter<List<SavedSearchMessage>, String> {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<SavedSearchMessage> attribute) {
		if (attribute == null)
			return "[]";
		try {
			return mapper.writeValueAsString(attribute);
		} catch (Exception e) {
			throw new RuntimeException("Error converting Saved Searches to JSON", e);
		}
	}

	@Override
	public List<SavedSearchMessage> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty())
			return new ArrayList<>();
		try {
			return mapper.readValue(dbData, new TypeReference<List<SavedSearchMessage>>() {
			});
		} catch (Exception e) {
			// Graceful fallback prevents app crash on corrupt data
			return new ArrayList<>();
		}
	}
}
