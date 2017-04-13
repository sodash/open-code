package com.winterwell.nlp.spam;

import java.io.File;

import junit.framework.TestCase;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.KErrorPolicy;

import com.winterwell.depot.Depot;
import com.winterwell.utils.Printer;

public class RudeWordsFilterTest extends TestCase {

	public void testScheissKopf() {
		RudeWordsFilter rwf = new RudeWordsFilter();
		String rudeDe = "Scheisse!";
		assert rwf.containsRudeWord(rudeDe) != null;
		assert rwf.containsRudeWord("winterstein") == null;
		assert rwf.containsRudeWord("Quacksalber") == null : rwf.containsRudeWord("Quacksalber");
		assert rwf.containsRudeWord("A") == null;
	}
	
	public void testContainsRudeWord() {
		RudeWordsFilter rwf = new RudeWordsFilter();
		String clean = "Building jobs vital, says Scott: 1438: David Cameron has told us momentum is building against what he calls \"Gordo...";
		assert rwf.containsRudeWord("he") == null;
		assert rwf.containsRudeWord(clean) == null : rwf
				.containsRudeWord(clean);
		assert rwf.containsRudeWord("Fuck") != null;
		assert rwf
				.containsRudeWord("@BeckyBadwan ARGH INNIT. ITS LIKE, FUCK OFF DAVID CAMERON - YOUR NOT EVEN GOODLOOKING YOU IDIOT") != null;
		assert rwf
				.containsRudeWord("I'm trying to eat a big fuck-off slice of watermelon elegantly.") != null;
		
		System.out.println(rwf.containsRudeWord("b*llocks"));		
	}

	public void testExportToJS() {
		RudeWordsFilter rwf = new RudeWordsFilter();
		String guts = Printer.toString(rwf.rude.keySet(), "|");
		guts = guts.replace("*", "\\*");
		String js = "var SWEARY = /\\\\b("
				+ guts
				+ ")(ing|s|es|y)?\\\\b/i;\n"
				+ "function isSweary(txt) {if (!txt) return; return txt.match(SWEARY);}\n";

		FileUtils.write(new File("Swearing.js"), js);
	}

	public void testIsNotRude() {
		RudeWordsFilter rwf = new RudeWordsFilter();
		assert !rwf.isRude("mother");
		assert !rwf.isRude("annual");
		assert !rwf.isRude("fack");
		assert !rwf.isRude("fruction");
		assert !rwf.isRude("Microsoft");

	}
	
	public void testWithSoDashTokeniser() {
		RudeWordsFilter rwf = new RudeWordsFilter();
		
		// get words and urls, lose punctuation
		// - except for # and @ which are special and can start words
		WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser.TweetSpeak();
		t.setNormaliseToAscii(KErrorPolicy.ACCEPT);
		t.setSwallowPunctuation(true); 
		t.setSplitOnApostrophe(false);
		t.setLowerCase(true);
		
		WordAndPunctuationTokeniser tokens = t.factory("shit b*llocks F.U.C.K.");
		for (Tkn tkn : tokens) {
			System.out.println(tkn+"\t"+rwf.isRude(tkn.getText()));
		}
	}

	public void testIsRude() {
		RudeWordsFilter rwf = new RudeWordsFilter();
		assert rwf.isRude("fuck");
		assert rwf.isRude("Fuck");
		assert !rwf.isRude("he");
		assert !rwf.isRude("hello");

		assert rwf.isRude("fucking");
		assert rwf.isRude("fuckin");
		
		assert rwf.isRude("cunt");
		assert rwf.isRude("cunts");
		assert rwf.isRude("C*NTS");
		assert rwf.isRude("piss");
		assert rwf.isRude("pissing");
	}

	public void testSample() {
		Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK);
		
		RudeWordsFilter rwf = new RudeWordsFilter();
		for(int i=0; i<3; i++) {
			String a = rwf.sample();
			String b = rwf.sample();
			String c = rwf.sample();
			System.out.println(a + " you " + b + " "+c + "!");
			System.out.println("\t= "+rwf.getMeaning(a)+ " you " + rwf.getMeaning(b)+ " "
					+rwf.getMeaning(c) + ".");
		}
	}

	public void testWithAsterixs() {
		RudeWordsFilter rwf = new RudeWordsFilter();
		// bit of a tricky one
		assert rwf.isRude("f*ck");
	}
}
