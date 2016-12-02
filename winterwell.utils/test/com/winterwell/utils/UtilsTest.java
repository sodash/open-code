package com.winterwell.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.containers.ArrayMap;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {

	public void testTruthy() throws Exception {
		assert Utils.truthy(1);
		assert ! Utils.truthy(false);
		assert ! Utils.truthy(new ArrayList());
		assert ! Utils.truthy("");
		assert Utils.truthy("foo");
	}
	
	public void testAssert() throws Exception {
		try {
			assert false;
			fail("assert is not enabled! Set -ea on the JVM command line arguments!");
		} catch (AssertionError e) {
			// all good
		}
	}

	public void testGetRandomSelection() {
		{ // none
			List<Integer> choices = Arrays.asList(1, 2, 3);
			Collection<Integer> cs = Utils.getRandomSelection(0, choices);
			assert cs.isEmpty();
		}
	}

	// @Test(expected = IllegalArgumentException.class)
	public void testGetRandomSelectionError() {
		try { // less than none?
			List<Integer> choices = Arrays.asList(1, 2, 3);
			Collection<Integer> cs = Utils.getRandomSelection(-1, choices);
			assert cs.isEmpty();
		} catch (IllegalArgumentException ex) {
			return; // Success!
		}
		fail();
	}

	public void testCopy() {
		ArrayList x = new ArrayList();
		x.add("hello");
		x.add(new double[] { 0, 1 });
		ArrayMap map = new ArrayMap("a", "b");
		x.add(map);
		ArrayList copy = Utils.copy(x);
		assert copy.get(0).equals("hello");
		assert ((double[]) copy.get(1))[1] == 1;
		assert copy.get(2) != map;
		assert copy.get(2).equals(map);
	}

	public void testEquals() {
		String[] a = new String[]{"w-1","w"};
		String[] b = new String[]{new String("w-1"),"w"};
		String[] b2 = new String[]{"w-1","w"};
		assert ! a.equals(b);
		assert Utils.equals(a, b);
		assert Utils.equals(a, b2);
		assert Utils.equals(b, b2);
		assert ! a.equals(b2);
		
		Long n1 = Long.MAX_VALUE;
		Long n2 = n1 - 1;
		assert n1 != n2;
		assert !Utils.equals(n1, n2);
	}


	public void testGetPassword() {

	}

	// public void testGetSVNRevision(){
	// int r = Utils.getSVNRevision(FileUtils.getWorkingDirectory());
	// assert r > 100;
	// }

	public void testGetRandomString() {
		String a = Utils.getRandomString(6);
		String b = Utils.getRandomString(6);
		String c = Utils.getRandomString(6);
		assert !a.equals(b);
		assert !a.equals(c);
		assert a.length() == 6;
		Printer.out(Printer.format("{0} {1} {2}", a, b, c));
	}

	public void testIsBlank() {
		assert Utils.isBlank("");
		assert Utils.isBlank(null);
		assert Utils.isBlank("   	\n ");
		assert !Utils.isBlank("   	\n x		");
		assert !Utils.isBlank("x");
		assert !Utils.isBlank("monkeys monkeys");
	}

	public void testIsEmptyCollection() {
		ArrayList list1 = new ArrayList();
		assert Utils.isEmpty(list1);
		list1.add(null);
		assert Utils.isEmpty(list1) == false;
		list1.clear();
		list1.add('a');
		assert Utils.isEmpty(list1) == false;
	}

	public void testIsEmptyMap() {
		Map map1 = new HashMap();
		assert Utils.isEmpty(map1);
		map1.put(null, null);
		assert Utils.isEmpty(map1) == false;
		map1.clear();
		map1.put('a', 123);
		assert Utils.isEmpty(map1) == false;
	}

	public void testIsInt() {
		assert Utils.isInt("1");
		assert Utils.isInt("-1");
		assert Utils.isInt("") == false;
		assert Utils.isInt("2.3") == false;
		assert Utils.isInt("a") == false;
		assert Utils.isInt(null) == false;
	}

	public void testIsNumber() {
		assert StrUtils.isNumber("1");
		assert StrUtils.isNumber("-1");
		assert StrUtils.isNumber("-12.23");
		assert StrUtils.isNumber("a") == false;
		assert StrUtils.isNumber("") == false;
	}

	public void testOr() {
		assert Utils.or() == null;
		assert Utils.or(null) == null;// problems for this case
		assert Utils.or(1) == 1;
		assert Utils.or(null, 1) == 1;
		assert Utils.or(1, 2) == 1;
		assert Utils.or(1, null) == 1;
		assert Utils.or("", "ace") == "ace";
	}
	
	static class Dummy {
		public static String two = "World";
		public String one = "Hello";
		public String three;
	}
}
