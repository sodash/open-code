package com.winterwell.utils.containers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * BubbleSort: the textbook example of a sensible yet inefficient algorithm. But
 * - it allows the sort to take into account the top candidates. This allows you
 * to do a sort that rewards diversity. Which is why we have it.
 * 
 * @testedby BubbleSortTest
 * @author daniel
 */
public abstract class BubbleSort<X> {

	private ArrayList<X> selectd;

	private Object[] unsorted;

	public BubbleSort() {
	}

	/**
	 * Perform the sort.
	 * 
	 * @return sorted copy of the original list.
	 */
	public List<X> getSorted(Collection<? extends X> unsortd) {
		this.unsorted = unsortd.toArray();
		selectd = new ArrayList<X>(unsorted.length);
		for (int i = 0; i < unsorted.length; i++) {
			int bestIndex = i;
			X best = (X) unsorted[i];
			for (int j = i + 1; j < unsorted.length; j++) {
				X poss = (X) unsorted[j];
				boolean cmp = inOrder(best, poss, selectd);
				if (!cmp) {
					best = poss;
					bestIndex = j;
				}
			}
			// swap into place
			Object tmp = unsorted[i];
			// unsorted[i] = best;
			unsorted[bestIndex] = tmp;
			selectd.add(best);
		}
		return selectd;
	}

	/**
	 * Compare two elements
	 * 
	 * @param first
	 * @param second
	 * @param selected
	 *            the elements already selected, i.e. sorted to the front of the
	 *            list.
	 * @return true if first should be chosen ahead of second.
	 */
	protected abstract boolean inOrder(X first, X second, List<X> selected);

}
