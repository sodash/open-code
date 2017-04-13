package com.winterwell.nlp;

import junit.framework.TestCase;
import com.winterwell.nlp.Tense.KTense;

public class TenseTest extends TestCase {

	public void testGuessTense() {
		Tense tense = new Tense();
		String[] egPast = new String[] { "I went shopping last week",
				"The Romans ruled Europe for ages.",
				"The quick brown fox jumped over the lazy dog." };
		for (String string : egPast) {
			KTense t = tense.guessTense(string);
			assert t == KTense.PAST : string + t;
		}

		String[] egFuture = new String[] { "I'm going to go home.",
				"One day our code will work." };
		for (String string : egFuture) {
			KTense t = tense.guessTense(string);
			assert t == KTense.FUTURE : string + t;
		}
	}

}
