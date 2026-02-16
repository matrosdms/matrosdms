/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.store;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Static facade for backward compatibility with Hibernate Search ItemTextBridge.
 * Delegates to the proper IMatrosStore implementation.
 */
@Component
public class StoreContext {

    private static IMatrosStore store;

    private final IMatrosStore matrosStore;

    public StoreContext(IMatrosStore matrosStore) {
        this.matrosStore = matrosStore;
    }

    @PostConstruct
    public void init() {
        StoreContext.store = this.matrosStore;
    }

    /**
     * Reads a text file for the given UUID.
     * Used by Hibernate Search ItemTextBridge for indexing.
     * 
     * @param uuid Document UUID
     * @return Text content, or empty string if not found
     */
    public static String readTextFile(String uuid) {
        if (store == null) {
            return "";
        }
        return store.loadTextLayer(uuid);
    }
}