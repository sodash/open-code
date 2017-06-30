package com.winterwell.nlp.dict;

import java.util.Iterator;

/** 
 * Wraps the CMU Dictionary to provide a Rhyming Dictionary
 * 
 * TODO CMU vowels carry a lexical stress marker: 0 — No stress 1 — Primary stress 2 — Secondary stress
Also, our defn of rhyme is not perfect -- see https://en.wikipedia.org/wiki/Rhyme
we're doing syllabic rhymes.
 * 
 * @author daniel
 * @testedby {@link RhymeDictionaryTest}
 * @see CMUDict
 */
public class RhymeDictionary implements IDictionary {

	CMUDict cmud = new CMUDict();
	
	@Override
	public Iterator<String> iterator() {
		return cmud.getAllWords().iterator();
	}

	@Override
	public boolean contains(String word) {
		return cmud.getPhonemes(word) != null;
	}

	@Override
	public String getMeaning(String word) {
		String[] phonemes = cmud.getPhonemes(word);
		if (phonemes==null) {
			return null;
		}
		return phonemes[phonemes.length - 1];
	}

	@Override
	public String[] getMeanings(String word) {
		String[] phonemes = cmud.getPhonemes(word);
		if (phonemes==null) {
			return null;
		}
		return new String[]{phonemes[phonemes.length - 1]};
	}

	@Override
	public String match(String input, int start) {
		// TODO Auto-generated method stub
		return null;
	}

}
