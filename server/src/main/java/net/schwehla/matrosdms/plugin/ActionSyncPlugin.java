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
 * Service Provider Interface (SPI) for external Action Syncs. Implement this in
 * a separate JAR to
 * add Google/Outlook sync.
 */
public interface ActionSyncPlugin {
	String getName();

	void onActionCreated(MAction action);

	void onActionUpdated(MAction action);

	void onActionDeleted(String uuid);
}
