/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import java.nio.file.Path;

import net.schwehla.matrosdms.domain.content.MDocumentStream;

public interface IMatrosStore {
	MDocumentStream loadStream(String uuid);

	StoreResult persist(Path pdfFile, Path textFile, String uuid, String originalFilename);
}
