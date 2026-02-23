/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.search;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.store.StoreContext;
import net.schwehla.matrosdms.util.TextLayerUtils;

/**
 * Bridge that injects the file content into the search index.
 * Logic: Always attempts to read from disk to ensure self-healing of the index.
 */
public class ItemTextBridge implements TypeBridge<DBItem> {
    
    private static final Logger log = LoggerFactory.getLogger(ItemTextBridge.class);
    
    private final IndexFieldReference<String> contentField;
    private final IndexObjectFieldReference attributesObjectField;

    public ItemTextBridge(
            IndexFieldReference<String> contentField,
            IndexObjectFieldReference attributesObjectField) {
        this.contentField = contentField;
        this.attributesObjectField = attributesObjectField;
    }

    @Override
    public void write(DocumentElement target, DBItem item, TypeBridgeWriteContext context) {
        // 1. Basic Metadata
        if (item.getUuid() != null) target.addValue("uuid", item.getUuid());
        if (item.getName() != null) target.addValue("name", item.getName());
        if (item.getDescription() != null) target.addValue("description", item.getDescription());
        
        if (item.getFile() != null && item.getFile().getFilename() != null) {
            target.addValue("filename", item.getFile().getFilename());
        }

        if (item.getSource() != null) {
            target.addValue("source", item.getSource().name());
        }

        target.addValue("textParsed", String.valueOf(item.isTextParsed()));

        // 2. Fulltext Content (The critical part)
        // We ignore the DB flag and try to read from disk to ensure index consistency.
        if (item.getUuid() != null) {
            try {
                // Decrypts on the fly via StoreContext -> MatrosLocalStore -> EncryptionService
                String rawXmlContent = StoreContext.readTextFile(item.getUuid());
                
                if (rawXmlContent != null && !rawXmlContent.isBlank()) {
                    // Extract text from XML wrapper
                    String cleanText = TextLayerUtils.extractCleanText(rawXmlContent);
                    
                    if (!cleanText.isEmpty()) {
                        target.addValue(contentField, cleanText);
                    } else {
                        // Fallback: Index raw XML if cleaning fails (better than nothing)
                        target.addValue(contentField, rawXmlContent);
                    }
                }
            } catch (Exception e) {
                // Log debug to avoid flooding logs during mass reindexing of broken items
                log.debug("Index: Could not read text layer for {}: {}", item.getUuid(), e.getMessage());
            }
        }
        
        // 3. Context (Folder)
        if (item.getInfoContext() != null) {
            var ctx = target.addObject("infoContext");
            if (item.getInfoContext().getName() != null)
                ctx.addValue("name", item.getInfoContext().getName());
            if (item.getInfoContext().getUuid() != null)
                ctx.addValue("uuid", item.getInfoContext().getUuid());
        }

        // 4. Store (Physical Location)
        if (item.getStore() != null) {
            var st = target.addObject("store");
            if (item.getStore().getShortname() != null)
                st.addValue("shortname", item.getStore().getShortname());
            if (item.getStore().getUuid() != null)
                st.addValue("uuid", item.getStore().getUuid());
        }

        // 5. Categories (Tags) - Recursive
        if (item.getKindList() != null && !item.getKindList().isEmpty()) {
            Set<String> indexedCategories = new HashSet<>();
            for (DBCategory cat : item.getKindList()) {
                DBCategory current = cat;
                while (current != null) {
                    // Avoid cycles and duplicates
                    if (!indexedCategories.add(current.getName()))
                        break;
                        
                    var k = target.addObject("kindList");
                    k.addValue("name", current.getName());
                    k.addValue("raw", current.getName());
                    k.addValue("uuid", current.getUuid());
                    
                    current = current.getParent();
                }
            }
        }

        // 6. Attributes (Flexfields)
        if (item.getAttributes() != null && !item.getAttributes().isEmpty()) {
            DocumentElement attrObject = target.addObject(attributesObjectField);
            for (Map.Entry<String, Object> entry : item.getAttributes().entrySet()) {
                if (entry.getValue() != null) {
                    // Index as string for searchability
                    attrObject.addValue(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        // 7. Dates
        if (item.getIssueDate() != null) {
            target.addValue("issueDate", item.getIssueDate().toLocalDate());
        }
        if (item.getDateCreated() != null) {
            target.addValue("dateCreated", item.getDateCreated());
        }
    }
}