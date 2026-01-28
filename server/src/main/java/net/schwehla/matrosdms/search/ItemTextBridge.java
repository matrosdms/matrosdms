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

import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.entity.DBItem;
import net.schwehla.matrosdms.store.StoreContext;

public class ItemTextBridge implements TypeBridge<DBItem> {
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
		if (item.getUuid() != null)
			target.addValue("uuid", item.getUuid());
		if (item.getName() != null)
			target.addValue("name", item.getName());
		if (item.getDescription() != null)
			target.addValue("description", item.getDescription());
		if (item.getFile() != null && item.getFile().getFilename() != null)
			target.addValue("filename", item.getFile().getFilename());

		if (item.getSource() != null)
			target.addValue("source", item.getSource().name());

		// NEW: Write boolean as string for Index
		target.addValue("textParsed", String.valueOf(item.isTextParsed()));

		if (item.getUuid() != null) {
			String content = StoreContext.readTextFile(item.getUuid());
			if (content != null && !content.isEmpty())
				target.addValue(contentField, content);
		}
		if (item.getInfoContext() != null) {
			var ctx = target.addObject("infoContext");
			if (item.getInfoContext().getName() != null)
				ctx.addValue("name", item.getInfoContext().getName());
			if (item.getInfoContext().getUuid() != null)
				ctx.addValue("uuid", item.getInfoContext().getUuid());
		}
		if (item.getStore() != null) {
			var st = target.addObject("store");
			if (item.getStore().getShortname() != null)
				st.addValue("shortname", item.getStore().getShortname());
			if (item.getStore().getUuid() != null)
				st.addValue("uuid", item.getStore().getUuid());
		}
		if (item.getKindList() != null && !item.getKindList().isEmpty()) {
			Set<String> indexedCategories = new HashSet<>();
			for (DBCategory cat : item.getKindList()) {
				DBCategory current = cat;
				while (current != null) {
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
		if (item.getAttributes() != null && !item.getAttributes().isEmpty()) {
			DocumentElement attrObject = target.addObject(attributesObjectField);
			for (Map.Entry<String, Object> entry : item.getAttributes().entrySet()) {
				if (entry.getValue() != null)
					attrObject.addValue(entry.getKey(), entry.getValue().toString());
			}
		}
		if (item.getIssueDate() != null)
			target.addValue("issueDate", item.getIssueDate().toLocalDate());
		if (item.getDateCreated() != null)
			target.addValue("dateCreated", item.getDateCreated());
	}
}
