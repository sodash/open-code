package com.winterwell.web;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.web.WebUtils;

import junit.framework.TestCase;

public class HtmlTableTest extends TestCase {

	public void testAddCell() {
		HtmlTable table = new HtmlTable(3);
		table.setStyle("border: solid blue 1px;");
		table.addCell("A");
		table.addCell("B");
		table.addCell("C");
		table.addCell("1");
		table.addCell("2");
		table.addCell("3");
		table.addCell("4");
		String html = table.toHTML();
		WebUtils.display(html);
	}

	public void testAddRow1() {
		HtmlTable table = new HtmlTable(2);
		table.addRow("first row 1", "first row 2");
		assertEquals("Width of table", 2, table.getWidth());
		System.out.println(table.toHTML());
		assertEquals("Table with two rows and two columns",
				"<table ><tbody><tr class='even row0'><td class='even col0'>first row 1</td>"
						+ "<td class='odd col1'>first row 2</td></tr>\n</tbody></table>",
				table.toHTML());
	}

	public void testSetCSSClass1() {
		HtmlTable table = new HtmlTable(2);
		table.addRow("first row 1", "first row 2");
		table.setCSSForCell(0, 0, "color:red;");
		assertEquals("Table with classes",
				"<table ><tbody><tr class='even row0'><td class='even col0' style='color:red;'>first row 1</td>"
						+ "<td class='odd col1'>first row 2</td></tr>\n</tbody></table>",
				table.toHTML());
	}

	public void testSetCSSClass2() {
		HtmlTable table = new HtmlTable(2);
		table.addRow("first row 1", "first row 2");
		// affects nothing
		table.setCSSForCell(3, 3, "color:green; font-size:50%;");
		assertEquals("Table with classes",
				"<table ><tbody><tr class='even row0'><td class='even col0'>first row 1</td>"
						+ "<td class='odd col1'>first row 2</td></tr>\n</tbody></table>",
				table.toHTML());
	}

	public void testToHTML1() {
		List<String> headers = new ArrayList<String>();
		headers.add("first column");
		headers.add("second");
		HtmlTable table = new HtmlTable(headers);
		assertEquals("Table with header",
				"<table ><thead><tr class='header'><th class='even  col0' col='0'>first column</th>"
						+ "<th class='odd  col1' col='1'>second</th></tr></thead>\n<tbody></tbody></table>",
				table.toHTML());
	}

	public void testToHTML2() {
		List<String> headers = new ArrayList<String>();
		headers.add("first column");
		headers.add("second");
		HtmlTable table = new HtmlTable(headers);
		table.setStyle("style1");
		assertEquals("Table with style",
				"<table style='style1' ><thead><tr class='header'><th class='even  col0' col='0'>first column</th>"
						+ "<th class='odd  col1' col='1'>second</th></tr></thead>\n<tbody></tbody></table>",
				table.toHTML());
	}
}
