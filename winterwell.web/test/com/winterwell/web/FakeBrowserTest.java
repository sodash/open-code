package com.winterwell.web;

import static org.junit.Assert.*;

import org.junit.Test;

public class FakeBrowserTest {

	@Test
	public void testSetRetryOnError() {
		{	// simple unset
			FakeBrowser fb = new FakeBrowser();
			String beeb = fb.getPage("https://bbc.co.uk");
			assert ! beeb.isEmpty();
		}
		{	// simple set
			FakeBrowser fb = new FakeBrowser();
			fb.setRetryOnError(3);
			String beeb = fb.getPage("https://bbc.co.uk");
			assert ! beeb.isEmpty();
		}
		try {	// fail unset
			FakeBrowser fb = new FakeBrowser();
			String beeb = fb.getPage("https://bbcdadsasda.dadsada");
			assert false;
		} catch(Exception ex) {
			assert ex.toString().contains("UnknownHost") : ex.toString();
		}
		try {	// fail set
			FakeBrowser fb = new FakeBrowser();
			fb.setRetryOnError(3);
			String beeb = fb.getPage("https://dasdasda.ewadsas");
			assert false;
		} catch(Exception ex) {
			assert ex.toString().contains("UnknownHost");
		}
	}

}
