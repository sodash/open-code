package com.winterwell.nlp.spam;

import java.io.File;
import java.util.HashMap;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.PorterStemmer;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.log.Log;

/**
 * A simple rude words filter based on a list (rude-words.txt in Depot).
 * 
 * TODO support stemming -- perhaps via support for specifying a tokeniser
 * 
 * @testedby  RudeWordsFilterTest}
 * @author daniel
 */
public class RudeWordsFilter implements IFilter<String> {

	/**
	 * word 2 meaning
	 */
	final HashMap<String, String> rude = new HashMap<String, String>();

	/**
	 * @return Number of rude words. Does not include
	 */
	public int size() {
		return rude.size();
	}
	

	/**
	 * @param word
	 * @return The meaning of word OR the translation of word
	 */
	public String getMeaning(String word) {
		word = word.trim().toLowerCase();
		String m = rude.get(word);
		if (m!=null) return m;
		// Also catch embedded fucks
		if (word.contains("fuck"))
			return word.replace("fuck", "");
		return null;
	}
	
	
	/**
	 * Default: English, Spanish, French, German
	 * NB: This processes a text file. So it may be worth caching these objects.
	 */
	public RudeWordsFilter() {
		this(new String[]{"en", "es", "de", "fr"});
	}
	
	/**
	 * Create from a list of words
	 * @param rudeWordsFile
	 */
	public RudeWordsFilter(File rudeWordsFile) {
		addFile(rudeWordsFile);
	}
	
	/**
	 * NB: This processes a text file. So it may be worth caching these objects.
	 */
	public RudeWordsFilter(String[] languages) {	
		for(String lang : languages) {
			try {
				NLPWorkshop workshop = NLPWorkshop.get(lang);
				File rudeWordsFile = workshop.getFile("rude-words.txt");
				if (rudeWordsFile==null || ! rudeWordsFile.exists()) {
					Log.w("rude", "No rude-words.txt file: "+rudeWordsFile);
					continue;
				}
				addFile(rudeWordsFile);
			} catch(Throwable ex) {
				Log.e("RudeWordsFilter", ex);
			}
		}
	}

	public void addFile(File rudeWordsFile) {
		assert rudeWordsFile != null;
		CSVReader reader = new CSVReader(rudeWordsFile, '\t', '"');
		reader.setCommentMarker('#');
		reader.setNumFields(-1);
		for (String[] strings : reader) {
			String w = strings[0];
			if (w.length() < 3) {
				// too short, probably an alphabet marker
				continue;
			}
			// Is there a safe version? E.g. "twatbag" -> "fool"
			String safe = strings.length>1? strings[1] : "";
			addWord(w, safe);
		}
		reader.close();
	}


	public void addWord(String w, String safe) {
		// stem them (& lower-case)
		w = stemmer.stem(w);
		rude.put(w, safe);
		// also with vowels *d
		w = star(w);
		rude.put(w, safe);
		// also via PorterStemmer??
	}


	/**
	 * Uses a standard tokenizer to split this text for words. This is
	 * convenient but not the most robust of defences!
	 * 
	 * @param txt Can be null
	 * @return rude word or null.
	 */
	public String containsRudeWord(String txt) {
		if (txt==null) return null;
		WordAndPunctuationTokeniser tokeniser = new WordAndPunctuationTokeniser(
				txt);
		for (Tkn tok : tokeniser) {
			if (isRude(tok.getText()))
				return tok.getText();
		}
		return null;
	}

	private static final PorterStemmer stemmer = new PorterStemmer();
	
	/**
	 * @param word
	 *            A single word. case-insensitive
	 * @return true if this is rude
	 */
	public boolean isRude(String word) {
		word = word.trim().toLowerCase();
		// stem it (assume English)
		word = stemmer.stem(word);
		if (rude.containsKey(word))
			return true;
		// Also catch embedded fucks TODO factor out to support other words
		if (word.contains("fuck"))
			return true;
		return false;
	}

	/**
	 * @return sample swearword (post-stemming, so sometimes not quite English)
	 */
	public String sample() {
		return Utils.getRandomMember(rude.keySet());
	}

	private String star(String w) {
		return w.replaceFirst("[aeiou]", "*");
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	/**
	 * Used as a filter, this will reject rude texts via {@link #containsRudeWord(String)}
	 */
	@Override
	public boolean accept(String x) {		
		return containsRudeWord(x)==null;
	}

}
