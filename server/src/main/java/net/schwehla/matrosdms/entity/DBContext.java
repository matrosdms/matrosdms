/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import net.schwehla.matrosdms.domain.core.EStage;

@Entity
@Table(name = "Context", indexes = { @jakarta.persistence.Index(name = "context_uuid_index", columnList = "uuid") })
@NamedQueries({
		@NamedQuery(name = "DBContext.findAll", query = "SELECT c FROM DBContext c"),
		@NamedQuery(name = "DBContext.findByUUID", query = "SELECT c FROM DBContext c where c.uuid = :uuid")
})
public class DBContext extends AbstractDBInfoBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false, name = "CONTEXT_ID")
	protected Long id;

	@Override
	public Long getPK() {
		return id;
	}

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EStage stage = EStage.ACTIVE;

	@Column
	@Temporal(TemporalType.TIMESTAMP)
	LocalDateTime dateRunUntil;

	@OneToMany(mappedBy = "infoContext", fetch = FetchType.LAZY)
	List<DBItem> itemList = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "context_category", joinColumns = @JoinColumn(name = "CONTEXT_ID", referencedColumnName = "CONTEXT_ID"), inverseJoinColumns = @JoinColumn(name = "CATEGORY_ID", referencedColumnName = "CATEGORY_ID"), foreignKey = @ForeignKey(name = "FK_CTX_CAT_CONTEXT"), inverseForeignKey = @ForeignKey(name = "FK_CTX_CAT_CATEGORY"))
	private List<DBCategory> categoryList = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public EStage getStage() {
		return stage;
	}

	public void setStage(EStage stage) {
		this.stage = stage;
	}

	public List<DBCategory> getCategoryList() {
		return categoryList;
	}

	public void setCategoryList(List<DBCategory> categoryList) {
		this.categoryList = categoryList;
	}

	public List<DBItem> getItemList() {
		return itemList;
	}

	public void setItemList(List<DBItem> itemList) {
		this.itemList = itemList;
	}

	public LocalDateTime getDateRunUntil() {
		return dateRunUntil;
	}

	public void setDateRunUntil(LocalDateTime dateRunUntil) {
		this.dateRunUntil = dateRunUntil;
	}
}
