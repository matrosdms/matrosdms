/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.content;

import java.io.InputStream;

/** DTO for Streaming Downloads */
public class MDocumentStream {
	private InputStream inputStream;
	private long length;
	private String filename;
	private String contentType;

	public MDocumentStream(InputStream inputStream, long length) {
		this.inputStream = inputStream;
		this.length = length;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public long getLength() {
		return length;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
