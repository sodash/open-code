package com.winterwell.nlp.io;

import java.util.regex.Pattern;

import com.winterwell.utils.Key;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * A tokenizer that splits on a given regular expression. Matched expressions
 * are discarded.
 * 
 * @see WordAndPunctuationTokeniser for "standard" word tokenisation
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 */
public class RegexSplitTokenStream extends ATokenStream {

	public final static String WHITESPACE = "\\s+";
	public final static String WHITESPACE_PUNCTUATION = "(.,;:?!/\\s\\\\)+";

	private final String input;
	final Pattern regex;

	/**
	 * Construct a new tokenizer that splits on the specified regular expression
	 * e.g. \\s+
	 */
	public RegexSplitTokenStream(Pattern regex, String input) {
		this.regex = regex;
		this.input = input;
	}

	/**
	 * Construct a new tokenizer that splits on the specified regular expression
	 * e.g. \\s+
	 */
	public RegexSplitTokenStream(String regex, String input) {
		this.regex = Pattern.compile(regex);
		this.input = input;
		desc.put(new Key("rgx"), regex);
	}

	/**
	 * @see winterwell.nlp.io.ATokenStream#factory(java.lang.String)
	 */
	@Override
	public ITokenStream factory(String inpt) {
		return new RegexSplitTokenStream(this.regex, inpt);
	}

	@Override
	public AbstractIterator<Tkn> iterator() {
		return new AbstractIterator<Tkn>() {
			String toread = input;
			@Override
			protected Tkn next2() throws Exception {
				if (toread==null) return null;
				// TODO skip over empty matches (which would otherwise lead to an infinite loop)
				// Use a matcher, find() and an index into toread
				String[] bits = regex.split(toread, 2);
				toread = bits.length == 1 ? null : bits[1];
				return new Tkn(bits[0]);

			}			
		};
	}
	

}
