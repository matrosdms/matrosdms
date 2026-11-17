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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import net.schwehla.matrosdms.domain.action.ESyncTarget;

/**
 * Where one action lives in one remote system. Keyed by action uuid rather than a
 * foreign key so it survives the action's deletion until the remote copy is gone.
 */
@Entity
@Table(name = "action_sync", uniqueConstraints = @UniqueConstraint(name = "uk_action_sync_target", columnNames = {
		"action_uuid", "target" }), indexes = @Index(name = "idx_action_sync_account", columnList = "account_user_uuid"))
public class DBActionSync {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "action_sync_seq")
	@SequenceGenerator(name = "action_sync_seq", sequenceName = "action_sync_seq", allocationSize = 50)
	private Long id;

	@Version
	private Long version;

	@Column(name = "action_uuid", nullable = false, length = 16)
	private String actionUuid;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ESyncTarget target;

	/** The MatrosDMS user whose Google account holds the remote copy. */
	@Column(name = "account_user_uuid", nullable = false, length = 16)
	private String accountUserUuid;

	/** Task list id or calendar id. */
	@Column(name = "container_id", nullable = false)
	private String containerId;

	@Column(name = "external_id", nullable = false)
	private String externalId;

	@Column(name = "external_etag")
	private String externalEtag;

	@Column(name = "date_synced")
	private LocalDateTime dateSynced;

	/**
	 * Completion state last pushed to or pulled from Google: the base of the
	 * three-way merge. Null until the first push records it; until then nothing is pulled.
	 */
	@Column(name = "synced_completed")
	private Boolean syncedCompleted;

	/** Due date last pushed to or pulled from Google (UTC wall time, like the action's). */
	@Column(name = "synced_due")
	private LocalDateTime syncedDue;

	public boolean hasBase() {
		return syncedCompleted != null;
	}

	public Boolean getSyncedCompleted() {
		return syncedCompleted;
	}

	public void setSyncedCompleted(Boolean syncedCompleted) {
		this.syncedCompleted = syncedCompleted;
	}

	public LocalDateTime getSyncedDue() {
		return syncedDue;
	}

	public void setSyncedDue(LocalDateTime syncedDue) {
		this.syncedDue = syncedDue;
	}

	public Long getId() {
		return id;
	}

	public Long getVersion() {
		return version;
	}

	public String getActionUuid() {
		return actionUuid;
	}

	public void setActionUuid(String actionUuid) {
		this.actionUuid = actionUuid;
	}

	public ESyncTarget getTarget() {
		return target;
	}

	public void setTarget(ESyncTarget target) {
		this.target = target;
	}

	public String getAccountUserUuid() {
		return accountUserUuid;
	}

	public void setAccountUserUuid(String accountUserUuid) {
		this.accountUserUuid = accountUserUuid;
	}

	public String getContainerId() {
		return containerId;
	}

	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getExternalEtag() {
		return externalEtag;
	}

	public void setExternalEtag(String externalEtag) {
		this.externalEtag = externalEtag;
	}

	public LocalDateTime getDateSynced() {
		return dateSynced;
	}

	public void setDateSynced(LocalDateTime dateSynced) {
		this.dateSynced = dateSynced;
	}
}
