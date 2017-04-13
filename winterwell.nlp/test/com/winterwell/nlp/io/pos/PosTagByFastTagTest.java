package com.winterwell.nlp.io.pos;

import java.util.List;

import org.junit.Test;

import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;

public class PosTagByFastTagTest {

	@Test
	public void testSimple() {
		WordAndPunctuationTokeniser base = new WordAndPunctuationTokeniser(
				"The cat sat on the mat.");
		PosTagByFastTag tagd = new PosTagByFastTag(base);
		List<Tkn> tagged = tagd.toList();
		System.out.println(tagged);
		String tags = "";
		for (Tkn tkn : tagged) {
			tags += tkn.getPOS()+" ";
		}
		System.out.println(tags);
		assert tags.equals("DT NN VBD IN DT NN . ") : tags;
	}

	
	@Test
	public void test2() {
		WordAndPunctuationTokeniser base = new WordAndPunctuationTokeniser(
				"The boy saw the man on the hill with a telescope.");
		PosTagByFastTag tagd = new PosTagByFastTag(base);
		List<Tkn> tagged = tagd.toList();
		System.out.println(tagged);
		String tags = "";
		for (Tkn tkn : tagged) {
			tags += tkn.getPOS()+" ";
		}
		System.out.println(tags);
	}
	

	@Test
	public void testNoGood() {
		WordAndPunctuationTokeniser base = new WordAndPunctuationTokeniser(
				"No good will come of it.");
		PosTagByFastTag tagd = new PosTagByFastTag(base);
		List<Tkn> tagged = tagd.toList();
		System.out.println(tagged);
		String tags = "";
		for (Tkn tkn : tagged) {
			tags += tkn.getPOS()+" ";
		}
		System.out.println(tags);
	}

}
