/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.domain.content.MDocumentStream;

@Service
public class MatrosObjectStoreService {

	@Autowired
	IMatrosStore store;

	public StoreResult persist(Path pdfFile, Path textFile, String uuid, String originalFilename) {
		return store.persist(pdfFile, textFile, uuid, originalFilename);
	}

	public MDocumentStream load(String uuid) {
		return store.loadStream(uuid);
	}

	// NEW: Direct access to text layer for API/AI
	public String loadTextLayer(String uuid) {
		return StoreContext.readTextFile(uuid);
	}
}
