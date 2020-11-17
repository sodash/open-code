package com.winterwell.nlp.similarity;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;

/**
 * Status: experimental - not clear if this is a good/useful algorithm!
 * 
 * Applies {@link LevenshteinEditDistance} at both the word and character level.
 * 
 * @author daniel
 * @testedby  MetaLevenshteinEditDistanceTest}
 */
public class MetaLevenshteinEditDistance extends LevenshteinEditDistance {

	LevenshteinEditDistance led = new LevenshteinEditDistance();
	ITokenStream tokeniser;

	public MetaLevenshteinEditDistance() {
		tokeniser = initTokeniser();
	}

	@Override
	protected int cost(Object ai, Object bj) {
		return (int) led.editDistance(led.list((String) ai), led.list((String) bj));
	}

	private ITokenStream initTokeniser() {
		WordAndPunctuationTokeniser wt = new WordAndPunctuationTokeniser();
		wt.setLowerCase(true);
		return wt;
	}

	@Override
	protected List list(String a) {
		ITokenStream tokens = tokeniser.factory(a);
		ArrayList list = new ArrayList();
		for (Tkn token : tokens) {
			list.add(token.getText());
		}
		return list;
	}

}
