/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.attribute;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the supported data types for Flexfields (AttributeTypes). Used by the
 * Frontend to
 * determine which input widget to render.
 */
@Schema(enumAsRef = true)
public enum EAttributeType {
	TEXT, BOOLEAN, DATE, NUMBER, CURRENCY, LINK
}
