package com.winterwell.utils.web;

import org.junit.Test;
import org.w3c.dom.Document;

import com.winterwell.utils.StrUtils;

public class WebUtils2Test {

	@Test
	public void testXmlDocToString() {
		String xml = "<foo blah='whatever'><bar src=''>huh?</bar></foo>";
		Document doc = WebUtils2.parseXml(xml);
		String xml2 = WebUtils2.xmlDocToString(doc);
		String c = StrUtils.compactWhitespace(xml2.trim()).replaceAll("[\r\n]", "").replace('"', '\'');
		assert c.equals(xml) : xml2+" -> "+c;
	}

	@Test
	public void testCanonicalEmailString() {
		{
			String e = WebUtils2.canonicalEmail("Bob <Bob@FOO.COM>");
			assert e.equals("bob@foo.com");
		}
		{
			String e = WebUtils2.canonicalEmail("Alice.1@FOO.bar.co.uk");
			assert e.equals("alice.1@foo.bar.co.uk");
		}
	}

}
