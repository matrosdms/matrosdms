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

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.content.MDocumentStream;

// @Component
// @Order(2)
public class MatrosRemoteStore implements IMatrosStore {

	@Autowired
	AppServerSpringConfig appServerSpringConfig;

	@Override
	public MDocumentStream loadStream(String uuid) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public StoreResult persist(Path pdfFile, Path textFile, String uuid, String originalFilename) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void moveToTrash(String uuid) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean hasThumbnail(String uuid) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void storeThumbnail(String uuid, byte[] data) {
		throw new RuntimeException("Not implemented");

	}

	@Override
	public byte[] loadThumbnail(String uuid) {
		throw new RuntimeException("Not implemented");
	}
}
