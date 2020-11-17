package com.winterwell.nlp.io;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Chop text into sentences.
 * 
 * @testedby  SentenceStreamTest}
 * @author daniel
 * 
 */
public class SentenceStream extends ATokenStream {

	private String input;

	public SentenceStream() {
	}

	Pattern rx = Pattern.compile("(\\S.+?[.!?])(?=\\s+|$)");
	
	public SentenceStream(String input) {
		this.input = input;	
	}

	@Override
	public ITokenStream factory(String inpt) {
		return new SentenceStream(inpt);
	}

	@Override
	public AbstractIterator<Tkn> iterator() {
		return new AbstractIterator<Tkn>() {	

			private Matcher m = rx.matcher(input);
			int last = 0;
			
			@Override
			protected Tkn next2() throws Exception {
				if (m == null)
					return null;
				if ( ! m.find()) {
					m = null;
					String s = input.substring(last);
					return Utils.isBlank(s) ? null : new Tkn(s);
				}
				String s = m.group().trim();
				last = m.end();
				return new Tkn(s);			
			}
		};
	}	

}
