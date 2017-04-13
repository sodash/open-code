package com.winterwell.nlp.io;

import com.winterwell.nlp.PorterStemmer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Stem words as they go through. Uses the {@link PorterStemmer} to perform
 * stemming, assuming each incoming token is a single word.
 * 
 * 
 * @testedby {@link StemmerFilterTest}
 * @author daniel
 */
public class StemmerFilter extends ATokenStream {

	private final PorterStemmer stemmer;

	public StemmerFilter(ITokenStream base) {
		this(base, new PorterStemmer());
//		TODO desc.put("lang", stemmer.getLanguage());
	}

	public StemmerFilter(ITokenStream base, PorterStemmer stemmer) {
		super(base);
		this.stemmer = stemmer;
//		TODO desc.put("lang", stemmer.getLanguage());
	}
	

	/**
	 * Protect certain tokens, e.g. links and hashtags. By default, it will not
	 * stem any word containing ascii punctuation marks. This can be overridden.
	 * 
	 * @param token
	 * @return false for normal words (which get stemmed), true to avoid
	 *         stemming.
	 */
	protected boolean doNotStem(Tkn token) {
		String word = token.getText();
		boolean punctuated = StrUtils.ASCII_PUNCTUATION.matcher(word).find();
		// this should not stem urls, right?
		if (punctuated)
			return true;
		return false;
	}

	@Override
	public ITokenStream factory(String input) {
		StemmerFilter st = new StemmerFilter(base.factory(input), stemmer);
		return st;
	}

	public PorterStemmer getStemmer() {
		return stemmer;
	}

	/**
	 * See {@link ATokenStream#processFromBase(Tkn)}
	 */
	@Override
	protected Tkn processFromBase(Tkn token, AbstractIterator<Tkn> bit) {
		if (doNotStem(token))
			return token;
		String word = token.getText();
		String stemmed = stemmer.stem(word);
		token.setText(stemmed);
		return token;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " <- " + base;
	}

}
