package com.winterwell.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.Printer;

import junit.framework.TestCase;

public class PrinterTest extends TestCase {

	public void testArray() {
		{
			String s = Printer.toString(new Object[] { 1, 2 });
			assertEquals("1, 2", s);
			List<Object[]> list = Arrays.asList(new Object[] { 1, 2 },
					new Object[] { 3 });
			String s2 = Printer.toString(list);
			assertEquals("1, 2, 3", s2);
		}
	}

	public void testFormatNumber() {
		{
			String seven = Printer.toStringNumber(7);
			String third = Printer.toStringNumber(1.0 / 3);
			String sevenAndAThird = Printer.toStringNumber(7 + 1.0 / 3);
			Printer.out(seven + " " + third + " " + sevenAndAThird);
		}
		{
			Printer.out(100012.704);
			Printer.out(0.78765);
			Printer.out(0.000001234);
		}
	}

	public void testListWithNulls() {
		ArrayList list1 = new ArrayList();
		list1.add("a");
		list1.add(null);
		list1.add("c");
		list1.add(null);
		assert Printer.toString(list1, " + ").equals("a + c");
	}

	public void testOut() {
		Printer.out("Hello");
		Printer.out("Hello", "There");
		Printer.out("Hello", new Object[] { 1, 2, 3 });
	}

	public void testPrettyNumber() {
		{
			String s = Printer.prettyNumber(100012.704);
			assertEquals("100,000", s);
			String s2 = Printer.prettyNumber(140000);
			assertEquals("140,000", s2);
			String s3 = Printer.prettyNumber(100000000);
			assertEquals("100 million", s3);
		}
		{
			String s = Printer.prettyNumber(0.234, 2);
			assertEquals("0.23", s);
			String s2 = Printer.prettyNumber(2.10008, 2);
			assertEquals("2.1", s2);
			String s3 = Printer.prettyNumber(2.18008, 2);
			assertEquals("2.2", s3);
		}
	}

	public void testToStringCollection1() {
		ArrayList list1 = new ArrayList();
		list1.add('a');
		list1.add('b');
		assert Printer.toString(list1, " + ").equals("a + b");
	}

	public void testToStringCollection2() {
		ArrayList list1 = new ArrayList();
		list1.add(1);
		list1.add(2);
		assert Printer.toString(list1, " + ").equals("1 + 2");
	}

	public void testToStringCollection3() {
		ArrayList list1 = new ArrayList();
		list1.add(1.3);
		list1.add('c');
		assert Printer.toString(list1, " + ").equals("1.3 + c");
	}

	public void testToStringMap() {
		Map map1 = new HashMap();
		map1.put(0, 3.5);
		map1.put(1, 4.9);
		String ms = Printer.toString(map1, "\n", ":");
		assertEquals("{0:3.5\n1:4.9}", ms);
	}

	public void testThrowable() {
		String message = "Test exception";
		for (int i = 0; i < 50; i++)
			message += " with a really long message";
		String output = "";
		try {
			throw new Exception(message);
		} catch (Exception ex) {
			output = Printer.toString(ex, true);
		}
		System.out.println(output);
		assert output.contains(message);
		assert output.contains("PrinterTest");
	}

}
