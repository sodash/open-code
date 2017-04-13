/**
 * 
 */
package com.winterwell.nlp.io;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.web.WebUtils2;

/**
 * This is a wrapper, which first strips out any xml/html tags, then passes down to the
 * child tokeniser.
 * So it works the opposite way round to most tokenisers with a base.
 * 
 * It has the significant downside that Tkn.start / end will not match up with the original String :(
 * TODO So maybe have ignore-tags as an option in {@link WordAndPunctuationTokeniser}??
 * 
 * @author daniel
 */
public class StripXmlTokeniser extends ATokenStream {

	public StripXmlTokeniser(ITokenStream base) {
		super(base);
	}
	
	@Override
	public String toString() {
		return "StripXml -> " + base;
	}

	@Override
	public AbstractIterator<Tkn> iterator() {
		// the stripping work is already done by factory()
		return base.iterator();
	}
	
	@Override
	public ITokenStream factory(String input) {
		String input2 = WebUtils2.getPlainText(input);
		ITokenStream base2 = base.factory(input2);
		return new StripXmlTokeniser(base2);
	}

}
