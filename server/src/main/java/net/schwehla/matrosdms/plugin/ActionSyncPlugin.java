/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.plugin;

import net.schwehla.matrosdms.domain.action.MAction;

/**
 * Service Provider Interface (SPI) for external action syncs, loaded from JARs in
 * {@code workspace/plugins}. Callbacks run after the commit on a single background
 * thread, in order; they are best effort, with no retry. Google Tasks/Calendar is
 * built in instead (see {@code integration.google}), with persistent retrying jobs.
 */
public interface ActionSyncPlugin {
	String getName();

	void onActionCreated(MAction action);

	void onActionUpdated(MAction action);

	void onActionDeleted(String uuid);
}
