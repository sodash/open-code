package com.winterwell.nlp.dict;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.analysis.SyllableCounter;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.io.LineReader;

/**
 * Wrapper for the CMU pronunciation dictionary, which can be used for syllable-counting and rhyming.
 * See https://en.wikipedia.org/wiki/CMU_Pronouncing_Dictionary
 * @author daniel
 * @testedby {@link CMUDictTest}
 * @see SyllableCounter
 */
public class CMUDict {
	
	public CMUDict() {
		load();
	}
	
	/**
	 * The data is loaded into a static field, so repeated calls are fast and harmless.
	 * @return
	 */
	public CMUDict load() {		
		synchronized (phonemesFromWord) {
			if ( ! phonemesFromWord.isEmpty()) return this;
	//		Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK);
			File file = NLPWorkshop.get("en").getFile("cmudict");
	//		String currentDirectory = System.getProperty("user.dir");
	//		File file = new File(currentDirectory + "/res/model/cmudict");
	//		assert file.isFile() : "No "+filepath+" -> "+file;
			LineReader reader = new LineReader(file);
			for (String input : reader) {
				if (input.startsWith(";;")) continue; // a comment
				String parsed[] = input.toLowerCase().split(" +");
				String word = parsed[0];
				String[] bits = Arrays.copyOfRange(parsed, 1, parsed.length);
				phonemesFromWord.put(word, bits);
				if (bits.length==0) {
					continue;
				}
				String last = lastBits(bits);
				lastToWords.add(last, word);
			}		
			reader.close();
			return this;
		}
	}

	private String lastBits(String[] bits) {
		return bits.length==1? bits[bits.length-1] : bits[bits.length-2]+"-"+bits[bits.length-1];
	}

	/**
	 * 
	 * @param word Cannot be null
	 * @return Always gives an answer (if not in the dictionary, uses {@link SyllableCounter})
	 */
	public int getSyllableCount(String word) {
		word = canon(word);
		String[] phonemes = phonemesFromWord.get(word);
		if (phonemes==null) {
			 // unknown :(
			// HACK: Fallback to SyllableCounter 
			int s = SyllableCounter.syllableCount(word);
			return s;
		}
		int syllables = 1;
		for(String p : phonemes){
			if (p.length() > 0 && p.charAt(0) == '-') {
				syllables++;
			}
		}	
		return syllables;
	}

	private static final Map<String,String[]> phonemesFromWord = new HashMap();
	
	/**
	 * For rhyme lookups: last phoneme -> list of words
	 */
	private static final ListMap<String,String> lastToWords = new ListMap();
	
	public Set<String> getAllWords() {
		return phonemesFromWord.keySet();
	}

	/**
	 * Can be null if unknown
	 * @param string
	 * @return
	 */
	public String[] getPhonemes(String string) {
		return phonemesFromWord.get(canon(string));
	}
	
	private String canon(String string) {
		return StrUtils.toCanonical(string);
	}

	/**
	 * CMU vowels carry a lexical stress marker: 0 — No stress 1 — Primary stress 2 — Secondary stress
	 * @param word
	 * @return stress per syllable
	 */
	public int[] getStressPattern(String word) {
		word = canon(word);
		String[] phonemes = phonemesFromWord.get(word);
		if (phonemes==null) return null;
		int[] sp = new int[phonemes.length];
		int syllables = 0;
		for(String p : phonemes) {
			if (p.endsWith("1")) sp[syllables] = 1;
			if (p.endsWith("2")) sp[syllables] = 2;
			if (p.length() > 0 && p.charAt(0) == '-') {
				syllables++;
			}
		}	
		int[] sptrim = Arrays.copyOf(sp, syllables+1);
		return sptrim;
	}

	/**
	 * TODO our defn of rhyme is not perfect -- see https://en.wikipedia.o … we're doing syllabic rhymes.
	 * @param word
	 * @return rhymes (defined by same final 2 syllables)
	 */
	public List<String> getRhymes(String word) {
		String[] bits = getPhonemes(canon(word));
		if (bits==null) return Collections.EMPTY_LIST;
		List<String> words = lastToWords.get(lastBits(bits));
		if (words==null) return words;
		words = new ArrayList(words);
		words.remove(word);
		return words;
	}

}
