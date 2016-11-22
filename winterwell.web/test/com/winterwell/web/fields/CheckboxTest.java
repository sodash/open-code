package com.winterwell.web.fields;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.web.test.TestHttpServletRequest;

public class CheckboxTest {
	
	@Test public void testHtml() {
		Checkbox cb = new Checkbox("dummy");
		cb.setLabel("foo");
		String html = cb.getHtml();
		System.out.println(html);
	}
	
	@Test public void testConvertStringString() {
		Checkbox cb = new Checkbox("dummy");
		assert cb.fromString("true");
		assert !cb.fromString("false");
		try {
			Boolean v = cb.fromString("frogs legs");
			assert false : v;
		} catch (Exception e) {
			// OK
		}
	}

	@Test public void testConvertToStringBoolean() {
		Checkbox cb = new Checkbox("dummy");
		assert cb.toString(true).equals("true");
		assert cb.fromString(cb.toString(false)) == false;
	}

	@Test public void testGetStringValue() {
		{
			Checkbox cb = new Checkbox("dummy");
			String v = cb.getStringValue(new TestHttpServletRequest());
			assert v.equals("false") : v;
		}
		{
			Checkbox cb = new Checkbox("dummy");
			TestHttpServletRequest req = new TestHttpServletRequest(
					new ArrayMap("dummy", "true"));
			String v = cb.getStringValue(req);
			assert v.equals("true") : v;
			assert cb.getValue(req);
		}
	}

}
