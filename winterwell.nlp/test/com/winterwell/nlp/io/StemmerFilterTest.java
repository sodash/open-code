package com.winterwell.nlp.io;

import java.util.List;

import org.junit.Test;

public class StemmerFilterTest {

	@Test
	public void testDictionaryStems() {
		StemmerFilter f = new StemmerFilter(new ListTokenStream(
				"#foo today dog dogs DOGS WIP.wop"));
		f.getStemmer().setDictionaryStems(true);
		ATSTestUtils.assertTokenisation(f, "#foo today dog dog dog WIP.wop");
	}

	@Test
	public void testDoNotStem() {
		{
			StemmerFilter f = new StemmerFilter(new ListTokenStream(
					"#fo #foo dogs DOGS WIP.wop"));
			ATSTestUtils.assertTokenisation(f, "#fo #foo dog dog WIP.wop");
		}
		{
			StemmerFilter f = new StemmerFilter(new ListTokenStream(
					"Visit http://www.stuff.com/things/index.html today"));
			ATSTestUtils.assertTokenisation(f,
					"visit http://www.stuff.com/things/index.html todai");
		}

	}

	@Test
	public void testFactory() {
		StemmerFilter f = new StemmerFilter(new WordAndPunctuationTokeniser());
		f.getStemmer().setDictionaryStems(true);
		ITokenStream f2 = f.factory("#foo today dog dogs DOGS WIP.wop");
		List<Tkn> toks = ATSTestUtils.getTokens(f2);
		ITokenStream f3 = f.factory("today dogs");
		ATSTestUtils.assertTokenisation(f3, "today dog");

	}

}
