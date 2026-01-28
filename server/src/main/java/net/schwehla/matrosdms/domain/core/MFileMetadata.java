/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

public class MFileMetadata {

	String source;

	String mimetype;
	String filename;
	String sha256;
	Long filesize;

	transient String textLayer;

	public Long getFilesize() {
		return filesize;
	}

	public void setFilesize(Long filesize) {
		this.filesize = filesize;
	}

	public String getTextLayer() {
		return textLayer;
	}

	public void setTextLayer(String textLayer) {
		this.textLayer = textLayer;
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String parseFileExtension() {
		if (filename != null && filename.indexOf('.') > 0) {
			String[] tokens = filename.split("\\.(?=[^\\.]+$)"); // $NON-NLS-1$

			return tokens[tokens.length - 1];
		}

		return null;
	}
}
