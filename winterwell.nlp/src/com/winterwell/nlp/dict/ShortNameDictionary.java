package com.winterwell.nlp.dict;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.utils.containers.Cache;

/**
 * Short names, e.g. bob = robert.
 * 
 * Note: this accepts any-case as input, but always returns lowercased names. 
 * 
 * Uses the datastore/winterwell.nlp file for "short-names.txt" 
 * 
 * @testedby  ShortNameDictionaryTest}
 * @author daniel
 */
public class ShortNameDictionary extends Dictionary {

	/**
	 * 
	 * @param lang
	 * @return Can return null
	 */
	private static File getDictFile(String lang) {
		NLPWorkshop workshop = NLPWorkshop.get(lang);
		File dictFile = workshop.getFile("short-names.txt");
		return dictFile;
	}
	
	/**
	 * Lower-cases & leniently normalises non-ascii chars.
	 * 
	 * TODO should we use MetaPhone?? Should we do any special case stemming?
	 */
	@Override
	public String toCanonical(String word) {		
		String w = super.toCanonical(word);
		return w;
	}
	
	private Dictionary short2long;
	
	@Override
	public void add(String word, String meaning) {
		super.add(word, meaning);
		// reverse
		// NB: this method is called by the super constructor, so before this class gets to init its fields! 
		if (short2long==null) short2long = new Dictionary();
		short2long.add(meaning,word);
	}
	
	/**
	 * @return The long form of name, or null if not in the dictionary, or "" if there are multiple
	 * long-forms (e.g. Sam=Samuel or Samantha).
	 * @see #getMeanings(String)
	 */
	@Override
	public String getMeaning(String shortName) {
		return super.getMeaning(shortName);
	}
	
	/**
	 * Lower-case
	 */
	@Override
	protected String toCanonicalMeaning(String meaning) {
		return meaning.toLowerCase();
	}

	/**
	 * Reloads the dictionary, so best to cache
	 */
	public ShortNameDictionary() {
		this("en");
	}
	
	public ShortNameDictionary(String lang) {
		super(getDictFile(lang), '\t');
	}

	static final Cache<String,Object> cache = new Cache(4);
	
	/**
	 * Get a cached version
	 * @param lang
	 * @return
	 */
	public static ShortNameDictionary get(String lang) {
		try {
			Object d = cache.get(lang);
			if (d!=null) {
				return d instanceof ShortNameDictionary? (ShortNameDictionary) d : null;
			}
			ShortNameDictionary snd = new ShortNameDictionary(lang);
			cache.put(lang, snd);
			return snd;
		} catch(Throwable ex) {
			cache.put(lang, ex);
			ex.printStackTrace();
			return null;
		}
	}

	public List<String> getShortNames(String firstName) {		
		String[] ms = short2long.getMeanings(firstName);
		if (ms==null) return Collections.EMPTY_LIST;
		return Arrays.asList(ms);
	}

}
