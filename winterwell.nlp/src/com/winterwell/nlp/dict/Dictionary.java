/**
 * 
 */
package com.winterwell.nlp.dict;

import java.io.File;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.nlp.similarity.LevenshteinEditDistance;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.LineReader;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.log.Log;

/**
 * Make a dictionary or thesaurus, e.g. from a file. Includes a simple spell-checker -- getCloseWords()
 * <p>
 * Typical file format:
 * <pre><code>
 * # comment
 * value	translation
 * valueWithoutTranslation
 * </code></pre>
 * <p>
 * If a dictionary contains conflicting translations -- then {@link #getMeanings(String)} will
 * return the list, whilst {@link #getMeaning(String)} semi-fails, returning "".
 * <p>
 * Dictionaries are case-insensitive by default -- in fact they use
 * {@link StrUtils#normalise()} to ignore umlauts, etc. as well. To change this,
 * override {@link #toCanonical(String)}.
 * 
 * @testedby {@link DictionaryTest}
 * @author daniel
 * 
 */
public class Dictionary implements IDictionary {

	/**
	 * This is Unicode char "Information Separator One"
	 * So it shouldn't occur in any meanings.
	 */
	protected static final String DUPE_SEPARATOR = "\u001F";
	

	/**
	 * TODO 
	 * @return the language of this dictionary (e.g. "en"), or null for all.
	 */
	public String getLang() {
		return null;
	}
	
	/**
	 * word to meaning. Multiple meanings are stored separated by {@link #DUPE_SEPARATOR}
	 */
	protected final HashMap<String, String> dict = new HashMap<String, String>();

	/**
	 * match dictionary words - anchored to the start char.
	 * This regex is only made if asked for via {@link #match(String, int)}.
	 * 
	 * TODO this could be quite large -- can we remove it altogether?
	 */
	private transient Pattern dictRegex;

	private File file;

	/**
	 * What flags to use in the regex -- {@link Pattern#CASE_INSENSITIVE} by
	 * default
	 */
	protected int FLAGS = Pattern.CASE_INSENSITIVE;

	/**
	 * All the characters this dictionary uses. Created lazily, so can be null.
	 */
	private transient HashSet<Character> alphabet;

	/**
	 * Format: one word per line. One or two columns, with the first column
	 * being the word, and the optional second column being it's
	 * meaning/translation. 
	 * If there is no translation, "" is used.
	 * <p>
	 * Blank lines and lines starting with a # are ignored.
	 * 
	 * @param file
	 * @param separator
	 *            For the optional 2nd column. Normally tab! (which is the UTX Simple format)
	 */
	public Dictionary(File file, char separator) {
        this(FileUtils.getReader(file), separator);
		assert file != null : file;
		this.file = file;
    }

    public Dictionary(Reader rdr, char separator) {
    	trainFromFile(rdr, separator);
    }
    public void trainFromFile(Reader rdr, char separator) {
		// this.caseSensitive = caseSensitive;
		LineReader reader = new LineReader(rdr);
		if ( ! reader.hasNext()) {
			Log.report("nlp", "Empty dictionary at " + file, Level.WARNING);
			reader.close();
			return;
		}
		for (String line : reader) {
			if (Utils.isBlank(line) || line.startsWith("#")) {
				continue;
			}
			// Is there a 2nd column?
			int i = line.indexOf(separator);
			if (i == -1 || i == line.length() - 1) { // No -- just the word
				add(line, "");
				continue;
			}			
			String word = line.substring(0, i);
			String meaning = line.substring(i + 1).trim();
			add(word, meaning);
		}
		reader.close(); // Not needed. But keep Eclipse's resource checker happy, why not?
	}

	/**
	 * Add a mapping to the dictionary (normally handled by the constructor when using file-based dictionaries)
	 * @param word
	 * @param meaning Use "" if there is no translation as such.
	 */
	public void add(String word, String meaning) {
		// lowercase whilst loading
		word = toCanonical(word);
		if (Utils.isBlank(word)) {
			return;
		}
		meaning = toCanonicalMeaning(meaning);			
		String old = dict.put(word, meaning);
		// Multiple meanings?
		if (old==null || old == "" || old.equals(meaning)) {
			dictRegex=null;
			alphabet=null;
			return;
		}
		if (Containers.contains(meaning, old.split(DUPE_SEPARATOR))) {
			return;
		}
		// Multiple translation!				
		// Log.w(getClass().getSimpleName(), "Multiple translations for "+word+": "+old+" "+meaning);
		dict.put(word, old+DUPE_SEPARATOR+meaning);
	}

	/**
	 * By default, meaning keys are used as-is (unlike input keys, which may get lower-cased).
	 * Over-ride this to implement a canonicalisation scheme.
	 * Use case: protects against dictionary file editors doing unwanted things with case.   
	 * @param meaning
	 * @return meaning to store & return.
	 */
	protected String toCanonicalMeaning(String meaning) {
		return meaning;
	}

	public Dictionary(Map<String, String> word2meaning) {
		for(String k : word2meaning.keySet()) {
			String m = word2meaning.get(k);
			add(k, m);
		}
	}
	
	public Dictionary(Collection<String> words) {
		for(String k : words) {
			add(k, "");
		}
	}
	
	/**
	 * Make a blank dictionary (then use {@link #add(String, String)} to build it up).
	 */
	public Dictionary() {
	
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+file+"]";
	}

	@Override
	public boolean contains(String word) {
		word = toCanonical(word);
		return dict.containsKey(word);
	}

	/**
	 * @return the file used to create this Dictionary, or null if it was made
	 *         in-memory from a Map.
	 */
	public File getFile() {
		return file;
	}

	@Override
	public String getMeaning(String word) {
		word = toCanonical(word);
		String m = dict.get(word);
		if (m==null) return null;
		// Duplicate meanings? Bugger! We can't choose!
		if (m.indexOf(DUPE_SEPARATOR) != -1) {
			return "";
		}
		return m;
	}
	

	@Override
	public final String[] getMeanings(String word) {
		word = toCanonical(word);
		String m = dict.get(word);
		if (m==null) return null;
		if (m=="") return new String[0];
		return m.split(DUPE_SEPARATOR);
	}


	@Override
	public Iterator<String> iterator() {
		return dict.keySet().iterator();
	}

	@Override
	public String match(String input, int start) {
		
		if (dictRegex == null) {
			match2_compileRegex();
		}
		Matcher m = dictRegex.matcher(input);
		m.region(start, m.regionEnd());
		boolean fnd = m.find();
		if (!fnd)
			return null;
		String word = input.substring(start, m.end());
		word = toCanonical(word);
		return word;
	}

	protected void match2_compileRegex() {
		// order by length: longest first
		List<String> words = Containers.getList(this);
		Collections.sort(words, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if (o1.length() == o2.length())
					return o1.compareTo(o2);
				return o1.length() < o2.length() ? 1 : -1;
			}
		});
		// build a big regex
		if (words.isEmpty()) {
			// hack: bogus -- should always fail
			dictRegex = Pattern.compile("^Fail2Fail1Fail3Fail");
			return;
		}
		StringBuilder regex = new StringBuilder("^(");
		for (String w : words) {
			regex.append(Pattern.quote(w));
			regex.append('|');
		}
		StrUtils.pop(regex, 1); // note: assumes dict is not empty
		regex.append(')');
		dictRegex = Pattern.compile(regex.toString(), FLAGS);
	}

	public int size() {
		return dict.size();
	}

	/**
	 * By default, this lower-cases & leniently normalises non-ascii chars.
	 * <p>
	 * Sub-classes can override to implement stricter/looser matching regimes.
	 * 
	 * @param word
	 * @return the canonical version for comparisons.
	 */
	protected String toCanonical(String word) {
		return StrUtils.normalise(word.toLowerCase().trim(),
				KErrorPolicy.ACCEPT);
	}

	public String sample() {
		return Utils.getRandomMember(dict.keySet());
	}

    transient Cache<String,Map<String,Double>> cacheCloseWords;

	/**
	 * @param word
	 * @return close words, with their edit distance. 0=perfect match
	 */
	public Map<String, Double> getCloseWords(String word) {
		word = toCanonical(word);
        // cache answers
        if (cacheCloseWords==null) cacheCloseWords = new Cache(200);
        Map<String, Double> cached = cacheCloseWords.get(word);
        if (cached != null) return cached;

		// Small map? Then check each entry
		if (size() < 10000) {
			// threshold what we consider
			int max = 3;
			// TODO pluggable, so you can use e.g. metafone
			LevenshteinEditDistance dist = new LevenshteinEditDistance();
			Map<String, Double> map = new HashMap();
			for(String w : this) {
				double d = dist.editDistance(word, w);
				if (d>max) continue;
				map.put(w, d);
			}
            cacheCloseWords.put(word, map);
			return map;		
		}
		
		// Large map? Then its faster to check each possible 1-or-2-step edit
		if (alphabet==null) {
			// Build the alphabet of characters this dictionary uses.
			// NB: race-condition: not thread-safe wrt add()
			alphabet = new HashSet<Character>();
			for (String w : this) {
				for(int i=0,n=w.length(); i<n; i++) {
					alphabet.add(w.charAt(i));
				}
			}
		}
		// All 1 step and 2 step edits
		Set<String> step1 = oneEditAway(word, false);
		Set<String> step2 = new HashSet();
		for(String w1 : step1) step2.addAll(oneEditAway(w1, true));
		
		Map<String, Double> map = new HashMap();
		for(String w : step2) {
			map.put(w, 2.0);			
		}
		// NB: do step1 last, so it will overwrite overlaps with step2
		for(String w : step1) {
			if (dict.containsKey(w)) {
				map.put(w, 1.0);
			}
		}
		
        cacheCloseWords.put(word, map);
		return map;
	}

	/**
	* All strings one-edit-step away from the input word.
	* @param filterNow filter here and return dictionary-only words.  
	*/
	private Set<String> oneEditAway(CharSequence word, boolean filterNow) {
		assert alphabet != null;
		// NB: Use String'cos StringBuilder doesnt define equals()
		Set<String> oneEdits = new HashSet();
		// deletes
		for(int i=0; i<word.length(); i++) {
			StringBuilder sb = new StringBuilder(word);
			sb.deleteCharAt(i);
			oneEdits.add(sb.toString());
		}
		// transposes
		for(int i=0; i<word.length()-1; i++) {
			StringBuilder sb = new StringBuilder(word);
			char c0 = word.charAt(i);
			char c1 = word.charAt(i+1);
			sb.setCharAt(i, c1);
			sb.setCharAt(i+1, c0);
			oneEdits.add(sb.toString());
		}
		// replaces and inserts
		for(char c : alphabet) {
			for(int i=0; i<word.length(); i++) {
				// replace
				StringBuilder sb = new StringBuilder(word);
				sb.setCharAt(i, c);
				oneEdits.add(sb.toString());
				// insert
				sb = new StringBuilder(word);
				sb.insert(i, c);
				oneEdits.add(sb.toString());
			}
		}
		// appends
		for(char c : alphabet) {
			StringBuilder sb = new StringBuilder(word);
			sb.append(c);
			oneEdits.add(sb.toString());
		}
		// filter non-words?
		if (filterNow) {
			oneEdits.retainAll(dict.keySet());
		}
		return oneEdits;
	}

}
