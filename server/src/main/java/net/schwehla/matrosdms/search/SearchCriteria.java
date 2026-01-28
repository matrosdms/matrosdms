/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.search;

import java.util.List;

import net.schwehla.matrosdms.domain.search.EOperator;
import net.schwehla.matrosdms.domain.search.ESearchDimension;

public class SearchCriteria {
	public enum Logic {
		AND, OR, NOT
	}

	public enum Type {
		GROUP, FILTER
	}

	private Type type = Type.FILTER;
	private Logic logic = Logic.AND;
	private List<SearchCriteria> children;
	private ESearchDimension field;
	private EOperator operator;
	private String value;

	public static SearchCriteria forText(String text) {
		SearchCriteria sc = new SearchCriteria();
		sc.setType(Type.FILTER);
		sc.setField(ESearchDimension.FULLTEXT);
		sc.setOperator(EOperator.CONTAINS);
		sc.setValue(text);
		return sc;
	}

	// Getters & Setters
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Logic getLogic() {
		return logic;
	}

	public void setLogic(Logic logic) {
		this.logic = logic;
	}

	public List<SearchCriteria> getChildren() {
		return children;
	}

	public void setChildren(List<SearchCriteria> children) {
		this.children = children;
	}

	public ESearchDimension getField() {
		return field;
	}

	public void setField(ESearchDimension field) {
		this.field = field;
	}

	public EOperator getOperator() {
		return operator;
	}

	public void setOperator(EOperator operator) {
		this.operator = operator;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
