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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;

import net.schwehla.matrosdms.domain.core.EItemSource;
import net.schwehla.matrosdms.domain.core.EStage;
import net.schwehla.matrosdms.entity.converter.JpaJsonConverter;
import net.schwehla.matrosdms.entity.management.DBUser;
import net.schwehla.matrosdms.search.ItemTextBinder;

@Entity
@Indexed
@TypeBinding(binder = @TypeBinderRef(type = ItemTextBinder.class))
@Table(name = "Item", indexes = {
		@Index(columnList = "uuid", name = "idx_item_uuid"),
		@Index(columnList = "issueDate", name = "idx_item_issueDate"),
		@Index(columnList = "dateArchived", name = "idx_item_dateArchived"),
		@Index(columnList = "source", name = "idx_item_source"),
		@Index(columnList = "CONTEXT_ID", name = "idx_item_context"),
}, uniqueConstraints = @UniqueConstraint(columnNames = { "ITEM_ID" }, name = "UNIQUE_ID"))
@NamedEntityGraph(name = "Item.detail", attributeNodes = {
		@NamedAttributeNode("infoContext"),
		@NamedAttributeNode("user"),
		@NamedAttributeNode("store"),
		@NamedAttributeNode("file")
})
public class DBItem extends AbstractDBInfoBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(nullable = false, name = "ITEM_ID")
	private Long id;

	@Override
	public Long getPK() {
		return this.id;
	}

	@Column(name = "attributes", columnDefinition = "json")
	@Convert(converter = JpaJsonConverter.class)
	private Map<String, Object> attributes = new HashMap<>();

	@Override
	public String getName() {
		return super.getName();
	}

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private EItemSource source = EItemSource.UPLOAD;

	// NEW: Boolean Flag. True if text extraction was successful.
	@Column(name = "text_parsed", nullable = false)
	private boolean textParsed = false;

	@ManyToMany
	@JoinTable(name = "Item_Category", joinColumns = @JoinColumn(name = "ITEM_ID"), inverseJoinColumns = @JoinColumn(name = "CATEGORY_ID"), foreignKey = @ForeignKey(name = "FK_ITEM_CAT_ITEM"), inverseForeignKey = @ForeignKey(name = "FK_ITEM_CAT_CATEGORY"))
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	private List<DBCategory> kindList = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "CONTEXT_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_ITEM_CONTEXT"))
	private DBContext infoContext;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "STORE_ID", nullable = true, foreignKey = @ForeignKey(name = "FK_ITEM_STORE"))
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	private DBStore store;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_ITEM_USER"))
	private DBUser user;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FILE_ID", unique = true, nullable = true, foreignKey = @ForeignKey(name = "FK_ITEM_METADATA"))
	private DBItemMetadata file;

	@Temporal(TemporalType.TIMESTAMP)
	LocalDateTime issueDate;

	@Temporal(TemporalType.TIMESTAMP)
	LocalDateTime dateExpire;

	@Enumerated(EnumType.STRING)
	@Column
	private EStage stage;

	private String storageItemIdentifier;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public List<DBCategory> getKindList() {
		return kindList;
	}

	public void setKindList(List<DBCategory> kindList) {
		this.kindList = kindList;
	}

	public DBContext getInfoContext() {
		return infoContext;
	}

	public void setInfoContext(DBContext infoContext) {
		this.infoContext = infoContext;
	}

	public DBStore getStore() {
		return store;
	}

	public void setStore(DBStore store) {
		this.store = store;
	}

	public DBUser getUser() {
		return user;
	}

	public void setUser(DBUser user) {
		this.user = user;
	}

	public DBItemMetadata getFile() {
		return file;
	}

	public void setFile(DBItemMetadata file) {
		this.file = file;
	}

	public LocalDateTime getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(LocalDateTime issueDate) {
		this.issueDate = issueDate;
	}

	public LocalDateTime getDateExpire() {
		return dateExpire;
	}

	public void setDateExpire(LocalDateTime dateExpire) {
		this.dateExpire = dateExpire;
	}

	public EStage getStage() {
		return stage;
	}

	public void setStage(EStage stage) {
		this.stage = stage;
	}

	public String getStorageItemIdentifier() {
		return storageItemIdentifier;
	}

	public void setStorageItemIdentifier(String storageItemIdentifier) {
		this.storageItemIdentifier = storageItemIdentifier;
	}

	public EItemSource getSource() {
		return source;
	}

	public void setSource(EItemSource source) {
		this.source = source;
	}

	// Getter/Setter for new flag
	public boolean isTextParsed() {
		return textParsed;
	}

	public void setTextParsed(boolean textParsed) {
		this.textParsed = textParsed;
	}
}
