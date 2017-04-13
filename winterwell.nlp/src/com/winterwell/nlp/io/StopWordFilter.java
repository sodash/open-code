/**
 * 
 */
package com.winterwell.nlp.io;

import com.winterwell.nlp.NLPWorkshop;

/**
 * Uses {@link NLPWorkshop#getStopwords()} and {@link FilteredTokenStream}
 * 
 * @warning is case sensitive!
 * 
 * @author daniel
 * 
 */
public class StopWordFilter extends FilteredTokenStream {

	public StopWordFilter(ITokenStream input) {
		super(NLPWorkshop.get().getStopwords(), KInOut.EXCLUDE_THESE, input);
	}

}
