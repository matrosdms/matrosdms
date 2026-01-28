/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.exception;

public class EntityNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EntityNotFoundException(String message) {
		super(message);
	}
}
