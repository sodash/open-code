package com.winterwell.web.fields;

import java.util.Arrays;

import junit.framework.TestCase;

import com.winterwell.utils.Printer;

public class SelectFieldTest extends TestCase {

	public void testConvertString() {
		String[] options = { "a'b", "c\"d", "e&f", "g/h", "i\\j" };
		SelectField<String> select = new SelectField<String>("Talking Heads", Arrays.asList(options));
		select.setUseValuesDirectly(false);
		String v = select.fromString("1");
		assert v.equals("c\"d") : v;
		v = select.fromString("2");
		assert v.equals("e&f") : v;
	}

	public void testHtmlEntityEscaping() {
		String[] options = { "a'b", "c\"d", "e&f", "g/h", "i\\j" };
		SelectField<String> select = new SelectField<String>("Talking Heads",
				Arrays.asList(options));
		String html = select.getHtml();
		Printer.out(html);
	}

	public void testIndexOf() {
		String[] options = { "psycho", "killer", "qu'est-ce", "que", "c'est" };
		SelectField<String> select = new SelectField<String>("Talking Heads",
				Arrays.asList(options));
		for (int index = 0; index < options.length; index++) {
			assert select.indexOf(new String(options[index])) == index;
		}
		assert select.indexOf("not-present") == -1;
	}

	public void testOrderPreservation() {
		String[] options = { "psycho", "killer", "qu'est-ce", "que", "c'est" };
		options = new String[] { "1 Not at all knowledgeable", "2", "3", "4",
				"5 Deeply knowledgeable" };
		SelectField<String> select = new SelectField<String>("Talking Heads",
				Arrays.asList(options));
		for (int index = 0; index < options.length; index++) {
			assert select.indexOf(options[index]) == index;
		}
		assert select.indexOf("not-present") == -1;
	}

}
