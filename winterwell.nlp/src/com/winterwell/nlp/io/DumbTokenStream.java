/**
 * 
 */
package com.winterwell.nlp.io;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.winterwell.utils.containers.AbstractIterator;

import com.winterwell.depot.Desc;
import com.winterwell.depot.IHasDesc;

/**
 * Just wrap a String[].
 * TODO merge with {@link ListTokenStream}
 * @author daniel
 * @deprecated
 */
public final class DumbTokenStream extends ATokenStream {

	private final List<Tkn> tokens;

	public DumbTokenStream(String[] tokens) {
		this.tokens = new ArrayList();
		for (int i = 0; i < tokens.length; i++) {
			this.tokens.add(new Tkn(tokens[i]));
		}
	}
	
	/**
	 * Split on whitespace 
	 * @param string
	 */
	public DumbTokenStream(String string) {
		this(string.split("\\s+"));
	}

	public DumbTokenStream(List<Tkn> tokens) {
		this.tokens = tokens;
	}

	@Override
	public AbstractIterator<Tkn> iterator() {
		final Iterator<Tkn> it = tokens.iterator();
		return new AbstractIterator<Tkn>() {
			@Override
			protected Tkn next2() throws Exception {
				if (! it.hasNext()) return null;
				return it.next();
			}
		};
	}
	
	@Override
	public ITokenStream factory(String input) {
		throw new IllegalArgumentException();
	}

}
