package com.winterwell.nlp.io;

import java.util.List;

import org.junit.Test;

public class SentenceStreamTest {
	@Test
	public void testSimple() {
		{
			SentenceStream ss = new SentenceStream("hello-world");
			ATSTestUtils.assertTokenisation(ss, "hello-world");
		}
		{
			SentenceStream ss = new SentenceStream("One. And two! And a three.");
			List<Tkn> tks = ATSTestUtils.getTokens(ss);
			assert tks.get(0).getText().equals("One.") : tks;
			assert tks.get(1).getText().equals("And two!") : tks;
			assert tks.get(2).getText().equals("And a three.") : tks;
		}
	}
}
