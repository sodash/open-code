package com.winterwell.utils.containers;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.winterwell.utils.Utils;

import junit.framework.TestCase;

public class ArrayMapTest extends TestCase {

	/**
	 * Check that having the mod counter wrap is OK
	 */
	public void testModOverload() {
		ArrayMap map = new ArrayMap();
		for (int i = 0; i < Character.MAX_VALUE * 2; i++) {
			map.put(i % 10, i);
			map.remove(2 + i % 10);
		}
		System.out.println(map);
	}
	

	public void testSort() {
		// lowest first
		ArrayMap map = new ArrayMap("A", 1, "B", 2, "C", 0);
		map.sort(null);
		System.out.println(map);
		assert map.first().first == "C";
		// Something custom: big evens first
		ArrayMap map2 = new ArrayMap("A", 1, "B", 2, "C", 3, "D", 4);
		map2.sort(new Comparator<Number>() {
			@Override
			public int compare(Number o1, Number o2) {
				if (o1.intValue() % 2 == 0) {
					if (o2.intValue() % 2 == 0) {
						return - Utils.compare(o1, o2);
					} else {
						return -1;
					}
				}
				if (o2.intValue() % 2 == 0) {
					return 1;
				}
				return - Utils.compare(o1, o2);
			}
		});
		// {D=4, B=2, C=3, A=1}
		System.out.println(map2);
		assert map2.first().first == "D";
	}


	public void testEntrySet1() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0);
		Set<Entry<String, Double>> set = map.entrySet();
		assert set.size() == 2;
	}

	public void testEntrySetSet() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0);
		Set<Entry<String, Double>> set = map.entrySet();
		Entry<String, Double> e = set.iterator().next();

		assert e.getKey().equals("first");
		assert e.getValue().equals(1.0);

		e.setValue(5.0);
		assert e.getValue().equals(5.0) : map;
		assert map.get("first") == 5 : map;
	}

	public void testEntrySet2() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>();
		Set<Entry<String, Double>> set = map.entrySet();
		Iterator<Entry<String, Double>> it = set.iterator();
		assert it.hasNext() == false;
	}

	public void testEntrySet3() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0);
		Set<Entry<String, Double>> set = map.entrySet();
		Iterator<Entry<String, Double>> it = set.iterator();
		assert it.hasNext();
		assert it.next().getValue() == 1.0;
		assert it.hasNext() == false;
	}

	public void testGetObject1() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0);
		assert map.get("first") == 1.0;
		assert map.get("second") == 2.0;
		assert map.get("third") == null;
		assert map.get(null) == null;
	}

	public void testGetObject2() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>();
		assert map.get("first") == null;
		assert map.get(null) == null;
	}

	public void testKeySet1() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0);
		Set<String> set = map.keySet();
		assert set.size() == 2;
		assert set.contains("first");
		assert set.contains("second");
	}

	public void offtestKeySet_null_key() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>(
				"first", 1.0, "second", 2.0, 
				null, 3.0);
		Set<String> set = map.keySet();
		assert set.size() == 3;
		assert set.contains("first");
		assert set.contains("second");
		assert set.contains(null);
	}

	public void testPutKV1() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0);
		assert map.put("third", 3.3) == null;
		assert map.get("third") == 3.3;
		assert map.size() == 3;
	}

	public void testPutKV2() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0);
		assert map.put("second", 3.4) == 2.0;
		assert map.get("second") == 3.4;
		assert map.size() == 2;
	}

	public void testPutKV3() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0);
		assert map.put("third", null) == null;
		assert map.put("second", null) == 2.0;
		assert map.get("second") == null;
		assert map.size() == 3;
	}

	// not supported
//	public void testPutNull() {
//		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
//				1.0, "second", 2.0);
//		assert map.put(null, 2.3) == null;
//		assert map.put(null, null) == 2.3;
//		assert map.get(null) == null;
//		assert map.size() == 3;
//	}

	public void testRemoveObject1() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0);
		assert map.remove("first") == 1.0;
		assert map.remove("third") == null;
		assert map.size() == 1;
		assert map.put("first", 25.1) == null;
	}

	public void testRemoveObject2() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>();
		assert map.remove("first") == null;
		assert map.size() == 0;
	}

	public void offtestRemoveObject_null_key() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>(null, 2.4);
		assert map.remove(null) == 2.4;
		assert map.size() == 0;
	}

	public void testSize() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>();
		assert map.size() == 0;
	}

	public void testValues1() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0);
		Collection<Double> col = map.values();
		assert col.size() == 2;
		assert col.contains(1.0);
		assert col.contains(2.0);
	}

	public void offtestValues_null_value() {
		ArrayMap<String, Double> map = new ArrayMap<String, Double>("first",
				1.0, "second", 2.0, null, 3.0);
		Collection<Double> col = map.values();
		assert col.size() == 3;
		assert col.contains(1.0);
		assert col.contains(2.0);
		assert col.contains(3.0);
	}

}
