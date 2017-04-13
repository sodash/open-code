package com.winterwell.nlp.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.stats.distributions.cond.Cntxt;
import com.winterwell.maths.stats.distributions.cond.ISitnStream;
import com.winterwell.maths.stats.distributions.cond.Sitn;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Wrap a Token stream as a Sitn stream
 * 
 * @author daniel
 * 
 */
public class Token2UnConditionalSitnStream implements ISitnStream<Tkn> {

	ITokenStream tokeniser;

	public Token2UnConditionalSitnStream(ITokenStream tokens) {
		this.tokeniser = tokens;
		assert tokens != null;
	}

	@Override
	public ISitnStream<Tkn> factory(Object sourceSpecifier) {
		String text;
		if (sourceSpecifier instanceof IDocument) {
			text = ((IDocument) sourceSpecifier).getContents();
		} else {
			text = (String) sourceSpecifier;
		}

		ITokenStream tokens = tokeniser.factory(text);
		return new Token2UnConditionalSitnStream(tokens);
	}

	@Override
	public String[] getContextSignature() {
		return Cntxt.EMPTY.getSignature();
	}

	@Override
	public Collection<Class> getFactoryTypes() {
		return (List) Arrays.asList(String.class, IDocument.class);
	}

	@Override
	public AbstractIterator<Sitn<Tkn>> iterator() {
		final Iterator<Tkn> it = tokeniser.iterator();
		return new AbstractIterator<Sitn<Tkn>>() {
			@Override
			protected Sitn<Tkn> next2() throws Exception {
				if ( ! it.hasNext())
					return null;
				Tkn token = it.next();
				return new Sitn<Tkn>(token, Cntxt.EMPTY);
			}
		};
	}

	@Override
	public Map<String, Object> getFixedFeatures() {
		return null;
	}

}
