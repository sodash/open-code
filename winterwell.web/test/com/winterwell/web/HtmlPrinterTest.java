package com.winterwell.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.web.HtmlPrinter;

import junit.framework.TestCase;

public class HtmlPrinterTest extends TestCase {

	public void testToStringKeepingHtml() {
		String str = "a<b>b</b> <BR>";
		assertEquals(str, HtmlPrinter.toString(str));
	}

	public void testToStringObject1() {
		String str = "<html><body/></html>";
		assert HtmlPrinter.toString(str).equals(
				"&lt;html&gt;&lt;body/&gt;&lt;/html&gt;");
	}

	public void testToStringObject2() {
		String str = "<html><body>Hello world\n</body></html>";
		assert HtmlPrinter
				.toString(str)
				.equals("&lt;html&gt;&lt;body&gt;Hello world\n&lt;/body&gt;&lt;/html&gt;");
	}

	// instance of List
	public void testToStringObject4() {
		List<String> list = new ArrayList<String>();
		list.add("first element");
		list.add("second");
		assert HtmlPrinter.toString(list).equals(
				"<ol><li><p>first element</p></li><li><p>second</p></li></ol>");
	}

	public void testToStringObject5() {
		List<String> list = new ArrayList<String>();
		assert HtmlPrinter.toString(list).equals("<ol></ol>");
	}

	// instance of Map
	public void testToStringObject6() {
		Map<String, Double> map = new HashMap<String, Double>();
		map.put("first", 1.4);
		map.put("second", 0.0);
		assertEquals("<table><tr><td>second</td><td>0</td></tr>"
				+ "<tr><td>first</td><td>1.4</td></tr></table>",
				HtmlPrinter.toString(map));
	}

	public void testToStringObject7() {
		Map<String, Double> map = new HashMap<String, Double>();
		assert HtmlPrinter.toString(map).equals("<table></table>");
	}

	// instance of Set
	public void testToStringObject8() {
		Set<String> set = new HashSet<String>();
		set.add("first element");
		set.add("second");
		assert HtmlPrinter.toString(set).equals(
				"<ul><li><p>first element</p></li><li><p>second</p></li></ul>");
	}

	public void testToStringObject9() {
		Set<String> set = new HashSet<String>();
		assert HtmlPrinter.toString(set).equals("<ul></ul>");
	}
}
