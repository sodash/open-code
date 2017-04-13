package com.winterwell.nlp;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.winterwell.utils.containers.Cache;

public class PorterStemmerTest {

	@Test
	public void testFancyStemDictionary() {
		PorterStemmer ps = new PorterStemmer();
		ps.setStemDictionary(new Cache<String, String>(1), true);
		Assert.assertEquals("monkeys", ps.stem("monkeys")); // Perils of not
															// initialising a
															// dictionary
		Assert.assertEquals("monkey", ps.stem("monkey"));
		Assert.assertEquals("monkey", ps.stem("monkeys"));
		Assert.assertEquals("pineapple", ps.stem("pineapple"));
		Assert.assertEquals("pineapple", ps.stem("pineapples"));
		Assert.assertEquals("monkeys", ps.stem("monkeys"));
	}

	@Test
	public void testSetDictionaryStems() {
		PorterStemmer ps = new PorterStemmer();
		String b0 = ps.stem("monkeys");
		Assert.assertEquals("monkei", b0);

		ps.setDictionaryStems(true);

		// prep -- this happens to include both "monkey" and "folklore"
		NLPWorkshop workshop = NLPWorkshop.get();
		Set<String> dict = workshop.getDictionary();
		for (String string : dict) {
			ps.stem(string);
		}

		String s2 = ps.stem("flying");
		String b2 = ps.stem("monkeys");
		String c2 = ps.stem("folklore");
		Assert.assertEquals("fly", s2);
		Assert.assertEquals("monkey", b2);
		Assert.assertEquals("folklore", c2);
	}

	@Test
	public void testStem() {
		PorterStemmer ps = new PorterStemmer();
		String s0 = ps.stem("flying");
		String s1 = ps.stem("fly");

		// algorithm fails on these
		String s2 = ps.stem("flies");
		String s3 = ps.stem("flew");

		assertEquals(s0, s1);
	}

	@Test
	public void testStemSpecialWords() {
		PorterStemmer ps = new PorterStemmer();
		String so = ps.stem("dgs");
		String s1 = ps.stem("DGS");
		// hashtags, email addresses and links all get mangled
		String s2 = ps.stem("#DGS");
		String s3 = ps.stem("dan@dgs");
		String s4 = ps.stem("http://example.com/foo?x=DGS");
		// assertEquals("http://example.com/foo?x=DGS", s4);
	}
	
	@Test
	public void scratch() {
		PorterStemmer ps = new PorterStemmer();
		String so = ps.stem("enquiry");
	}
}
