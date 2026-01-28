/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.action;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum EActionStatus {
	OPEN(false), IN_PROGRESS(false), ON_HOLD(false),

	DONE(true), REJECTED(true);

	private final boolean completed;

	EActionStatus(boolean completed) {
		this.completed = completed;
	}

	public boolean isCompleted() {
		return completed;
	}
}
