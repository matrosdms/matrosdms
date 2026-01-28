/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "ItemMetadata")

/**
 * Space for the physical files
 *
 * @author Martin
 */
@NamedQueries({
		@NamedQuery(name = "DBItemMetadata.findByChecksum", query = "SELECT c FROM DBItemMetadata c where c.sha256Original = :checksum")
})
public class DBItemMetadata {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false, name = "FILE_ID")
	private Long id;

	@Column(nullable = false, unique = false)
	String source;

	@Column(nullable = false, unique = false)
	Long filesize;

	@Column(nullable = false)
	String filename;

	@Column(nullable = false, unique = false)
	String mimetype;

	// SHA shall not be equal for same file
	@Column(nullable = false, unique = true, updatable = false)
	String sha256Original;

	// SHA shall not be equal for same file
	@Column(nullable = false, unique = true, updatable = false)
	String sha256Crypted;

	@Column(nullable = false, unique = false)
	String cryptSettings;

	public Long getFileId() {
		return id;
	}

	public void setFileId(Long fileId) {
		this.id = fileId;
	}

	public String getSha256Original() {
		return sha256Original;
	}

	public void setSha256Original(String sha256Original) {
		this.sha256Original = sha256Original;
	}

	public String getSha256Crypted() {
		return sha256Crypted;
	}

	public void setSha256Crypted(String sha256Crypted) {
		this.sha256Crypted = sha256Crypted;
	}

	public String getCryptSettings() {
		return cryptSettings;
	}

	public void setCryptSettings(String cryptSettings) {
		this.cryptSettings = cryptSettings;
	}

	public Long getFilesize() {
		return filesize;
	}

	public void setFilesize(Long filesize) {
		this.filesize = filesize;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
