/**
 * 
 */
package com.winterwell.nlp.io;

import java.io.Serializable;
import java.util.Collection;

import com.winterwell.nlp.corpus.brown.BrownCorpusTags;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.containers.ArrayMap;

/**
 * A nugget of text, e.g. a word. Can carry offsets for the original text in
 * which it appeared. Has an associated bag of properties.
 * <p>
 * Tokens are only equals() if they have the same text *and* property bag.
 * <p>
 * Tokens are not immutable: both text and properties can be edited. This is
 * both convenient and efficient, given that tokens typically flow through
 * high-volume pipelines getting edited.
 * 
 * @author daniel
 * 
 * NB - named "Tkn" because every jar file in existence seems to have a "Token" class.
 */
public final class Tkn implements IProperties, Serializable {
	private static final long serialVersionUID = 1L;

	public String getRawText(CharSequence original) {
		if (start==end || start==-1) return null;
		if (end > original.length()) {
			// Swallow and return null??
		}
		return original.subSequence(start, end).toString();
	}
	
	/**
	 * TODO shorten to just "pos"
	 * 
	 * Part-of-speech property for Tokens. The actual values are dependent on
	 * the code which uses this!
	 * @see BrownCorpusTags or http://en.wikipedia.org/wiki/Brown_Corpus#Part-of-speech_tags_used
	 */
	public static final Key<String> POS = new Key<String>("Token.pos");

	/**
	 * A special marker for "start of the text".
	 * Which is also the marker for "end of the text", since the end of one sentence
	 * is the start of another.
	 */
	public static final Tkn START_TOKEN = new Tkn("<start>");
	/**
	 * == START_TOKEN, since the end of one sentence is the start of another.
	 */
	public static final Tkn END_TOKEN = START_TOKEN;

	public static final String UNKNOWN = "<?>";
	static {
		// give it a property to avoid accidental equals with some real text.
		START_TOKEN.put(new Key("marker"), true);
	}

	/**
	 * Last character, exclusive, in the original text
	 */
	public final int end;

	@SuppressWarnings("rawtypes")
	private final ArrayMap<Key, Object> properties = new ArrayMap<Key, Object>();

	/**
	 * First character, inclusive, in the original text.
	 * -1 for unset
	 */
	public final int start;

	/**
	 * The text of this Token
	 */
	private String text;

	/**
	 * Create a token with span information relating to the original source
	 * text.
	 * 
	 * @param processed
	 *            The text that will be stored for this token. The relationship
	 *            between processed and the original can vary.
	 * @param start
	 *            first character in the original, inclusive
	 * @param end
	 *            last character in the original, exclusive
	 */
	public Tkn(CharSequence processed, int start, int end) {
		assert processed != null;
		this.text = processed.toString();
		this.start = start;
		this.end = end;
		assert (start >= 0 && start <= end) || (start==-1 && end==-1) : start+" "+end;
	}

	/**
	 * Create a Token without linked text.
	 * 
	 * @param text
	 */
	public Tkn(String text) {
		assert text != null;
		this.text = text;
		start = -1;
		end = -1;
	}

	@Override
	public <T> boolean containsKey(Key<T> key) {
		return properties.containsKey(key);
	}

	/**
	 * equals only if text and properties match
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tkn other = (Tkn) obj;
		if ( ! text.equals(other.text))
			return false;
		if ( ! properties.equals(other.properties))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Key<T> key) {
		return (T) properties.get(key);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Collection<Key> getKeys() {
		return properties.keySet();
	}

	/**
	 * Convenience for accessing the POS tag, if set.
	 * 
	 * @return POS tag (format depends on who's doing the tagging), or null
	 */
	public String getPOS() {
		return get(POS);
	}

	/**
	 * The word (or whatever) this Token holds. Equivalent to
	 * {@link #toString()}.
	 */
	public String getText() {
		return text;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = properties.hashCode();
		result = prime * result + text.hashCode();
		return result;
	}

	@Override
	public boolean isTrue(Key<Boolean> key) {
		Boolean v = get(key);
		return v != null && v;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T put(Key<T> key, T value) {
		if (value == null)
			return (T) properties.remove(key);
		else
			return (T) properties.put(key, value);
	}

	/**
	 * Modify the text of this token.
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * The text
	 */
	@Override
	public String toString() {
		return text;
	}

	/**
	 * Uses {@link BrownCorpusTags#toCanon(String)} for the avoidance of accidental formatting mis-matches.
	 * @param tag
	 */
	public void setPOS(String tag) {
		put(Tkn.POS, BrownCorpusTags.toCanon(tag));
	}
}
