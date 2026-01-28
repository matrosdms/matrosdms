/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

public class MBaseElement implements IIdentifiable, Serializable, Comparable<MBaseElement> {

	private static final long serialVersionUID = 1L;

	public MBaseElement() {
	}

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	protected String uuid;

	protected String name;
	protected String icon;
	protected String description;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	protected MLifecycle lifecycle;

	@Schema(description = "Optimistic Locking Version. Send this back on updates.", accessMode = Schema.AccessMode.READ_ONLY)
	protected Long version;

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MLifecycle getLifecycle() {
		return lifecycle;
	}

	public void setLifecycle(MLifecycle identification) {
		this.lifecycle = identification;
	}

	@Override
	public boolean equals(Object other) {
		try {
			if (other instanceof IIdentifiable) {
				return getUuid().equals(((IIdentifiable) other).getUuid());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return false;
	}

	@Override
	public int compareTo(MBaseElement o) {
		if (o == null) {
			return 1;
		}
		return -o.getUuid().compareTo(o.getUuid());
	}

	@Override
	public String getUuid() {
		return uuid;
	}
}
