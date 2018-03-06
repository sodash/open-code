package com.winterwell.utils.web;

import static org.junit.Assert.*;

import org.junit.Test;

public class WebUtils2Test {


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
