package com.winterwell.utils.containers;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class IntRangeTest extends TestCase {

	public void testIterator() {
		IntRange list = new IntRange(1, 4);
		List<Integer> lst = Containers.getList(list);
		System.out.println(lst);
		assert lst.equals(Arrays.asList(1, 2, 3, 4));
	}

	public void testGetInt() {
		IntRange list = new IntRange(10, 14);
		assert list.get(0) == 10;
		assert list.get(4) == 14;
	}

	public void testIsIn() {
		IntRange list = new IntRange(0, 4);
		assert list.contains(0);
		assert list.contains(4);
		assert list.contains(-1) == false;
		assert list.contains(5) == false;
	}

	public void testSize1() {
		IntRange list = new IntRange(0, 1);
		assert list.size() == 2;
	}

	public void testSize2() {
		IntRange list = new IntRange(2, 0);
		assert list.size() == 3;
	}

}
