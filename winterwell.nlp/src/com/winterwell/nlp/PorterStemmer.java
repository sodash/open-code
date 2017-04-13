/*
 * (c) Winterwell
 * December 2008
 */
package com.winterwell.nlp;

import java.util.Map;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.frenchStemmer;
import org.tartarus.snowball.ext.germanStemmer;
import org.tartarus.snowball.ext.italianStemmer;
import org.tartarus.snowball.ext.portugueseStemmer;
import org.tartarus.snowball.ext.russianStemmer;
import org.tartarus.snowball.ext.spanishStemmer;
import org.tartarus.snowball.ext.turkishStemmer;

import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.log.Log;

/**
 * TODO upgrade to the latest Snowball from snowballstem.org
 * BUT this has a tricky compilation process.
 * 
 * TODO Other languages!
 * 
 * The Stemmer class transforms an English word into its root form.
 * 
 * http://snowball.tartarus.org/index.php
 * 
 * @warning things can get mangled, so not suitable for hashtags, email
 *          addresses and links.
 *          <p>
 *          Porter stemmer in Java originally written by Martin Porter.
 *          <p>
 *          The original paper is in
 *          <p>
 *          Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14,
 *          no. 3, pp 130-137,
 *          <p>
 *          See <a
 *          href="http://www.tartarus.org/~martin/PorterStemmer">homepage</a>
 * 
 * @author Martin Porter, Tiphaine Dalmas, Daniel Winterstein
 * @testedby PorterStemmerTest
 */
public final class PorterStemmer {

	private final String lang;

	public PorterStemmer() {	
		this("en");
	}
	
	public PorterStemmer(String lang) {
		assert lang.length() == 2 : lang;
//		if ( ! "en".equals(lang)) {
//			Log.e("lang", "Using English PorterStemmer for "+lang+" :(");
//			lang = "en"; // :(
//		}
		this.lang = lang;
	}

	boolean modifyStem2word;

	// TODO make this static?
	// It SHOULD get shared via "proper" use of StemmerFilter.factory()
	Map<String, String> stem2word;

	/**
	 * Retrieve the stem dictionary. May be null.
	 * 
	 * @return
	 */
	public Map<String, String> getStemDictionary() {
		return stem2word;
	}

	/**
	 * False by default. If true, will keep a map of the most recently seen
	 * 50,000 stem-to-(seen)word mappings which allows stems to be converted
	 * into words.
	 * 
	 * The "stems" returned will be the shortest word with that stem.
	 * <p>
	 * Warning: this means stems can mutate if a new shorter word is seen! You
	 * can guard against this by feeding the stemmer a dictionary first.
	 * </p>
	 * 
	 * @deprecated Use {@link #setStemDictionary(Map)} instead
	 * @param maintainDictionary
	 */
	@Deprecated
	public void setDictionaryStems(boolean maintainDictionary) {
		if ( ! maintainDictionary) {
			stem2word = null;
			modifyStem2word = false;
			return;
		}
		if (stem2word == null) {
			stem2word = new Cache<String, String>(50000);
			modifyStem2word = true;
		}
	}

	/**
	 * Set a dictionary of stems to root words. Set to null to switch off the
	 * stemming dictionary altogether. If modifyStems is true newly encountered
	 * stems will be added. Using a map with bounded capacity (such as @link
	 * {@link Cache} is encouraged in this case.
	 * <p>
	 * Warning! Adding a modifiable dictionary can have surprising effects e.g.
	 * "monkeys" will be stemmed as "monkeys" if the stemmer has never
	 * encountered a "monkey".
	 * </p>
	 * 
	 * @param dictionary
	 * @param shouldModify
	 *            if true, old stems will be updated with newer shorter roots
	 *            and newly encountered stems will be added.
	 */
	// Possibly these two modification behaviours should be separated out.
	public void setStemDictionary(Map<String, String> dictionary,
			boolean shouldModify) {
		assert (dictionary != null || !shouldModify); // Probably an error
		stem2word = dictionary;
		this.modifyStem2word = shouldModify;
	}

	/**
	 * Stem this word
	 * 
	 * @param word
	 * @return stemmed lower-cased version of word
	 */
	public String stem(String word) {
		// the code only works on lower-case
		word = word.toLowerCase();
		String stem = stem2(word);
		
		if (stem2word == null)
			return stem;
		String w = stem2word.get(stem);
		if (w == null || word.length() < w.length()) {
			if (modifyStem2word) {
				stem2word.put(stem, word);
			}
			return word;
		}
		return w;
	}

	private String stem2(String word) {
		if ("en".equals(lang)) {
			PorterStemmer2 ps = new PorterStemmer2(word.length());
			ps.add(word.toCharArray(), word.length());
			ps.stem();
			return ps.toString();			
		}
		try {
			SnowballStemmer ss = null;
			if ("es".equals(lang)) {
				ss = new spanishStemmer();
			} else if ("fr".equals(lang)) {
				ss = new frenchStemmer();
			} else if ("de".equals(lang)) {
				ss = new germanStemmer();
			} else if ("pt".equals(lang)) {
				ss = new portugueseStemmer();
			} else if ("ru".equals(lang)) {
				ss = new russianStemmer();
			} else if ("tr".equals(lang)) {
				ss = new turkishStemmer();
			} else if ("it".equals(lang)) {
				ss = new italianStemmer();
			} else if ("pt".equals(lang)) {
				ss = new portugueseStemmer();
			}
			// swedish danish dutch norwegian romanian finnish		
			if (ss==null) {
				// Fallback to English
				PorterStemmer2 ps = new PorterStemmer2(word.length());
				ps.add(word.toCharArray(), word.length());
				ps.stem();
				return ps.toString();
			}
			ss.setCurrent(word);
			boolean ok = ss.stem();
			String stem = ss.getCurrent();
			return stem;
		} catch(Throwable ex) {
			// paranoia robustness (error seen on DW local computer March 2017)
			Log.e("PorterStemmer", ex);
			// fallback to English
			PorterStemmer2 ps = new PorterStemmer2(word.length());
			ps.add(word.toCharArray(), word.length());
			ps.stem();
			return ps.toString();			
		}
	}

	/**
	 * @return whether or not newly encountered stems will be added
	 */
	public boolean willModifyStemDictionary() {
		return modifyStem2word;
	}

}

/**
 * Does the work for {@link PorterStemmer}. Makes PorterStemmer thread safe.
 * FIXME Should we switch to Snowball's Porter2 -- which is not the same algorithm!!
 * @author Daniel
 * 
 */
final class PorterStemmer2 {

	/** unit of size whereby b is increased */
	private static final int INC = 50;
	private char[] b;
	/** offset to end of stemmed word in b */
	private int i_end, j, k;
	/** offset into b */
	private int i_in_b;

	public PorterStemmer2(int length) {
		b = new char[length];
		i_in_b = 0;
		i_end = 0;
	}

	/**
	 * Add a character to the word being stemmed. When you are finished adding
	 * characters, you can call stem(void) to stem the word.
	 */
	void add(char ch) {
		if (i_in_b == b.length) {
			char[] new_b = new char[i_in_b + INC];
			for (int c = 0; c < i_in_b; c++) {
				new_b[c] = b[c];
			}
			b = new_b;
		}
		b[i_in_b++] = ch;
	}

	/**
	 * Adds wLen characters to the word being stemmed contained in a portion of
	 * a char[] array. This is like repeated calls of add(char ch), but faster.
	 */
	void add(char[] w, int wLen) {
		if (i_in_b + wLen >= b.length) {
			char[] new_b = new char[i_in_b + wLen + INC];
			for (int c = 0; c < i_in_b; c++) {
				new_b[c] = b[c];
			}
			b = new_b;
		}
		for (int c = 0; c < wLen; c++) {
			b[i_in_b++] = w[c];
		}
	}

	/** cons(i) is true <=> b[i] is a consonant. */

	private final boolean cons(int i) {
		switch (b[i]) {
		case 'a':
		case 'e':
		case 'i':
		case 'o':
		case 'u':
			return false;
		case 'y':
			return (i == 0) ? true : !cons(i - 1);
		default:
			return true;
		}
	}

	/**
	 * cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
	 * and also if the second c is not w,x or y. this is used when trying to
	 * restore an e at the end of a short word. e.g.
	 * 
	 * cav(e), lov(e), hop(e), crim(e), but snow, box, tray.
	 */
	private final boolean cvc(int i) {
		if (i < 2 || !cons(i) || cons(i - 1) || !cons(i - 2))
			return false;
		{
			int ch = b[i];
			if (ch == 'w' || ch == 'x' || ch == 'y')
				return false;
		}
		return true;
	}

	/** doublec(j) is true <=> j,(j-1) contain a double consonant. */
	private final boolean doublec(int j) {
		if (j < 1)
			return false;
		if (b[j] != b[j - 1])
			return false;
		return cons(j);
	}

	private final boolean ends(String s) {
		int l = s.length();
		int o = k - l + 1;
		if (o < 0)
			return false;
		for (int i = 0; i < l; i++)
			if (b[o + i] != s.charAt(i))
				return false;
		j = k - l;
		return true;
	}

	/**
	 * Returns a reference to a character buffer containing the results of the
	 * stemming process. You also need to consult getResultLength() to determine
	 * the length of the result.
	 */
	char[] getResultBuffer() {
		return b;
	}

	/**
	 * Returns the length of the word resulting from the stemming process.
	 */
	int getResultLength() {
		return i_end;
	}

	/**
	 * m() measures the number of consonant sequences between 0 and j. if c is a
	 * consonant sequence and v a vowel sequence, and <..> indicates arbitrary
	 * presence,
	 * 
	 * <c><v> gives 0 <c>vc<v> gives 1 <c>vcvc<v> gives 2 <c>vcvcvc<v> gives 3
	 * ....
	 */
	private final int m() {
		int n = 0;
		int i = 0;
		while (true) {
			if (i > j)
				return n;
			if (!cons(i)) {
				break;
			}
			i++;
		}
		i++;
		while (true) {
			while (true) {
				if (i > j)
					return n;
				if (cons(i)) {
					break;
				}
				i++;
			}
			i++;
			n++;
			while (true) {
				if (i > j)
					return n;
				if (!cons(i)) {
					break;
				}
				i++;
			}
			i++;
		}
	}

	/** r(s) is used further down. */
	private final void r(String s) {
		if (m() > 0) {
			setto(s);
		}
	}

	/**
	 * setto(s) sets (j+1),...k to the characters in the string s, readjusting
	 * k.
	 */
	private final void setto(String s) {
		int l = s.length();
		int o = j + 1;
		for (int i = 0; i < l; i++) {
			b[o + i] = s.charAt(i);
		}
		k = j + l;
	}

	/**
	 * Stem the word placed into the Stemmer buffer through calls to add().
	 * Returns true if the stemming process resulted in a word different from
	 * the input. You can retrieve the result with
	 * getResultLength()/getResultBuffer() or toString().
	 */
	void stem() {
		k = i_in_b - 1;
		if (k > 1) {
			step1();
			step2();
			step3();
			step4();
			step5();
			step6();
		}
		i_end = k + 1;
		i_in_b = 0;
	}

	/**
	 * step1() gets rid of plurals and -ed or -ing. e.g.
	 * 
	 * caresses -> caress ponies -> poni ties -> ti caress -> caress cats -> cat
	 * 
	 * feed -> feed agreed -> agree disabled -> disable
	 * 
	 * matting -> mat mating -> mate meeting -> meet milling -> mill messing ->
	 * mess
	 * 
	 * meetings -> meet
	 */
	private final void step1() {
		if (b[k] == 's') {
			if (ends("sses")) {
				k -= 2;
			} else if (ends("ies")) {
				setto("i");
			} else if (b[k - 1] != 's') {
				k--;
			}
		}
		if (ends("eed")) {
			if (m() > 0) {
				k--;
			}
		} else {
			if ((ends("ed") || ends("ing")) && vowelinstem()) {
				k = j;
				if (ends("at")) {
					setto("ate");
				} else if (ends("bl")) {
					setto("ble");
				} else if (ends("iz")) {
					setto("ize");
				} else if (doublec(k)) {
					k--;
					int ch = b[k];
					if (ch == 'l' || ch == 's' || ch == 'z') {
						k++;
					}
				}
			}
		}
	}

	/** step2() turns terminal y to i when there is another vowel in the stem. */

	private final void step2() {
		if (ends("y") && vowelinstem()) {
			b[k] = 'i';
		}
	}

	/**
	 * step3() maps double suffices to single ones. so -ization ( = -ize plus
	 * -ation) maps to -ize etc. note that the string before the suffix must
	 * give m() > 0.
	 */

	private final void step3() {
		if (k == 0)
			return;
		/** For Bug 1 */
		switch (b[k - 1]) {
		case 'a':
			if (ends("ational")) {
				r("ate");
				break;
			}
			if (ends("tional")) {
				r("tion");
				break;
			}
			break;
		case 'c':
			if (ends("enci")) {
				r("ence");
				break;
			}
			if (ends("anci")) {
				r("ance");
				break;
			}
			break;
		case 'e':
			if (ends("izer")) {
				r("ize");
				break;
			}
			break;
		case 'l':
			if (ends("bli")) {
				r("ble");
				break;
			}
			if (ends("alli")) {
				r("al");
				break;
			}
			if (ends("entli")) {
				r("ent");
				break;
			}
			if (ends("eli")) {
				r("e");
				break;
			}
			if (ends("ousli")) {
				r("ous");
				break;
			}
			break;
		case 'o':
			if (ends("ization")) {
				r("ize");
				break;
			}
			if (ends("ation")) {
				r("ate");
				break;
			}
			if (ends("ator")) {
				r("ate");
				break;
			}
			break;
		case 's':
			if (ends("alism")) {
				r("al");
				break;
			}
			if (ends("iveness")) {
				r("ive");
				break;
			}
			if (ends("fulness")) {
				r("ful");
				break;
			}
			if (ends("ousness")) {
				r("ous");
				break;
			}
			break;
		case 't':
			if (ends("aliti")) {
				r("al");
				break;
			}
			if (ends("iviti")) {
				r("ive");
				break;
			}
			if (ends("biliti")) {
				r("ble");
				break;
			}
			break;
		case 'g':
			if (ends("logi")) {
				r("log");
				break;
			}
		}
	}

	/** step4() deals with -ic-, -full, -ness etc. similar strategy to step3. */
	private final void step4() {
		switch (b[k]) {
		case 'e':
			if (ends("icate")) {
				r("ic");
				break;
			}
			if (ends("ative")) {
				r("");
				break;
			}
			if (ends("alize")) {
				r("al");
				break;
			}
			break;
		case 'i':
			if (ends("iciti")) {
				r("ic");
				break;
			}
			break;
		case 'l':
			if (ends("ical")) {
				r("ic");
				break;
			}
			if (ends("ful")) {
				r("");
				break;
			}
			break;
		case 's':
			if (ends("ness")) {
				r("");
				break;
			}
			break;
		}
	}

	/** step5() takes off -ant, -ence etc., in context <c>vcvc<v>. */
	private final void step5() {
		if (k == 0)
			return;
		/** for Bug 1 */
		switch (b[k - 1]) {
		case 'a':
			if (ends("al")) {
				break;
			}
			return;
		case 'c':
			if (ends("ance")) {
				break;
			}
			if (ends("ence")) {
				break;
			}
			return;
		case 'e':
			if (ends("er")) {
				break;
			}
			return;
		case 'i':
			if (ends("ic")) {
				break;
			}
			return;
		case 'l':
			if (ends("able")) {
				break;
			}
			if (ends("ible")) {
				break;
			}
			return;
		case 'n':
			if (ends("ant")) {
				break;
			}
			if (ends("ement")) {
				break;
			}
			if (ends("ment")) {
				break;
			}
			/** element etc. not stripped before the m */
			if (ends("ent")) {
				break;
			}
			return;
		case 'o':
			if (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) {
				break;
			}
			/** j >= 0 fixes Bug 2 */
			if (ends("ou")) {
				break;
			}
			return;
			/** takes care of -ous */
		case 's':
			if (ends("ism")) {
				break;
			}
			return;
		case 't':
			if (ends("ate")) {
				break;
			}
			if (ends("iti")) {
				break;
			}
			return;
		case 'u':
			if (ends("ous")) {
				break;
			}
			return;
		case 'v':
			if (ends("ive")) {
				break;
			}
			return;
		case 'z':
			if (ends("ize")) {
				break;
			}
			return;
		default:
			return;
		}
		if (m() > 1) {
			k = j;
		}
	}

	/** step6() removes a final -e if m() > 1. */

	private final void step6() {
		j = k;
		if (b[k] == 'e') {
			int a = m();
			if (a > 1 || a == 1 && !cvc(k - 1)) {
				k--;
			}
		}
		if (b[k] == 'l' && doublec(k) && m() > 1) {
			k--;
		}
	}

	/**
	 * After a word has been stemmed, it can be retrieved by toString(), or a
	 * reference to the internal buffer can be retrieved by getResultBuffer and
	 * getResultLength (which is generally more efficient.)
	 */
	@Override
	public String toString() {
		return new String(b, 0, i_end);
	}

	/** vowelinstem() is true <=> 0,...j contains a vowel */
	private final boolean vowelinstem() {
		int i;
		for (i = 0; i <= j; i++)
			if (!cons(i))
				return true;
		return false;
	}

}
