/**
 * 
 */
package com.winterwell.nlp.io;

import java.util.Set;
import java.util.regex.Pattern;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.ArraySet;

/**
 * Handle "no good" as a single token.
 * If POS data is available, only applies to adjective (JJ) tokens.
 * 
 * TODO Better -- chunk into phrases, then look for negations in an adjective phrase.
 * 
 * @author daniel
 * @testedby NoGoodTokeniserTest
 */
public class NoGoodTokeniser extends ATokenStream {

	public NoGoodTokeniser(ITokenStream _tokeniser) {
		super(_tokeniser);
	}
	
	@Override
	public ITokenStream factory(String input) {
		ITokenStream base2 = base.factory(input);
		return new NoGoodTokeniser(base2);
	}

	Set<String> negs = new ArraySet("no", "nae", "not", "never");
	Set<String> boosts = new ArraySet("very");
	
	static final Pattern AZ = Pattern.compile("[a-z]");
	
	@Override
	protected Tkn processFromBase(Tkn original, AbstractIterator<Tkn> bit) {
		// TODO need to refactor if this is going to be more general / ambitious -- eg handle Adj-phrases
		if (boosts.contains(original.getText())) {
			// TODO
		}
		if ( ! negs.contains(original.getText())) {
			return original;
		}
		Tkn peek = bit.peekNext();
		if (peek==null) return original;	
		// HACK: not a good
		if (peek.getText().equals("a") || peek.getText().equals("the")) {
			bit.next(); // discard!
			peek = bit.peekNext();
			if (peek==null) return original;
		}
		
		// ignore punctuation
		if ( ! AZ.matcher(original.getText()).find()) {
			return original;
		}			
		String pos = original.getPOS();
		if (pos!=null && ! pos.toUpperCase().startsWith("JJ")) {
			// not an adjective
			return original; 
		}
		
		// join them!
		Tkn tkn = bit.next();
		tkn.setText("no-"+tkn.getText());
		return tkn;
	}
	
}
