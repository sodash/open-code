package com.winterwell.nlp.dict;

import java.io.File;
import java.util.Collections;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.utils.log.Log;

/**
 * A simple emoticon filter based on a list.
 * 
 * @testedby  EmoticonDictionaryTest}
 * @author daniel
 */
public class EmoticonDictionary extends Dictionary {

	private static EmoticonDictionary _emotes;

	private static File getEmoticonFile() {
		NLPWorkshop workshop = NLPWorkshop.get();
		File dictFile = workshop.getFile("emoticons.txt");		
		return dictFile;
	}

	/**
	 * Loads from file, so try to cache these
	 */
	EmoticonDictionary() {
		super(getEmoticonFile(), '\t');
		FLAGS = 0;
	}
	
	
	private EmoticonDictionary(boolean dummy) {
		super(Collections.EMPTY_MAP);
		FLAGS = 0;
	}
	
	public static EmoticonDictionary getDefault() {
		if (_emotes==null) {
			try {
				_emotes = new EmoticonDictionary();
			} catch (Throwable ex) {
				// Probably a security-issue blocking depot 
				Log.e("init", "No EmoticonDictionary support: "+ex);
				_emotes = new EmoticonDictionary(true);
			}
		}
		return _emotes;
	}

	@Override
	protected String toCanonical(String word) {
		return word.trim();
	}

}
