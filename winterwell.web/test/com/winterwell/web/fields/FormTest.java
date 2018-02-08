package com.winterwell.web.fields;

import junit.framework.TestCase;

public class FormTest extends TestCase {

	public void testAddRow() {
		Form form = new Form(null);
		form.startTable();
		AField<Double> field = new AField<Double>("first value");
		form.addRow("first question", field);
		assertEquals(
				"Table with one row",
				"\n<table class='form-table' >\n<tr><td>first question</td><td><input type='text'"
						+ " name='first value' value=\"\" size='20' class='' />\n</td></tr>\n",
				form.sb().toString());
	}

	public void testBasic() {
		StringBuilder page = new StringBuilder("<html><body>");
		Form form = new Form(null);
		form.appendHtmlTo(page);
		page.append("</body></html>");
		assertTrue(page.toString(), page.toString().contains(
				"<form "));
		assertTrue(page.toString(), page.toString().contains(
				"</form>"));
	}

// This methor is not implemented as the test suggests
//	public void testSetAction() {
//		Form form = new Form(null);
//		form.setAction("servletLogin");
//		assertEquals("\n<input type='hidden' name='action' value='servletLogin'>\n", form.sb().toString());
//	}

	public void testSetSubmit1() {
		StringBuilder page = new StringBuilder("<html><body>");
		Form form = new Form(null);
		form.setSubmit(null);
		form.appendHtmlTo(page);
		page.append("</body></html>");
		assertTrue(page.toString(), page.toString().contains("<html><body>\n<form action='null' method='post'></form>\n"));
	}

	public void testSetSubmit2() {
		StringBuilder page = new StringBuilder("<html><body>");
		Form form = new Form(null);
		form.setSubmit(" <input type='input' onclick='function()'> ");
		form.appendHtmlTo(page);
		page.append("</body></html>");
		assertTrue(page.toString().contains(
				"<html><body>\n<form action='null' method='post'> <input type='input' onclick='function()'> </form>\n"));
	}

	public void testStartTable() {
		Form form = new Form(null);
		form.startTable();
		assertEquals("Just-started table", "\n<table class='form-table' >\n", form.sb().toString());
	}

	public void testStartTableString() {
		Form form = new Form(null);
		form.startTable("table_name");
		assertEquals("Named just-started table", "\n<table class='form-table' table_name>\n", form
				.sb().toString());
	}

	public void testToString1() {
		Form form = new Form(null);
		assert form.toString().equals("Form:");
	}

	public void testToString2() {
		Form form = new Form(null);
		form.startTable("table_name");
		assertEquals("Form with named table", "Form:\n<table class='form-table' table_name>\n",
				form.toString());
	}
}
