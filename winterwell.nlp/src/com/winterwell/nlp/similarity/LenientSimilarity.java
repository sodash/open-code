package com.winterwell.nlp.similarity;

import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.StopWordFilter;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.log.KErrorPolicy;

/**
 * Levenshtein edit distance, but with some canonicalisation and ignoring stop words.
 * This is syntax similarity - NOT semantic similarity.
 * 
 * @author daniel
 * @testedby {@link LenientSimilarityTest}
 */
public class LenientSimilarity implements ICompareWords {

	LevenshteinEditDistance led = new LevenshteinEditDistance();

	ITokenStream tokeniser;

	public LenientSimilarity() {
		WordAndPunctuationTokeniser wt = new WordAndPunctuationTokeniser();
		wt.setLowerCase(true);
		wt.setSwallowPunctuation(true);
		wt.setLowerCase(true);
		wt.setNormaliseToAscii(KErrorPolicy.ACCEPT);
		tokeniser = new StopWordFilter(wt);
	}

	private String prep(String a) {
		ITokenStream tokens = tokeniser.factory(a);
		StringBuilder sb = new StringBuilder(a.length());
		for (Tkn token : tokens) {
			sb.append(token.getText());
			sb.append(' ');
		}
		return a;
	}

	@Override
	public double similarity(String a, String b) {
		a = prep(a);
		b = prep(b);
		return led.similarity(a, b);
	}

}
