/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runs a callback once the surrounding transaction has committed, or right away when
 * there is none. A rolled-back transaction never runs it, so side effects such as
 * scheduling a sync never fire for data that was not saved.
 */
public final class AfterCommit {

	private AfterCommit() {
	}

	public static void run(Runnable callback) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					callback.run();
				}
			});
		} else {
			callback.run();
		}
	}
}
