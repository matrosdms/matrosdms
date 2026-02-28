/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.search;

import java.util.EnumSet;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.*;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;

import net.schwehla.matrosdms.entity.DBItem;

public class ItemTextBinder implements TypeBinder {

	@Override
	public void bind(TypeBindingContext context) {
		context
				.dependencies()
				.use("uuid")
				.use("name")
				.use("description")
				.use("file")
				.use("attributes")
				.use("infoContext")
				.use("kindList")
				.use("dateCreated")
				.use("issueDate")
				.use("store")
				.use("source")
				.use("stage")
				.use("storageItemIdentifier")
				.use("textParsed");

		IndexSchemaElement root = context.indexSchemaElement();

		// 1. Basic Fields
		root.field("uuid", f -> f.asString().projectable(Projectable.YES).sortable(Sortable.YES))
				.toReference();
		root.field(
				"name",
				f -> f.asString()
						.analyzer("standard")
						.projectable(Projectable.YES)
						.highlightable(EnumSet.of(Highlightable.ANY)))
				.toReference();
		root.field(
				"description",
				f -> f.asString()
						.analyzer("standard")
						.projectable(Projectable.YES)
						.highlightable(EnumSet.of(Highlightable.ANY)))
				.toReference();
		root.field(
				"filename",
				f -> f.asString()
						.analyzer("standard")
						.projectable(Projectable.YES)
						.highlightable(EnumSet.of(Highlightable.ANY)))
				.toReference();

		root.field("source", f -> f.asString().projectable(Projectable.YES)).toReference();
		root.field("textParsed", f -> f.asString().projectable(Projectable.YES)).toReference();

		// NEW PROJECTIONS
		root.field("stage", f -> f.asString().projectable(Projectable.YES)).toReference();
		root.field("storageItemIdentifier", f -> f.asString().projectable(Projectable.YES)).toReference();

		// Capture Reference 1: Fulltext
		IndexFieldReference<String> fulltextRef = root.field(
				"fulltext",
				f -> f.asString()
						.analyzer("standard")
						.projectable(Projectable.NO)
						.highlightable(EnumSet.of(Highlightable.ANY)))
				.toReference();

		// 2. Context (Folder)
		IndexSchemaObjectField infoContextObj = root.objectField("infoContext", ObjectStructure.FLATTENED);
		infoContextObj
				.field("name", f -> f.asString().analyzer("standard").projectable(Projectable.YES))
				.toReference();
		infoContextObj.field("uuid", f -> f.asString().projectable(Projectable.YES)).toReference();
		infoContextObj.toReference();

		// 3. Categories (Tags)
		IndexSchemaObjectField kindListObj = root.objectField("kindList", ObjectStructure.FLATTENED).multiValued();
		kindListObj
				.field("name", f -> f.asString().analyzer("standard").projectable(Projectable.YES))
				.toReference();
		kindListObj
				.field(
						"raw",
						f -> f.asString()
								.aggregable(Aggregable.YES)
								.projectable(Projectable.YES)
								.sortable(Sortable.YES))
				.toReference();
		kindListObj.field("uuid", f -> f.asString().projectable(Projectable.YES)).toReference();
		kindListObj.toReference();

		// 4. Store
		IndexSchemaObjectField storeObj = root.objectField("store", ObjectStructure.FLATTENED);
		storeObj
				.field("shortname", f -> f.asString().analyzer("standard").projectable(Projectable.YES))
				.toReference();
		storeObj.field("uuid", f -> f.asString().projectable(Projectable.YES)).toReference();
		storeObj.toReference();

		// 5. Dates
		root.field(
				"issueDate",
				f -> f.asLocalDate()
						.sortable(Sortable.YES)
						.aggregable(Aggregable.YES)
						.projectable(Projectable.YES))
				.toReference();
		root.field(
				"dateCreated",
				f -> f.asLocalDateTime().sortable(Sortable.YES).projectable(Projectable.YES))
				.toReference();

		// 6. Attributes
		IndexSchemaObjectField attrField = root.objectField("attr", ObjectStructure.FLATTENED);
		root.fieldTemplate("attributeTemplate", f -> f.asString().analyzer("standard"))
				.matchingPathGlob("attr.*");

		// Capture Reference: Attribute Object
		IndexObjectFieldReference attrObjRef = attrField.toReference();

		// Register Bridge with captured references
		context.bridge(DBItem.class, new ItemTextBridge(fulltextRef, attrObjRef));
	}
}