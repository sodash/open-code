package com.winterwell.utils.containers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class Pair2Test extends TestCase {

	public void testEqualsObject1() {
		Pair2<Character, Double> pair1 = new Pair2<Character, Double>('a', -1.2);
		Pair2<Character, Double> pair2 = new Pair2<Character, Double>('a', -1.2);
		assert pair1.equals(pair2);
	}

	public void testEqualsObject2() {
		Pair2<Character, Integer> pair1 = new Pair2<Character, Integer>('a', -1);
		Pair2<Character, Double> pair2 = new Pair2<Character, Double>('a', -1.0);
		assert pair1.equals(pair2) == false;
	}

	public void testEqualsObject3() {
		Pair2<Character, Integer> pair1 = new Pair2<Character, Integer>('a',
				null);
		Pair2<Character, Integer> pair2 = new Pair2<Character, Integer>('a',
				null);
		assert pair1.equals(pair2);
	}

	public void testEqualsObject4() {
		Pair2<Character, String> pair1 = new Pair2<Character, String>(null, "a");
		Pair2<Character, Character> pair2 = new Pair2<Character, Character>(
				null, 'a');
		assert pair1.equals(pair2) == false;
	}

	public void testEqualsObject5() {
		Pair2<Character, Character> pair1 = new Pair2<Character, Character>(
				'a', null);
		assert pair1.equals(null) == false;
	}

	public void testEqualsObject6() {
		Pair2<Character, Character> pair1 = new Pair2<Character, Character>(
				'a', null);
		Pair2<Character, Character> pair2 = pair1;
		assert pair1.equals(pair2);
	}

	public void testHashCode() {
		Pair2<Character, Double> a = new Pair2<Character, Double>('a', 23.8);
		Pair2<Character, Double> b = new Pair2<Character, Double>('a', 23.8);
		Pair2<Character, Double> c = new Pair2<Character, Double>('c', -0.46);
		Pair2<Double, Character> d = new Pair2<Double, Character>(0.9, 'c');
		assert a.hashCode() == b.hashCode();
		assert a.hashCode() != c.hashCode();
		assert a.hashCode() != d.hashCode();
	}

	public void testSort() {
		{
			Pair2<String, Object> aa = new Pair2<String, Object>("a", "a");
			Pair2<String, Object> ba = new Pair2<String, Object>("b", "a");
			Pair2<String, Object> ab = new Pair2<String, Object>("a", "b");
			Pair2<String, Object> bb = new Pair2<String, Object>("b", "b");

			List<Pair2<String, Object>> list = Arrays.asList(bb, ba, ab, aa);
			Collections.sort(list);

			assert list.get(0) == aa;
			assert list.get(1) == ab;
			assert list.get(2) == ba;
			assert list.get(3) == bb;
		}
		{
			Pair2<String, Object> aa = new Pair2<String, Object>("a", "a");
			Pair2<String, Object> ba = new Pair2<String, Object>(null, "a");
			Pair2<String, Object> ab = new Pair2<String, Object>("a", null);
			Pair2<String, Object> bb = new Pair2<String, Object>(null, null);

			List<Pair2<String, Object>> list = Arrays.asList(bb, ba, ab, aa);
			Collections.sort(list);

			assert list.get(0) == aa;
			assert list.get(1) == ab;
			assert list.get(2) == ba : list;
			assert list.get(3) == bb;
		}
		{ // check non-comparable 2nd place items are ok
			Pair2<String, Object> aa = new Pair2<String, Object>("a", "a");
			Pair2<String, Object> ba = new Pair2<String, Object>(null, "a");
			Pair2<String, Object> ax = new Pair2<String, Object>("a",
					new Object());
			Pair2<String, Object> bx = new Pair2<String, Object>("b",
					new Object());

			List<Pair2<String, Object>> list = Arrays.asList(bx, ba, ax, aa);
			Collections.sort(list);

			assert list.indexOf(aa) < list.indexOf(ba);
			assert list.indexOf(ax) < list.indexOf(bx);
		}
	}

	public void testToString1() {
		Pair2<String, Character> pair = new Pair2<String, Character>("first",
				's');
		assert pair.toString().equals("(first, s)");
	}

	public void testToString2() {
		Pair2<String, String> pair = new Pair2<String, String>("first", null);
		assert pair.toString().equals("(first, null)");
	}

}
