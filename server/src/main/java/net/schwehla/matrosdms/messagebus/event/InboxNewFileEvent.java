/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.messagebus.event;

import java.nio.file.Path;

public class InboxNewFileEvent {
	Path path;

	public InboxNewFileEvent(Path path) {
		this.path = path;
	}

	public Path getPath() {
		return path;
	}
}
