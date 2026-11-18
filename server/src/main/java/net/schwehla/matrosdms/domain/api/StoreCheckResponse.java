/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.api;

import java.time.Instant;
import java.util.List;

import net.schwehla.matrosdms.service.management.StoreConsistencyService;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Last comparison of the document store with the database (after start, and by the Integrity Check)")
public record StoreCheckResponse(
		Instant checkedAt,
		@Schema(description = "Documents in the database") int databaseItems,
		@Schema(description = "Documents in the document store") int storeDocuments,
		@Schema(description = "Documents in the store the database does not know") int unknownInStore,
		@Schema(description = "Documents in the database without a file in the store") int missingInStore,
		@Schema(description = "Files in the store that belong to no document") int strayFiles,
		@Schema(description = "What to tell the administrator; empty when store and database match") List<String> warnings) {

	public static StoreCheckResponse of(StoreConsistencyService.Result result) {
		return new StoreCheckResponse(result.checkedAt(), result.databaseItems(), result.storeDocuments(),
				result.unknownInStore().size(), result.missingInStore().size(), result.strayFiles().size(),
				result.warnings());
	}
}
