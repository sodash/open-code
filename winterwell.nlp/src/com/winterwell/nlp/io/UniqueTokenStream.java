package com.winterwell.nlp.io;

import java.util.HashSet;
import java.util.Set;

import com.winterwell.utils.containers.AbstractIterator;

/**
 * Extract the unique items from a token stream. Beware! This is a stream of
 * unique *tokens* (strings + property bags) rather than unique strings.
 * Calling {@link #factory(String)} will give a "fresh" copy, with no previous memory.
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class UniqueTokenStream extends ATokenStream implements ITokenStream {

	Set<Tkn> tokenSeen = new HashSet<Tkn>();
	
	public UniqueTokenStream(ITokenStream base) {
		super(base);
	}

	/**
	 * @see winterwell.nlp.io.ATokenStream#factory(java.lang.String)
	 */
	@Override
	public ITokenStream factory(String input) {
		return new UniqueTokenStream(base.factory(input));
	}

	
	@Override
	protected Tkn processFromBase(Tkn original, AbstractIterator<Tkn> bit) {
		boolean newToken = tokenSeen.add(original);
		if (newToken) {
			return original;
		}
		return null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " <- " + base;
	}
}
