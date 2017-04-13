package com.winterwell.nlp.io;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.io.FileUtils;

/**
 * Reads from a stream, so it can handle large files better than
 * {@link WordAndPunctuationTokeniser}. The stream will be closed when finished.
 * 
 * @author daniel
 * 
 */
public class FastSimpleTokenStream extends ATokenStream {

	private Reader input;

	public FastSimpleTokenStream() {
		input = null;
	}

	public FastSimpleTokenStream(Reader input) {
		this.input = input;
	}

	public FastSimpleTokenStream(String input) {
		this(new StringReader(input));
	}

	@Override
	public FastSimpleTokenStream factory(String input) {
		return new FastSimpleTokenStream(input);
	}

	/**
	 * A just-in-case resource close.
	 */
	@Override
	protected void finalize() throws Throwable {
		FileUtils.close(input);
		super.finalize();
	}

	protected boolean isWordPart(char ch) {
		// return Character.isJavaIdentifierPart(ch);
		return Character.isLetterOrDigit(ch);
	}

	protected boolean isWordStart(char ch) {
		return Character.isJavaIdentifierStart(ch);
	}
	
	@Override
	public AbstractIterator<Tkn> iterator() {
		// allow re-run of the stream? 
		// No -- it gets in the way of close(input)		
		return new AbstractIterator<Tkn>() {
			@Override
			protected Tkn next2() throws Exception {
				try {
					return FastSimpleTokenStream.this.next3();
				} catch (IOException e) {						
					throw Utils.runtime(e);
				}
			}			
		};
	}

	private Tkn next3() throws IOException {
		String sb = next4();
		return new Tkn(sb);				
	}

	private String next4() throws IOException {
		if (input==null) return null;
		StringBuilder sb = new StringBuilder();
		while (true) {
			int c = input.read();
			if (c == -1) {
				FileUtils.close(input);
				input = null;
				break;
			}
			char ch = (char) c;

			// start a word?
			if (sb.length() == 0) {
				if (isWordStart(ch)) {
					sb.append(ch);
				}
				continue;
			}

			// continue a word
			if (isWordPart(ch)) {
				sb.append(ch);
			} else {
				// drop the char & return our word
				break;
			}			
		}
		// fail?
		if (sb.length() == 0)
			return null;
		return sb.toString();
	}

	public List<String> toStrings() {
		try {
			List words = new ArrayList();
			while(true) {
				String w = next4();
				if (w==null) break;
				words.add(w);
			}				
			return words;
		} catch(IOException ex) {
			throw Utils.runtime(ex);
		}
	}


}
