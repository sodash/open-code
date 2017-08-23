package com.winterwell.nlp.io;

import java.util.Collection;
import java.util.HashSet;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.Key;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Filter a stream, either ignoring all words from a set - e.g. stop words - or
 * ignoring all words that are outside that set.
 * 
 * @warning This is case sensitive! You probably want to lower-case both the
 *          filter-words & the token stream.
 * 
 * @author daniel
 * @testedby FilteredTokenStreamTest
 * @see ApplyFnToTokenStream which can use a function to filter
 */
public class FilteredTokenStream extends ATokenStream {

	public enum KInOut {
		EXCLUDE_THESE, ONLY_THESE
	}

	private final KInOut inOut;
	private final Collection<String> words;
	private Object filter;

	/**
	 * Pipe the stopwords through the tokeniser, thereby applying any processing that the pipeline does
	 * (e.g. lower-casing, canonicalisation) to them.
	 * @param stopwords
	 * @param include
	 * @param base
	 * @param processStopwords true to do the piping, false -- why are you using this constructor?
	 */
	public FilteredTokenStream(Collection<String> stopwords, KInOut include, ITokenStream base, boolean processStopwords) {
		this(process(stopwords, base, processStopwords), include, base);
	}
	
	private static Collection<String> process(Collection<String> stopwords, ITokenStream base, boolean processStopwords) {
		if ( ! processStopwords) return stopwords;
		HashSet<String> processed = new HashSet(stopwords.size());
		for (String sw : stopwords) {
			ITokenStream b2 = base.factory(sw);
			for (Tkn tkn : b2) {
				String procText = tkn.getText();
				if (Utils.isBlank(procText)) continue;
				if (procText.equals(sw)) {
					processed.add(sw);
				} else {
					processed.add(procText);
				}
			}
		}
		return processed;
	}

	public FilteredTokenStream(Collection<String> stopwords, KInOut include,
			ITokenStream input) {
		super(input);
		assert stopwords != null && include != null;
		assert input != null;
		this.words = stopwords;
		this.inOut = include;
		// Desc
		desc.put(new Key("inout"), inOut);
		// Add a small token which can do some good at 
		// "what are the words we're filtering against?"
		// without poking a whole dictionary into Desc
		desc.put(new Key("words.size"), words.size());
	}

	@Override
	public ITokenStream factory(String text) {
		return new FilteredTokenStream(words, inOut, base.factory(text));
	}

	@Override
	public boolean isFactory() {
		return true;
	}

	int skipped = 0;
	
	@Override
	protected Tkn processFromBase(Tkn original, AbstractIterator<Tkn> bit) {		
		boolean in = words.contains(original.getText());
		if (in) {
			// it is in the set -- return if that's what we wanted
			// (eg dictionary-words-only filtering)
			if (inOut == KInOut.ONLY_THESE)
				return original;
		} else {
			// it's not in the set -- return if that's what we wanted
			// (eg stopword filtering)
			if (inOut == KInOut.EXCLUDE_THESE)
				return original;
		}
		// try again (with some safety against infinite loops)
		skipped++;
		if (skipped > 10000000)
			throw new FailureException("long/infinite loop on " + this);
		return null;
	}
	

	@Override
	public String toString() {
		return getClass().getSimpleName() + " <- " + base;
	}

}
