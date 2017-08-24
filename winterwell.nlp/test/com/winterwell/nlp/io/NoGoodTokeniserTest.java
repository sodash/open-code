package com.winterwell.nlp.io;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;

public class NoGoodTokeniserTest {

	@Test
	public void testNoGood() {
		WordAndPunctuationTokeniser base = new WordAndPunctuationTokeniser("");
		base.setLowerCase(true);
		NoGoodTokeniser ngt = new NoGoodTokeniser(base);
		
		Printer.out(Containers.getList(ngt.factory("No good will come")));
		
		ATSTestUtils.assertTokenisation(ngt.factory("No good will come"), "no-good will come");
		ATSTestUtils.assertTokenisation(ngt.factory("Cameron is not a good PM"), "cameron is no-good pm");
	}

}
