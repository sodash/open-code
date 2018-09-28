package com.winterwell.utils.containers;


import junit.framework.TestCase;

public class ArraySetTest extends TestCase {

	public void testContains() {
		ArraySet<String> array = new ArraySet<String>();
		array.add("a");
		array.add("b");
		array.add("a");
		assert array.size() == 2;
		assert array.contains("a");
		assert array.contains("b");
		assert !array.contains("c");
	}

	public void testAddT1() {
		ArraySet<String> array = new ArraySet<String>();
		assert array.add("first");
		assert array.size() == 1;
	}

	public void testAddT2() {
		ArraySet<String> array = new ArraySet<String>("first");
		assert array.add("second");
		assert array.size() == 2;
	}

	public void testAddT3() {
		ArraySet<String> array = new ArraySet<String>("first", "second");
		assert array.add("first") == false;
		assert array.size() == 2;
	}

	public void testAddT4() {
		ArraySet<String> array = new ArraySet<String>("first", "second");
		assert ! array.add(null);
		assert array.size() == 2;
	}

	public void testAddT5() {
		ArraySet<String> array = new ArraySet<String>("first", null);
		assert array.add(null) == false;
		assert array.size() == 1;
	}

	public void testRemoveObject1() {
		ArraySet<String> array = new ArraySet<String>("first", "second");
		assert array.remove("first");
		assert array.size() == 1;
	}

	public void testRemoveObject2() {
		ArraySet<String> array = new ArraySet<String>("first", "second");
		assert array.remove("second");
		assert array.size() == 1;
	}

	public void testRemoveObject3() {
		ArraySet<String> array = new ArraySet<String>("first", "second");
		assert array.remove("foo") == false;
		assert array.size() == 2;
	}

	public void testRemoveObject4() {
		ArraySet<String> array = new ArraySet<String>("first", "second");
		assert array.remove(null) == false;
		assert array.size() == 2;
	}

	public void testRemoveObject5() {
		ArraySet<String> array = new ArraySet<String>("first", "second", null);
		array.remove(null);
		assert array.size() == 2;
	}

	public void testRemoveObject6() {
		ArraySet<String> array = new ArraySet<String>();
		assert array.remove("foo") == false;
		assert array.size() == 0;
	}

	public void testRemoveObject7() {
		ArraySet<String> array = new ArraySet<String>();
		assert array.remove(null) == false;
		assert array.size() == 0;
	}

	public void testSize1() {
		ArraySet<String> array = new ArraySet<String>(0);
		assert array.size() == 0;
	}

	public void testSize2() {
		ArraySet<String> array = new ArraySet<String>();
		assert array.size() == 0;
	}

	public void testSize3() {
		ArraySet<String> array = new ArraySet<String>(5);
		// initialSize= size or capacity?
		assert array.size() == 0;
	}

	public void testSize4() {
		ArraySet<String> array = new ArraySet<String>("first", "second",
				"third");
		assert array.size() == 3;
	}

	public void testSize5() {
		ArraySet<String> array = new ArraySet<String>("first", "second", null);
		assert array.size() == 2;
	}
}
