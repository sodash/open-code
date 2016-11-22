package com.winterwell.utils.containers;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * 
 * 
 * 
 * @tests {@link BubbleSort}
 * @author daniel
 */
public class BubbleSortTest extends TestCase {

	public void testGetSorted1() {
		List<String> list = Arrays.asList("A", "C", "B", "D");
		BubbleSort<String> sorter = new BubbleSort<String>() {
			@Override
			public boolean inOrder(String a, String b, List<String> selected) {
				if (selected.contains(a))
					return false;
				if (selected.contains(b))
					return true;
				return (a.compareTo(b) <= 0);
			}
		};
		List<String> sorted = sorter.getSorted(list);
		assertEquals(Arrays.asList("A", "B", "C", "D"), sorted);
	}

	public void testGetSorted2() {
		List<String> list = Arrays.asList("A", "C", "A", "B", "D");
		BubbleSort<String> sorter = new BubbleSort<String>() {
			@Override
			public boolean inOrder(String a, String b, List<String> selected) {
				if (selected.contains(a))
					return false;
				if (selected.contains(b))
					return true;
				return (a.compareTo(b) <= 0);
			}
		};
		List<String> sorted = sorter.getSorted(list);
		assertEquals(Arrays.asList("A", "B", "C", "D", "A"), sorted);
		// check original unchanged and repeat
		assertEquals(Arrays.asList("A", "C", "A", "B", "D"), list);
		sorted = sorter.getSorted(list);
		assertEquals(Arrays.asList("A", "B", "C", "D", "A"), sorted);

	}

}
