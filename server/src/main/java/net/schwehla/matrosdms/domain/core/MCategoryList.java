/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class MCategoryList extends MBaseElement implements List<MCategory> {

	private static final long serialVersionUID = 1L;

	public MCategoryList() {
	}

	List<MCategory> elements = new ArrayList<MCategory>();

	@Override
	public boolean add(MCategory e) {
		return elements.add(e);
	}

	@Override
	public void add(int index, MCategory element) {
		elements.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends MCategory> c) {
		return elements.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends MCategory> c) {
		return elements.addAll(index, c);
	}

	@Override
	public void clear() {
		elements.clear();
	}

	@Override
	public boolean contains(Object o) {
		return elements.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return elements.contains(c);
	}

	@Override
	public MCategory get(int index) {
		return elements.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return elements.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	@Override
	public Iterator<MCategory> iterator() {
		return elements.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return lastIndexOf(o);
	}

	@Override
	public ListIterator<MCategory> listIterator() {
		return listIterator();
	}

	@Override
	public ListIterator<MCategory> listIterator(int index) {
		return elements.listIterator(index);
	}

	@Override
	public boolean remove(Object o) {
		return elements.remove(o);
	}

	@Override
	public MCategory remove(int index) {
		return elements.remove(index);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return elements.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return elements.retainAll(c);
	}

	@Override
	public MCategory set(int index, MCategory element) {
		return elements.set(index, element);
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public List<MCategory> subList(int fromIndex, int toIndex) {
		return elements.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return elements.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return elements.toArray(a);
	}

	/**
	 * Hier k�nnte eine Optimierung gemacht werden
	 *
	 * @param currentList
	 * @return
	 */
	public boolean checkRekursivKategoriePasstRekursiv(MCategoryList currentList) {

		if (currentList == null || currentList.isEmpty()) {
			return false;
		}

		// can use cached graph if i want to optimize

		Set<MCategory> imTagGraph = getAllTransitiveChildren();
		return imTagGraph.containsAll(currentList);
	}

	public HashSet<MCategory> getAllTransitiveChildren() {

		HashSet<MCategory> tmp;

		// HashSet<ITagInterface>
		tmp = new HashSet<MCategory>();

		for (MCategory e : elements) {
			tmp.addAll(e.getSelfAndAllTransitiveChildren());
		}

		return tmp;
	}

	public HashSet<MCategory> getSelfAndTransitiveParents() {

		HashSet<MCategory> tmp;

		// HashSet<ITagInterface>
		tmp = new HashSet<MCategory>();

		for (MCategory e : elements) {
			tmp.addAll(e.getSelfAndTransitiveParents());
		}

		return tmp;
	}
}
