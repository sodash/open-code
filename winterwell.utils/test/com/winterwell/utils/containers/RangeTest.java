package com.winterwell.utils.containers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class RangeTest extends TestCase {
	
	public void testMaxFinite() {
		Range range = new Range(-Double.MAX_VALUE, Double.MAX_VALUE);
//		System.out.println(range);
		assert range.contains(10000000);
		assert range.contains(-10000000);
		assert range.contains(-10);
		assert range.contains(10);
	}

	
	public void testIsIn() {
		Range range = new Range(0, 4);
		assert range.contains(0);
		assert range.contains(4);
		assert range.contains(-0.10) == false;
		assert range.contains(4.001) == false;
	}

	public void testSize1() {
		Range range = new Range(0, 1);
		assert range.size() == 1;
	}

	public void testSize2() {
		Range range = new Range(2.5, -2);
		assert range.size() == 4.5;
	}

	public void testSort() {
		Range a = new Range(-1, 1);
		Range b = new Range(1, 10);
		Range c = new Range(2, 3);
		Range d = new Range(3, 4);

		List<Range> list = Arrays.asList(d, b, a, c);

		Collections.sort(list);

		assert list.get(0) == a;
		assert list.get(1) == b;
		assert list.get(2) == c;
		assert list.get(3) == d;
	}

	public void testToString1() {
		Range range = new Range(0, 4);
		assert range.toString().equals("[0.0,4.0]");
	}

	public void testToString2() {
		Range range = new Range(2.5, -2);
		assert range.toString().equals("[-2.0,2.5]");
	}
}
