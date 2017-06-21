package com.winterwell.nlp.io;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.Printer;

import com.winterwell.nlp.corpus.brown.BrownCorpusTags;
import com.winterwell.nlp.dict.IDictionary;
import com.winterwell.utils.Key;
import com.winterwell.utils.Mutable;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.web.WebUtils;

/**
 * Cut a string into a stream of words and punctuation tokens. This class does
 * no other processing. E.g. no lower-casing or stemming. Can optionally swallow
 * punctuation tokens.
 * <p>
 * hyphens lead to multiple tokens, e.g. "monkey-business" is split into
 * "monkey, "-", "business"<br>
 * _ is allowed inside word/number tokens<br>
 * Continuous blocks of punctuation are treated as a single token, e.g. !! -- ."
 * <br>
 * Numbers are allowed to contain .s and ,s in the middle, e.g. 1,000.1.2 would
 * be one token<br>
 * TODO .s are allowed inside words if part of a single-letter / dot sequence
 * ending with a ., e.g. U.K. but not U.K or ab.cd<br>
 * TODO what to do with apostrophes? 'ʹʼʾ
 * TODO keep * in words, e.g. "b*llocks"
 * 
 * @author Daniel
 * @testedby {@link WordAndPunctuationTokeniserTest}
 */
public class WordAndPunctuationTokeniser extends ATokenStream {

//	TODO maybe support Brown style /NN POS tags?? Boolean brownPosTags;
//	
//	public void setSupportBrownPosTags(boolean brownPosTags) {
//		this.brownPosTags = brownPosTags;
//	}
	
	/**
	 * Version of {@link WordAndPunctuationTokeniser} which treats hashtags, @you
	 * mentions, and urls as single words.
	 * <p>
	 * Note: you may still wish to apply further customisation! e.g.
	 * lower-casing or handling of apostrophes and punctuation.
	 */
	public static class TweetSpeak extends WordAndPunctuationTokeniser {
		/**
		 * Version of {@link WordAndPunctuationTokeniser} which treats hashtags, @you
		 * mentions, and urls as single words.
		 * <p>
		 * Note: you may still wish to apply further customisation! e.g.
		 * lower-casing or handling of apostrophes and punctuation.
		 */
		public TweetSpeak() {
			setUrlsAsWords(true);
		}

		/**
		 * Only used by {@link #factory(String)}.
		 */
		private TweetSpeak(String input) {
			super(input);
			setUrlsAsWords(true);
		}

		@Override
		public WordAndPunctuationTokeniser factory(String newInput) {
			// over-ride to avoid the deep-copy of dictionaries
			TweetSpeak wpt = new TweetSpeak(newInput);
			return factory2(wpt);
		}

		@Override
		protected boolean isWordStart(char c) {
			return Character.isLetter(c) || c == '#' || c == '@';
		}
	}

	/**
	 * For use with {@link Tkn#POS} to indicate this token is a url. Not a
	 * traditional part-of-speech tag, but it does turn up in a lot of web text.
	 */
	public static final String POS_URL = BrownCorpusTags.toCanon("url");

	/**
	 * this is anchored for local search only
	 */
	private static Pattern URL_REGEX2 = Pattern.compile("^"
			+ WebUtils.URL_REGEX.pattern());

	IDictionary[] dicts;
	boolean[] dictsTranslate;

	private String input;
	boolean lowerCase;

	/**
	 * null for "do not normalise"
	 */
	KErrorPolicy normaliseToAscii;

	boolean splitOnApostrophe = true;
	/**
	 * If true, punctuation tokens will not be emitted.
	 */
	boolean swallowPunctuation;

	boolean urlsAsWords = true;

	/**
	 * Create a tokeniser for use as a factory.
	 */
	public WordAndPunctuationTokeniser() {
		this("");
	}

	public WordAndPunctuationTokeniser(String input) {
		super();
		setInput(input);
	}

	/**
	 * Dictionary entries will be protected from *any* editing. So no
	 * lower-casing, and they can contain punctuation. Example usage: to treat
	 * emoticons as words, or phrases as single tokens (though this is a bit
	 * flaky due to the lack of punctuation handling in Dictionary).
	 * 
	 * @param dict
	 * @param translate If true, translate the words. If false, the dictionary "protects"
	 * words and phrases, but does not translate them.
	 */
	public void addDictionary(IDictionary dict, boolean translate) {
		assert !translate; // TODO
		dicts = dicts == null ? new IDictionary[1] : Arrays.copyOf(dicts, dicts.length + 1);
		dictsTranslate = dictsTranslate == null ? new boolean[1] : Arrays.copyOf(dictsTranslate, dicts.length);
		
		dicts[dicts.length - 1] = dict;
		dictsTranslate[dicts.length - 1] = translate;
		
		assert dicts.length == dictsTranslate.length;
		// imperfect but probably OK handling of dicts
		// We want to add a key/value "hash" which can act as a uid for this dictionary.
		// NB: Have to allow for other dictionaries too.
		desc.put(new Key("dict" + dicts.length+(translate?"translate":"")), 
						dict.getClass().getSimpleName());
	}

	/**
	 * Construct a new WordAndPunctuationTokeniser, with the specified string as
	 * input.
	 * <p>
	 * Sub-classes should override and call {@link #factory2(String)}, to
	 * avoid the deep-copy which this class will otherwise do for safety. 
	 * @testedby {@link WordAndPunctuationTokeniserTest#testInstantiate()}
	 */
	@Override
	public WordAndPunctuationTokeniser factory(String newInput) {
		if (getClass() != WordAndPunctuationTokeniser.class) {
			// deep copy for safety
			WordAndPunctuationTokeniser clone = Utils.copy(this);
			clone.setInput(newInput);
			return clone;
		}
		WordAndPunctuationTokeniser wpt = new WordAndPunctuationTokeniser(
				newInput);
		return factory2(wpt);
	}
	
	protected WordAndPunctuationTokeniser factory2(WordAndPunctuationTokeniser wpt) {		
		if (dicts != null) {
			for (IDictionary dict : dicts) {
				wpt.addDictionary(dict, false);
			}
		}
		wpt.setLowerCase(lowerCase);
		wpt.setNormaliseToAscii(normaliseToAscii);
		wpt.setSplitOnApostrophe(splitOnApostrophe);
		wpt.setSwallowPunctuation(swallowPunctuation);
		wpt.setUrlsAsWords(urlsAsWords);
//		wpt.setSupportBrownPosTags(Utils.yes(brownPosTags));
		return wpt;
	}

	/**
	 * The dictionaries in use
	 * 
	 * @return Can be null
	 */
	public IDictionary[] getDictionaries() {
		return dicts;
	}

	/**
	 * @param c
	 * @return true if c is any of the (normal) apostrophe characters
	 */
	private boolean isApostrophe(char c) {
		return c == '\'' || c == '`'
				|| c == "\u2018".charAt(0)  // left single quotation mark
				|| c == "\u2019".charAt(0)  // right single quotation mark
				|| c == "\u201b".charAt(0); // single high-reversed-9 quotation mark
	}

	/**
	 * @param c
	 * @return true if c can start a word
	 *         <p>
	 *         This method can be over-ridden for custom behaviour
	 */
	protected boolean isWordStart(char c) {
		return Character.isLetter(c);
	}

	@Override
	public AbstractIterator<Tkn> iterator() {
		return new AbstractIterator<Tkn>() {
			/**
			 * char to possibly begin the next word
			 */
			private Mutable.Int i = new Mutable.Int(0);
			@Override
			protected Tkn next2() {
				return WordAndPunctuationTokeniser.this.next2(i);
			}
		};
	}

	protected Tkn next2(Mutable.Int i) {
		// skip through whitespace
		char c = ' ';
		while (i.value < input.length()) {
			c = input.charAt(i.value);
			if (!Character.isWhitespace(c)) {
				break;
			}
			i.value++;
		}
		// end of input?
		if (i.value == input.length())
			return null;
		// pick out words and chunks of punctuation
		int start = i.value;
		int end = -1;
		boolean isUrl = false;
		assert c != ' ';

		// first: dictionary, if set, gets special treatment
		if (dicts != null) {
			Tkn token = next3_dict(start, i);
			if (token != null)
				return token;
		}

		if (isWordStart(c)) {
			// is it a url?
			if (urlsAsWords) {
				end = next3_possibleUrl(start);
				if (end != -1) {
					isUrl = true;
				}
			}
			// no - it's a word
			if (!isUrl) {
				end = next3_wordOrCode(start);
			}
		} else if (Character.isDigit(c)) {
			end = next3_numberOrCode(start);
		} else {
			end = next3_punctuation(start);
			// ignore this token?
			// TODO more fine grained or over-ridable control?
			if (swallowPunctuation) {
				i.value = end;
				return next2(i);
			}
			// TODO should we have a punctuation POS tag?
		}
		i.value = end;
		String word;
		if (swallowPunctuation && !isUrl) {
			// make sure we have no punctuation e.g. with "can't" or
			// "U.K."
			StringBuilder word2 = new StringBuilder();
			// take 1st letter without checking (allows override of
			// #isWordStart)
			word2.append(input.charAt(start));
			// scan the rest to be sure
			for (int j = start + 1; j < end; j++) {
				char c2 = input.charAt(j);
				if (!Character.isLetterOrDigit(c2)) {
					continue;
				}
				word2.append(c2);
			}
			word = word2.toString();
		} else {
			word = input.substring(start, end);
		}
		// lowercase?
		// Hm: why not do this at the input level??
		if (lowerCase && ! isUrl) {
			word = word.toLowerCase(Locale.ENGLISH);
		}
		if (normaliseToAscii != null && ! isUrl) {
			word = StrUtils.normalise(word, normaliseToAscii);
		}
		// Done!
		Tkn token = new Tkn(word, start, end);
		if (isUrl) {
			token.setPOS(POS_URL);
		}
		return token;
	}

	/**
	 * @param start
	 * @return word, or null. Advances i if successful
	 */
	private Tkn next3_dict(int start, Mutable.Int i) {
		assert dicts.length == dictsTranslate.length : Printer.toString(this.dicts);
		for (int di=0; di<dicts.length; di++) {
			IDictionary dict = dicts[di];
			String word = dict.match(input, start);
			if (word == null) {
				continue;
			}
			i.value = start + word.length();
			// Translate?
			if (dictsTranslate[di]) {
				String mword = dict.getMeaning(word);
				if (mword!=null && ! mword.isEmpty()) {
					word = mword;
				}
			}
			// skip any later processing such as lowercasing
			Tkn token = new Tkn(word, start, i.value);
			return token;
		}
		return null;
	}

	private int next3_numberOrCode(int ci) {
		while (ci < input.length()) {
			char c = input.charAt(ci);
			ci++;
			if (Character.isLetterOrDigit(c) || c == '_') {
				continue;
			}
			// 100.00 versus 100.Hello and 1,000 versus bad spacing in 123,Hello
			if (c == ',' || c == '.') {
				if (Character.isDigit(StrUtils.charAt(input, ci))) {
					continue;
				}
				return ci - 1;
			}
			// other punctuation: nope;
			return ci - 1;
		}
		return ci;
	}

	/**
	 * @param start
	 * @return end offset for the url, if we are at the start of one, or -1
	 */
	private int next3_possibleUrl(int start) {
		assert isWordStart(input.charAt(start));
		Matcher m = URL_REGEX2.matcher(input);
		// local search only please
		m.region(start, input.length());
		boolean yes = m.find();
		if (yes)
			return m.end();
		return -1;
	}

	/**
	 * Called even if {@link #swallowPunctuation} is true
	 * 
	 * @param ci
	 * @return
	 */
	private int next3_punctuation(int ci) {
		while (ci < input.length()) {
			char c = input.charAt(ci);
			if (Character.isLetterOrDigit(c) || Character.isWhitespace(c))
				return ci;
			ci++;
		}
		return ci;
	}

	private int next3_wordOrCode(int ci) {
		// Take the first letter as OK without testing
		assert isWordStart(input.charAt(ci)) : ci + ": " + input.charAt(ci);
		ci++;
		while (ci < input.length()) {
			char c = input.charAt(ci);
			if (!Character.isLetterOrDigit(c) && c != '_') {
				// apostrophes are a wee bit special
				if (!isApostrophe(c))
					return ci;
				if (splitOnApostrophe)
					return ci;
			}
			ci++;
		}
		return ci;
	}

	public void setInput(String input) {
		this.input = input;
	}

	/**
	 * @param b
	 *            If true, all words (except urls) are converted to lower case. false by
	 *            default
	 * @return 
	 */
	public WordAndPunctuationTokeniser setLowerCase(boolean b) {
		lowerCase = b;
		desc.put(new Key("lwr"), lowerCase);
		return this;
	}

	/**
	 * Convert words into Ascii?
	 * 
	 * Does not affect urls (but they're usually in ascii anyway).
	 * 
	 * @param normaliseToAscii
	 *            Can be null (the default) for no-normalisation.
	 * @return 
	 * @see StrUtils#normalise(String, KErrorPolicy)
	 */
	public WordAndPunctuationTokeniser setNormaliseToAscii(KErrorPolicy normaliseToAscii) {
		this.normaliseToAscii = normaliseToAscii;
		desc.put(new Key("nrm"), normaliseToAscii);
		return this;
	}

	/**
	 * Apostrophes can be a little special: By default we split on apostrophe,
	 * the same as for comma, whistepace, etc. However if set to false, "can't"
	 * will remain "can't", and not ["can", "'", "t"], and "dogs'" will remain
	 * "dogs'", and not ["dogs", "'"]. The default is true (ie. we do split out
	 * tokens on apostrophe).
	 * <p>
	 * Note: this is switched to false by
	 * {@link #setSwallowPunctuation(boolean)}. The combination leads to "can't"
	 * becoming "cant".
	 * 
	 * TODO should this be smart (i.e. know about 't and 's)? e.g. "'x'" will
	 * get split out, but "won't" won't?
	 * 
	 * @param doSplit
	 * @testedby {@link WordAndPunctuationTokeniserTest#testApostrophe()}
	 */
	public void setSplitOnApostrophe(boolean doSplit) {
		this.splitOnApostrophe = doSplit;
		desc.put(new Key("splt"), splitOnApostrophe);
	}

	/**
	 * If true, punctuation tokens will not be emitted. false by default.
	 * <p>
	 * Apostrophes are a little special: setting true will switch to swallowing
	 * 's within a word. So "can't" will become "cant", and not "can", "t". This
	 * can be changed by calling {@link #setSplitOnApostrophe(boolean)} _after_
	 * this method.
	 * @return 
	 */
	public WordAndPunctuationTokeniser setSwallowPunctuation(boolean b) {
		swallowPunctuation = b;
		// Hm... a bit dangerous this - what if they've already set it to their
		// taste?
		// Well, the documentation is clear.
		if (b) {
			setSplitOnApostrophe(false);
		}
		desc.put(new Key("swllw"), swallowPunctuation);
		return this;
	}

	/**
	 * @param urlsAsWords
	 *            If true, urls will be returned as individual tokens (ie.
	 *            unchopped by the punctuation cutter). They are also protected 
	 *            against lowercasing. true by default.<br>
	 *            url tokens get the attribute {@link Tkn#POS} set to {@link #POS_URL}.
	 * @return 
	 */
	public WordAndPunctuationTokeniser setUrlsAsWords(boolean urlsAsWords) {
		this.urlsAsWords = urlsAsWords;
		desc.put(new Key("urls"), urlsAsWords);
		return this;
	}

	@Override
	public String toString() {
		return "WordAndPunctuationTokeniser [urlsAsWords=" + urlsAsWords
				+ ", swallowPunctuation=" + swallowPunctuation
				+ ", splitOnApostrophe=" + splitOnApostrophe + ", lowerCase="
				+ lowerCase + "]" + StrUtils.ellipsize(input, 140);
	}

}
