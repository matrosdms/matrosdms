/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.schwehla.matrosdms.adapter.ParentArrayAdapter;

import io.swagger.v3.oas.annotations.media.Schema;

public class MCategory extends MBaseElement {

	public MCategory() {
		super();
	}

	private static final long serialVersionUID = 1L;

	// Category from InfoItem not from Context (wer,was,wo)
	boolean dropfieldCategory;

	private boolean object;

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	// @JsonIgnoreProperties("parents")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<MCategory> children = new ArrayList<>();

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	// @JsonIgnoreProperties("children")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<MCategory> parents = new ArrayList<>();

	public boolean isObject() {
		return object;
	}

	public void setObject(boolean object) {
		this.object = object;
	}

	@JsonSerialize(converter = ParentArrayAdapter.class)
	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	public List<MCategory> getParents() {
		return parents;
	}

	public void setParents(List<MCategory> parents) {
		this.parents = parents;
	}

	@Schema(accessMode = Schema.AccessMode.READ_ONLY)
	public List<MCategory> getChildren() {
		return children;
	}

	public void setChildren(List<MCategory> children) {
		this.children = children;
	}

	@JsonIgnore
	public List<MCategory> getInfoObjectList(boolean recursive) {

		List<MCategory> temp = new ArrayList<MCategory>();

		for (MCategory v : children) {

			if (v.isObject()) {
				temp.add(v);
			}

			if (recursive) {
				temp.addAll(v.getInfoObjectList(recursive));
			}
		}

		return temp;
	}

	/*
	 * public Identifier getInfoTyp() { return getRoot().getIdentifier(); }
	 */
	@JsonIgnore
	public List<MCategory> getRootElements() {
		List<MCategory> result = new ArrayList<MCategory>();

		if (parents.size() == 0) {
			result.add(this);
			return result;
		}

		Iterator<MCategory> enumerator = parents.iterator();

		while (enumerator.hasNext()) {
			MCategory item = enumerator.next();
			result.addAll(item.getRootElements());
		}

		return result;
	}

	public void connectWithChild(MCategory child) {
		child.parents.add(this);
		// child.ParentIdList.Add(this.Id);
		children.add(child);
	}

	@JsonIgnore
	public Set<MCategory> getSelfAndAllTransitiveChildren() {
		HashSet<MCategory> temp = new HashSet<MCategory>();

		temp.add(this);

		for (MCategory v : children) {
			temp.addAll(v.getSelfAndAllTransitiveChildren());
		}

		return temp;
	}

	/** Keeps the order */
	public void appendOrderedTransitiveChildren(List<MCategory> initialList) {

		initialList.addAll(children);
		for (MCategory v : children) {
			v.appendOrderedTransitiveChildren(initialList);
		}
	}

	/** Liefert alle InfoCategories als Set */
	@JsonIgnore
	public HashSet<MCategory> getSelfAndTransitiveParents() {
		HashSet<MCategory> temp = new LinkedHashSet<MCategory>();

		temp.add(this);
		for (MCategory v : parents) {
			temp.addAll(v.getSelfAndTransitiveParents());
		}

		return temp;
	}

	/**
	 * Per definition gibt es einen rootknoten welcher die Category ausmacht
	 *
	 * @return
	 */
	@JsonIgnore
	public MCategory getRoot() {

		MCategory root = this;

		List<MCategory> p = parents;
		while (p.size() > 0) {
			root = p.get(0);
			p = root.getParents();
		}

		return root;
	}
}
