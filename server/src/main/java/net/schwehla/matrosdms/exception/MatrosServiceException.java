/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.exception;

public class MatrosServiceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MatrosServiceException(String text, Exception e) {
		super(text, e);
	}

	public MatrosServiceException(String text) {
		super(text);
	}
}
