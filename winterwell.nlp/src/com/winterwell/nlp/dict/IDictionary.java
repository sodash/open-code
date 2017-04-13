package com.winterwell.nlp.dict;

/**
 * A simple dictionary, where words have only one meaning.
 * 
 * @author daniel
 * 
 */
public interface IDictionary extends Iterable<String> {

	/**
	 * @param word
	 *            A single word
	 * @return true if this is in the dictionary. May or may not be
	 *         case-sensitive depending on the dictionary. Usually NOT case
	 *         sensitive.
	 */
	boolean contains(String word);

	/**
	 * @param word
	 * @return The meaning of word OR the translation of word.
	 * null if not in the dictionary. "" if in the dictionary, but no translation is defined.<br>
	 * <i>If a word has multiple translations: returns ""</i>
	 * @see #getMeanings(String)
	 */
	String getMeaning(String word);
	
	/**
	 * Can handle words with multiple translations (unlike {@link #getMeaning(String)}).
	 * @param word
	 * @return The meaning of word OR the translation of word. <i>Can be null or empty!</i>
	 * null if not in the dictionary. empty if in the dictionary, but no translation is defined.
	 * 
	 * @see #getMeaning(String)
	 */
	String[] getMeanings(String word);

	/**
	 * Does *the start of input* match a word in the dictionary? Uses a big
	 * regex.
	 * 
	 * @param input
	 * @param start
	 * @return matching word or null.
	 *         <p>
	 *         If the dictionary is case-insensitive then this will be
	 *         lower-cased. (??is that what we want?) ??words do NOT have to be
	 *         whole words. E.g. if "hell" is in the dictionary, it will match
	 *         against "hello"
	 */
	String match(String input, int start);

}
