/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Defines the scope/target of a Root Category")
public enum ECategoryScope {
	@Schema(description = "Used for Contexts/Folders (e.g. WHO, WHAT, WHERE)")
	CONTEXT,

	@Schema(description = "Used for Documents/Items (e.g. KIND/MIME)")
	DOCUMENT,

	@Schema(description = "Global or Universal category")
	ANY
}
