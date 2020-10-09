package com.winterwell.nlp.io;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Split urls (as marked by {@link WordAndPunctuationTokeniser#POS_URL} into
 * host, path tokens.
 * 
 * TODO should we split more? e.g. http://porn.co.uk/twins/123b.jpg into http,
 * porn, co.uk, twins, 123b, jpg
 * 
 * TODO should we resolve urls, e.g. to deal with bit.ly opaqueness?
 * 
 * TODO unclear how much this helps
 * 
 * @author daniel
 * @testedby  UrlBreakerTest}
 */
public class UrlBreaker extends ATokenStream {

	final ArrayList<Tkn> buffer = new ArrayList<Tkn>();

	public UrlBreaker(ITokenStream base) {
		super(base);
		assert base != null;
	}

	@Override
	public ITokenStream factory(String input) {
		return new UrlBreaker(base.factory(input));
	}
	
	@Override
	public AbstractIterator<Tkn> iterator() {
		final Iterator<Tkn> bit = base.iterator();
		assert bit != null;
		return new AbstractIterator<Tkn>() {			
			@Override
			protected Tkn next2() throws Exception {
				return next3(bit);
			}
		};
	}

	protected Tkn next3(Iterator<Tkn> bit) {
		if ( ! buffer.isEmpty()) {
			Tkn tok = buffer.remove(buffer.size() - 1);
			return tok;
		}

		if ( ! bit.hasNext())
			return null;

		Tkn token = bit.next();

		String pos = token.getPOS();
		if ( ! WordAndPunctuationTokeniser.POS_URL.equals(pos)) {
			return token;
		}
		try {
			// What to do?
			// - split on [._-/] into many words?
			// - split into host / path / query?
			// - resolve redirects first?
			// - just keep the host, sod the rest?

			// Splitting into host / path for now!
			URI uri = new URI(token.getText());
			String host = uri.getHost();
			String path = uri.getRawPath();
			
			// a bad url, e.g. http://www.blah
			if (host == null)
				return token;
			// the host token (with positional data if we can)
			int s1 = token.start + token.getText().indexOf(host);
			Tkn t1 = token.start == -1? new Tkn(host)
						: new Tkn(host, s1, s1+host.length());
			t1.setPOS(WordAndPunctuationTokeniser.POS_URL);
			// put a path token into the buffer
			if ( ! Utils.isBlank(path)) {
				// positional data if we can
				int s2 = token.getText().indexOf(path);
				assert s2 != -1 : path+" not in "+token.getText();
				Tkn t2 = token.start == -1? new Tkn(path)
						: new Tkn(path, token.start+s2, token.start+s2+path.length());
				t2.setPOS(WordAndPunctuationTokeniser.POS_URL);
				buffer.add(t2);
			}
			return t1;
		} catch (URISyntaxException e) {
			// oh well - badly formed
			return token;
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " <- " + base;
	}

}
