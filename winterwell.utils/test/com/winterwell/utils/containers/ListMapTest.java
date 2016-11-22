package com.winterwell.utils.containers;

import java.util.Set;

import junit.framework.TestCase;

public class ListMapTest extends TestCase {

	public void testAdd1() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first list", 1.0);
		assert list.size() == 1;
		assert list.getOne("first list") == 1.0;
	}

	public void testAdd2() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first list", 1.0);
		list.add("second", 2.0);
		assert list.size() == 2;
		assert list.getOne("second") == 2.0;
	}

	public void testAdd3() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first list", 1.0);
		list.add("first list", 2.0);
		assert list.size() == 1;
		assert list.size("first list") == 2;
		assert list.get("first list").get(1) == 2.0;
	}

	public void testAdd4() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first list", 1.0);
		list.add("first list", 1.0);
		assert list.size() == 1;
		assert list.size("first list") == 2;
		assert list.get("first list").get(1) == 1.0;
	}

	public void testAdd5() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first list", 1.0);
		list.add(null, 1.0);
		assert list.size() == 2;
		assert list.size(null) == 1;
		assert list.getOne(null) == 1.0;
	}

	public void testAddOnce1() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first list", 1.0);
		assert list.addOnce("second", 2.0);
		assert list.size() == 2;
		assert list.getOne("second") == 2.0;
	}

	public void testAddOnce2() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first list", 1.0);
		assert list.addOnce("first list", 2.0);
		assert list.size() == 1;
		assert list.size("first list") == 2;
		assert list.get("first list").get(1) == 2.0;
	}

	public void testAddOnce3() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first list", 1.0);
		assert list.addOnce("first list", 1.0) == false;
		assert list.size() == 1;
		assert list.get("first list").size() == 1;
	}

	public void testAddOnce4() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first list", 1.0);
		list.addOnce(null, 1.0);
		assert list.size() == 2;
		assert list.size(null) == 1;
		assert list.getOne(null) == 1.0;
	}

	public void testGetOne() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first", 1.0);
		assert list.getOne("second") == null;
	}

	public void testGetValueSet1() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first", 1.0);
		list.add("first", 2.0);
		Set<Double> set = list.getValueSet();
		assert set.size() == 2;
		assert set.contains(1.0);
		assert set.contains(2.0);
	}

	public void testGetValueSet2() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first", 1.0);
		list.add("first", 1.0);
		Set<Double> set = list.getValueSet();
		assert set.size() == 1;
		assert set.contains(1.0);
	}

	public void testGetValueSet3() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first", null);
		list.add("second", 3.2);
		Set<Double> set = list.getValueSet();
		assert set.size() == 2;
		assert set.contains(null);
		assert set.contains(3.2);
	}

	public void testRemoveKV1() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first", 1.0);
		list.add("second", 2.0);
		assert list.removeOne("first", 1.0);
		assert list.size() == 1;
		assert list.containsKey("first") == false;
	}

	public void testRemoveKV2() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first", 1.0);
		list.add("first", 2.0);
		assert list.removeOne("first", 1.0);
		assert list.size() == 1;
		assert list.getOne("first") == 2.0;
		assert list.size("first") == 1;
	}

	public void testRemoveKV3() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		assert list.removeOne("first", 1.0) == false;
		assert list.size() == 0;
	}

	public void testRemoveKV4() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first", 1.0);
		list.add("first", 1.0);
		assert list.removeOne("first", 1.0);
		assert list.size() == 1;
		assert list.getOne("first") == 1.0;
		assert list.size("first") == 1;
	}

	public void testSizeK1() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		assert list.size("first") == 0;
	}

	public void testSizeK2() {
		ListMap<String, Double> list = new ListMap<String, Double>();
		list.add("first", 1.0);
		list.add("first", 2.0);
		assert list.size("first") == 2;
	}

}
