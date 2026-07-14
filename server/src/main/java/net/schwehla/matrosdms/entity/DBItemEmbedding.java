/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Semantic embedding vector for one item, generated at ingest. */
@Entity
@Table(name = "item_embedding", indexes = { @Index(name = "idx_item_embedding_uuid", columnList = "item_uuid") })
public class DBItemEmbedding {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_embedding_seq")
	@SequenceGenerator(name = "item_embedding_seq", sequenceName = "item_embedding_seq", allocationSize = 50)
	private Long id;

	@Version
	private Long version;

	@Column(name = "item_uuid", nullable = false, unique = true, length = 16)
	private String itemUuid;

	@Column(nullable = false)
	private String model;

	@Column(nullable = false)
	private int dimension;

	@Column(nullable = false)
	private byte[] vector;

	@Column(name = "date_created")
	private LocalDateTime dateCreated;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getItemUuid() {
		return itemUuid;
	}

	public void setItemUuid(String itemUuid) {
		this.itemUuid = itemUuid;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public int getDimension() {
		return dimension;
	}

	public void setDimension(int dimension) {
		this.dimension = dimension;
	}

	public byte[] getVector() {
		return vector;
	}

	public void setVector(byte[] vector) {
		this.vector = vector;
	}

	public LocalDateTime getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(LocalDateTime dateCreated) {
		this.dateCreated = dateCreated;
	}
}
