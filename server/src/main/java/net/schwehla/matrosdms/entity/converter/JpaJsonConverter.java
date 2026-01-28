/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

@Converter(autoApply = false)
public class JpaJsonConverter implements AttributeConverter<Map<String, Object>, String> {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Map<String, Object> attribute) {
		if (attribute == null) {
			return "{}";
		}
		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error converting Map to JSON", e);
		}
	}

	@Override
	@SuppressWarnings("unchecked") // FIX: Suppress warning for Map cast
	public Map<String, Object> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty()) {
			return new HashMap<>();
		}
		try {
			return objectMapper.readValue(dbData, Map.class);
		} catch (MismatchedInputException e) {
			try {
				String unescaped = objectMapper.readValue(dbData, String.class);
				return objectMapper.readValue(unescaped, Map.class);
			} catch (IOException ex) {
				throw new RuntimeException("Failed to parse JSON. Data: " + dbData, e);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error converting JSON to Map", e);
		}
	}
}
