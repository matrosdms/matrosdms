/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.entity.view;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import net.schwehla.matrosdms.domain.core.EStage;
import net.schwehla.matrosdms.entity.AbstractDBInfoBaseEntity;
import net.schwehla.matrosdms.entity.DBCategory;

@Entity
@Immutable
@Subselect("SELECT * FROM VW_CONTEXT")
public class VW_CONTEXT extends AbstractDBInfoBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false, name = "CONTEXT_ID")
	private Long id;

	@Column
	int sum;

	@Enumerated(EnumType.STRING)
	@Column
	private EStage stage = EStage.ACTIVE;

	@Column(name = "date_run_until")
	private LocalDateTime dateRunUntil;

	@ManyToMany
	@JoinTable(name = "context_category", joinColumns = @JoinColumn(name = "CONTEXT_ID", referencedColumnName = "CONTEXT_ID"), inverseJoinColumns = @JoinColumn(name = "CATEGORY_ID", referencedColumnName = "CATEGORY_ID"))
	private List<DBCategory> categoryList = new ArrayList<>();

	public List<DBCategory> getCategoryList() {
		return categoryList;
	}

	public int getSum() {
		return sum;
	}

	@Override
	public Long getPK() {
		return id;
	}

	public EStage getStage() {
		return stage;
	}

	public void setStage(EStage stage) {
		this.stage = stage;
	}
}
