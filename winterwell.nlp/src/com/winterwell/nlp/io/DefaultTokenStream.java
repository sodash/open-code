package com.winterwell.nlp.io;

import com.winterwell.nlp.PorterStemmer;

/**
 * A default pipeline for simplifying text. Break into words, lowercase, discard
 * punctuation, stem.
 * 
 * Note: This process is pretty radical e.g. "This is tested" becomes
 * "thi is test" Corollary: This class is completely inappropriate for deep
 * parsing
 * 
 * @author daniel
 * @deprecated Stemming is too weird to be used widely
 */
@Deprecated
public final class DefaultTokenStream extends ATokenStream {

	private static final PorterStemmer stemmer = new PorterStemmer();

	ITokenStream tokenizer;

	/**
	 * A default pipeline for simplifying text. Break into words, lowercase,
	 * discard punctuation, stem.
	 */
	public DefaultTokenStream(String input) {
		ITokenStream factory = new WordAndPunctuationTokeniser();
		tokenizer = factory.factory(input);
	}
	


	/**
	 * Returns a new DefaultTokenStream, with the specified string as input.
	 */
	@Override
	public ITokenStream factory(String in) {
		return new DefaultTokenStream(in);
	}
	
	@Override
	public boolean isFactory() {
		return true;
	}
	
}
