package com.winterwell.utils.containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.Test;

import winterwell.utils.IFn;

import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.threads.SafeExecutor;

public class ContainersTest {

	@Test
	public void testMultiThreadPlus() throws InterruptedException {
		AtomicReference<Throwable> err = new AtomicReference();
		SafeExecutor exe = new SafeExecutor(Executors.newFixedThreadPool(100));
		final ConcurrentHashMap<String, Double> map = new ConcurrentHashMap<String, Double>();
		for (int i = 0; i < 10 * 10000; i++) {
			final int fi = i % 10;
			exe.submit(new Runnable() {
				public void run() {
					Containers.plus(map, "k" + fi, 1);
				}
			});
		}
		exe.shutdown();
		exe.awaitTermination(100, TimeUnit.SECONDS);
		assert exe.getLastException() == null : exe.getLastException();

		System.err.println(map);
		for (String k : map.keySet()) {
			Double v = map.get(k);
			assert v == 10000 : k + "=" + v;
		}
	}

	@Test
	public void testGetter() {
		IFn first = Containers.getter(0);
		IFn second = Containers.getter(1);
		List<Pair> ab = Arrays.asList(new Pair("a", 1), new Pair("b", 2));
		List abc = Containers.apply(first, ab);
		List num = Containers.apply(second, ab);
		assert abc.equals(Arrays.asList("a", "b")) : abc;
		assert num.equals(Arrays.asList(1, 2)) : num;
	}

	@Test
	public void testEasy() {
		ArrayList list = new ArrayList();
		list.add("a");
		list.add("b");
		list.add("c");
		list.add("d");
		List easy = Containers.easy(list);
		String seen = "";
		for (Object object : easy) {
			seen += object;
			if ("b" == object) {
				easy.remove(object);
			}
		}
		assert seen.equals("abcd") : seen;
		assert list.equals(Arrays.asList("a", "c", "d")) : list;
		assert easy.equals(Arrays.asList("a", "c", "d")) : list;
		assert list.equals(Arrays.asList("a", "c", "d")) : list;
		assert list.size() == 3;
		assert easy.size() == 3;

		String seen2 = "";
		for (Object object : easy) {
			seen2 += object;
			if ("d" == object)
				easy.remove(object);
		}
		assert seen2.equals("acd") : seen2;
		assert list.equals(Arrays.asList("a", "c")) : list;
		assert easy.equals(Arrays.asList("a", "c")) : list;
		assert list.equals(Arrays.asList("a", "c")) : list;
		assert list.size() == 2;
		assert easy.size() == 2;
	}
	
	@Test
	public void testAsListStrings() {
		{	// Is it needed? Only for primitives
			Object array = new String[] {"A", "B"};
			List<Object> list = Arrays.asList((Object[])array);
			assert list.get(0).equals("A");
			assert list.get(1).equals("B");
			assert list.size() == 2;
		}
		{	// Is it needed? Only for primitives
			String[] array = new String[] {"A", "B"};
			Object thisWouldFail = array; // compile time type knowledge matters!
			List list = Arrays.asList(array);
			assert list.get(0).equals("A") : list;
			assert list.get(1).equals("B");
			assert list.size() == 2;
		}

	}

	@Test
	public void testArrayAsList1() {
		{	// Is it needed? Only for primitives
			Object array = new String[] {"A", "B"};
			List<Object> list = Arrays.asList((Object[])array);
			assert list.get(0).equals("A");
			assert list.get(1).equals("B");
			assert list.size() == 2;
		}
		{	// Doesn't work for ints
			int[] array = new int[] { 1, 2 };
			List list = Arrays.asList(array);
			assert list.get(0).getClass().isArray();
		}
		int[] array = new int[] { 1, 2 };
		List list = Containers.asList(array);
		assert (Integer) list.get(0) == 1;
		assert (Integer) list.get(1) == 2;
		assert list.size() == 2;
	}

	@Test
	public void testArrayAsList2() {
		try {
			int array = 1;
			List list = Containers.asList(array);
			assert false;
		} catch (IllegalArgumentException ex) {
			// OK
		}

	}

	@Test
	public void testArrayAsList3() {
		String[] array = new String[] { "first", null, "third" };
		List list = Containers.asList(array);
		assert list.get(0).equals("first");
		assert list.get(1) == null;
		assert list.get(2).equals("third");
		assert list.size() == 3;
	}

	@Test
	public void testChop() {
		List<String> data = Arrays.asList("a", "b", "c", "d", "e");
		List<List<String>> bits = Containers.chop(data, 2);
		assert bits.size() == 2;

		Assert.assertEquals(bits.get(0), Arrays.asList("a", "b"));
		Assert.assertEquals(bits.get(1), Arrays.asList("c", "d", "e"));
	}

	public void testCompare1() {
		assert Containers.compare(1, 1) == 0;
		assert Containers.compare("string", "string") == 0;
	}

	@Test
	public void testCompare2() {
		assert Containers.compare(0, null) == -1;
		assert Containers.compare(null, 0) == 1;
		assert Containers.compare(null, null) == 0;

		// however, this fails if the underlying compare throws an exception
		try {
			Containers.compare("string", 2.45);
		} catch (ClassCastException ex) {
			// oh well
		}

	}

	@Test
	public void testDifferences() {
		Set<String> set1 = new HashSet<String>(Arrays.asList("1", "2", "1",
				"3", "4"));
		Set<String> set2 = new HashSet<String>(Arrays.asList("1", "4", "7"));
		Containers.Changes<String> diffs = Containers.differences(set1, set2);
		Assert.assertEquals(Arrays.asList("3", "2"), diffs.getAdded()); // Not
																		// happy
																		// about
																		// the
																		// order-sensitivity
																		// here
		Assert.assertEquals(Arrays.asList("7"), diffs.getDeleted());
	}

	@Test
	public void testEntrySet() {
		List<Integer> data = Arrays.asList(1, 2, 3, 4, 1, 2, 2);
		Set<Integer> entries = Containers.listAsSet(Arrays.asList(1, 2, 3, 4));
		Assert.assertEquals(entries, Containers.entrySet(data));
	}

	@Test
	public void testFirst1() {
		List<String> list1 = new ArrayList<String>();
		list1.add("first");
		list1.add("second");
		assert Containers.first(null, list1).equals("first");

		assert Containers.first(null, new ArrayList()) == null;
	}

	@Test
	public void testFirst2() {
		List<String> list1 = new ArrayList<String>();
		list1.add("first");
		assert Containers.first(null, list1).equals("first");
	}

	@Test
	public void testFlatten() {
		List<Object> listOLists = Arrays
				.asList("a", "b", Arrays.asList("c", "d"),
						Arrays.asList("e", Arrays.asList("f")));
		List<Object> abcdef = Containers.flatten(listOLists);
		assert StrUtils.join(abcdef, ",").equals("a,b,c,d,e,f") : abcdef;
	}

	@Test
	public void testGetSortedKeys() {
		Map map = new ArrayMap("foo", 1, "bar", 4, "foo2", 7, "bar2", 2);
		List keys = Containers.getSortedKeys(map);
		assert keys.equals(Arrays.asList("foo", "bar2", "bar", "foo2")) : keys;
	}

	@Test
	public void testGetSortedKeys2() {
		Map map = new ArrayMap("foo", 4, "null", null);
		List keys = Containers.getSortedKeys(map);
		assert keys.equals(Arrays.asList("foo", "null")) : keys;
	}

	
	@Test
	public void testPluck() {			
		List<Map> list = Arrays.asList(new ArrayMap("a",1), null, new ArrayMap("a",null), new ArrayMap("a",2));
		List plucked = Containers.pluckNotNull(list, m -> m.get("a"));
		assert Containers.same(plucked, 1, 2);
	}

	/**
	 * @tests {@link Containers#getValueSortedMap(Map, boolean)}
	 */
	@Test
	public void testGetValueSortedMap() {
		{ // smallest first
			Map map = new ArrayMap("foo", 1, "bar", 4, "foo2", 7, "bar2", 2);
			Map<Object, Object> map2 = Containers.getValueSortedMap(map, true,
					3);
			List<Object> keys = Containers.getList(map2.keySet());
			assert keys.get(0).equals("foo") : keys;
			assert keys.get(1).equals("bar2") : keys;
			assert keys.get(2).equals("bar");
		}
		{ // largest first
			Map map = new ArrayMap("foo", 1, "bar", 4, "foo2", 7, "bar2", 2);
			Map<Object, Object> map2 = Containers.getValueSortedMap(map, false,
					-1);
			List<Object> keys = Containers.getList(map2.keySet());
			assert keys.get(0).equals("foo2");
			assert keys.get(1).equals("bar");
			assert keys.get(2).equals("bar2");
			assert keys.get(3).equals("foo");
		}
	}

	@Test
	public void testIndexOf1() {
		double[] array = new double[] { 2.5, 1.3, 8.9 };
		assert Containers.indexOf(2.5, array) == 0;
		assert Containers.indexOf(8.9, array) == 2;
		assert Containers.indexOf(0, array) == -1;
		assert Containers.indexOf("gg", array) == -1;
		assert Containers.indexOf(null, array) == -1;
	}

	@Test
	public void testIntersects() {
		Set<Integer> set1 = new TreeSet<Integer>(Arrays.asList(1, 2, 1, 3, 4));
		Set<Integer> set2 = new TreeSet<Integer>(Arrays.asList(1, 4, 7));
		Set<Integer> set3 = Collections.singleton(7);
		Assert.assertTrue(Containers.intersects(set1, set1));
		Assert.assertTrue(Containers.intersects(set1, set2));
		Assert.assertFalse(Containers.intersects(set1, set3));
		Assert.assertTrue(Containers.intersects(set2, set1));
		Assert.assertTrue(Containers.intersects(set2, set2));
		Assert.assertTrue(Containers.intersects(set2, set3));
		Assert.assertFalse(Containers.intersects(set3, set1));
		Assert.assertTrue(Containers.intersects(set3, set2));
		Assert.assertTrue(Containers.intersects(set3, set3));
	}

	@Test
	public void testLast1() {
		List<String> list1 = new ArrayList<String>();
		list1.add("first");
		list1.add("second");
		assert Containers.last(list1).equals("second");
	}

	@Test
	public void testLast2() {
		List<String> list1 = new ArrayList<String>();
		list1.add("first");
		assert Containers.last(list1).equals("first");
	}

	@Test
	public void testListAsSet1() {
		List list = new ArrayList();
		list.add('a');
		list.add('b');
		Set set = Containers.listAsSet(list);
		assert set.size() == 2;
		assert set.containsAll(list);
	}

	@Test
	public void testListAsSet2() {
		List list = new ArrayList();
		Set set = Containers.listAsSet(list);
		assert set.size() == 0;
		assert set.removeAll(list) == false;
	}

	@Test
	public void testPlus() {
		Map<String, Integer> map = new ArrayMap<String, Integer>("foo", 1,
				"bar", 4, "foo2", 7, "bar2", 2);
		assert Containers.plus(map, "foo", 1) == 2;
		assert Containers.plus(map, "fo", 1) == 1;
		// null key??
//		assert Containers.plus(map, null, 1) == 1;
	}

	@Test
	public void testSetDifference() {
		Set<Integer> set1 = new TreeSet<Integer>(Arrays.asList(1, 2, 1, 3, 4));
		Set<Integer> set2 = new TreeSet<Integer>(Arrays.asList(1, 4, 7));
		Assert.assertEquals(Arrays.asList(2, 3),
				Containers.setDifference(set1, set2));
		Assert.assertEquals(Arrays.asList(7),
				Containers.setDifference(set2, set1));
	}

	public void testSubList() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
		List<Integer> sub = Containers.subList(list, -4, 0);
		Assert.assertEquals(Printer.toString(sub), Arrays.asList(7, 8, 9, 0),
				sub);

		sub = Containers.subList(list, -0, 3);
		Assert.assertEquals(Printer.toString(sub), Arrays.asList(1, 2, 3), sub);
	}

	@Test
	public void testTranspose() {
		final int[] _a = { 32, 9, 12 };
		final int[] _b = { 1, 2, 3 };
		final List<Integer> list_a = Containers.asList(_a);
		final List<Integer> list_b = Containers.asList(_b);
		List<List<Integer>> in = new ArrayList<List<Integer>>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add(list_a);
				add(list_b);
			}
		};

		final int[] _1 = { 32, 1 };
		final int[] _2 = { 9, 2 };
		final int[] _3 = { 12, 3 };
		final List<Integer> list_1 = Containers.asList(_1);
		final List<Integer> list_2 = Containers.asList(_2);
		final List<Integer> list_3 = Containers.asList(_3);
		List<List<Integer>> expected = new ArrayList<List<Integer>>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add(list_1);
				add(list_2);
				add(list_3);
			}
		};

		Assert.assertEquals(expected, Containers.transpose(in));
	}

	@Test
	public void testTransposeDifferentLengths() {
		final int[] list4 = { 32, 9, 12, -100 };
		final int[] list3 = { 1, 2, 3 };
		List<List<Integer>> a = new ArrayList<List<Integer>>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add(Containers.asList(list4));
				add(Containers.asList(list3));
			}
		};
		Boolean threw = false;
		try {
			Containers.transpose(a);
		} catch (IllegalArgumentException e) {
			threw = true;
		}
		Assert.assertTrue(threw);
	}

	@Test
	public void testTransposeEmpty() {
		List<List<Integer>> a = new ArrayList<List<Integer>>();
		List<List<Integer>> b = Containers.transpose(a);
		Assert.assertTrue(b.isEmpty());
	}

	@Test
	public void testTransposeZeroLength() {
		List<List<Integer>> a = new ArrayList<List<Integer>>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add(new ArrayList<Integer>());
			}
		};
		Boolean threw = false;
		try {
			Containers.transpose(a);
		} catch (IllegalArgumentException e) {
			threw = true;
		}
		Assert.assertTrue(threw);
	}
}
