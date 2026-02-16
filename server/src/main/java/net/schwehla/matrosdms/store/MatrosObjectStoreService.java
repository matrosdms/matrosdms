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

	public String loadTextLayer(String uuid) {
		return store.loadTextLayer(uuid);
	}

	public void moveToTrash(String uuid) {
		store.moveToTrash(uuid);
	}
}