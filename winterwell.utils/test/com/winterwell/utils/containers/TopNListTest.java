package com.winterwell.utils.containers;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class TopNListTest {

	@Test
	public void testScored() {
		TopNList<String> list = new TopNList<String>(3);

		assert list.size() == 0;
		boolean added = list.maybeAdd("A", 1);
		assert added;
		assert list.size() == 1;

		added = list.maybeAdd("C", 3);
		assert added;
		assert list.size() == 2;
		// Printer.out(list);

		added = list.maybeAdd("B", 2);
		assert added;
		assert list.size() == 3;
		// Printer.out(list);

		added = list.maybeAdd("B", 2);
		assert added;
		assert list.size() == 3;
		
		added = list.maybeAdd("D", 4);
		assert added;
		assert list.size() == 3;

		assert list.get(0).equals("D");
		assert list.get(1).equals("C");
		assert list.get(2).equals("B");

		ArrayList copy = new ArrayList(list);
		assert copy.get(0).equals("D");
		assert copy.get(1).equals("C");
		assert copy.get(2).equals("B");
	}

	@Test
	/**
	 * based on a real example.
	 */
	public void testScored2RealCase() {
		TopNList<String> list = new TopNList<String>(3);
		list.maybeAdd("huxleysrvr", 150.0);
		list.maybeAdd("spoon", 150.0);
		list.maybeAdd("fork", 1150.0);
		
		assert list.get(0).equals("fork");
		assert list.get(1).equals("spoon")||list.get(1).equals("huxleysrvr");
		assert list.get(2).equals("spoon")||list.get(1).equals("huxleysrvr");
		
	}

	@Test
	public void testBugFromWWModelDescriptionTest() {
		TopNList<String> list = new TopNList<String>(2);
		list.maybeAdd(":)", 0.6);
		list.maybeAdd("happy", 0.6);
		assert list.size() == 2 : list;
		assert list.contains("happy") : list;
		assert list.contains(":)") : list;
	}

	@Test
	public void testMaybeAdd() {
		TopNList<String> list = new TopNList<String>(3);

		assert list.size() == 0;
		boolean added = list.maybeAdd("A");
		assert added;
		assert list.size() == 1;

		added = list.maybeAdd("C");
		assert added;
		assert list.size() == 2;
		// Printer.out(list);

		added = list.maybeAdd("B");
		assert added;
		assert list.size() == 3;
		// Printer.out(list);

		added = list.maybeAdd("D");
		assert !added;
		assert list.size() == 3;

		assert list.equals(Arrays.asList("A", "B", "C"));

	}

	@Test
	public void testMaybeAdd2() {
		TopNList<Integer> list = new TopNList<Integer>(3);
		for (int i : new int[] { 1, 4, 2, 3, 5 }) {
			list.maybeAdd(i);
		}
		assert list.size() == 3;
		assert list.equals(Arrays.asList(1, 2, 3));
	}

}
