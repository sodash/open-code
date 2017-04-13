/**
 * 
 */
package com.winterwell.nlp.io;

import java.util.Locale;

import com.winterwell.utils.containers.AbstractIterator;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class LowercaseTokenStream extends ATokenStream {


	public LowercaseTokenStream(ITokenStream base) {
		super(base);
	}

	/**
	 * @see winterwell.nlp.io.ATokenStream#factory(java.lang.String)
	 */
	@Override
	public ITokenStream factory(String input) {
		return new LowercaseTokenStream(base.factory(input));
	}

	@Override
	public boolean isFactory() {
		return true;
	}

	@Override
	protected Tkn processFromBase(Tkn next, AbstractIterator<Tkn> bit) {
		next.setText(next.getText().toLowerCase(Locale.ENGLISH));
		return next;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " <- " + base;
	}

}
