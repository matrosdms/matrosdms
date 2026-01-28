/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "Category", indexes = {
		@Index(name = "IDX_CATEGORY_UUID", columnList = "uuid"),
		@Index(name = "IDX_CATEGORY_PARENT_NAME", columnList = "PARENT_CATEGORY_ID, name")
}, uniqueConstraints = {
		@UniqueConstraint(name = "UQ_CATEGORY_PARENT_NAME", columnNames = { "PARENT_CATEGORY_ID", "name" })
})
public class DBCategory extends AbstractDBInfoBaseEntityWithOrdinal {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_gen")
	@SequenceGenerator(name = "category_gen", sequenceName = "category_seq", allocationSize = 50)
	@Column(unique = true, nullable = false, name = "CATEGORY_ID")
	private Long id;

	public Long getPK() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(unique = false, nullable = true)
	Boolean object = Boolean.FALSE;

	@ManyToOne
	// NEW: Explicit Name
	@JoinColumn(name = "PARENT_CATEGORY_ID", nullable = true, foreignKey = @ForeignKey(name = "FK_CATEGORY_PARENT"))
	private DBCategory parent;

	@OneToMany(mappedBy = "parent")
	private List<DBCategory> children;

	public boolean isObject() {
		return object != null && object;
	}

	public void setObject(boolean object) {
		this.object = object;
	}

	public DBCategory getParent() {
		return parent;
	}

	public void setParent(DBCategory parent) {
		this.parent = parent;
	}

	public List<DBCategory> getChildren() {
		return children;
	}

	public void setChildren(List<DBCategory> children) {
		this.children = children;
	}
}
