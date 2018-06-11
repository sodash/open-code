package com.winterwell.web.fields;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.web.app.TestWebRequest;
import com.winterwell.web.test.TestHttpServletRequest;

public class AFieldTest {

	@Test
	public void testGetStringValue() {
		// bit tricky to test, as it involves the vagaries of Java code & web browsers
		AField af = new AField("a");
		{
			TestWebRequest req = new TestWebRequest();		
			String v = af.getStringValue(req.getRequest());
			assert v == null;
		}		
	}

}
