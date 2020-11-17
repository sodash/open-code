package com.winterwell.nlp.classifier;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.maths.datastorage.Vectoriser;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.Tkn;

/**
 * Convenience implementation of {@link Vectoriser} for {@link IDocument}. Just
 * uses a tokeniser to break up the doc.
 * 
 * @author daniel
 * @testedby  DocVectoriserTest}
 */
public class DocVectoriser extends Vectoriser<String, IDocument> {

	private ITokenStream tokeniser;

	public DocVectoriser(ITokenStream tokeniser, IIndex<String> index,
			KUnknownWordPolicy policy) {
		super(index, policy);
		this.tokeniser = tokeniser;
	}

	public ITokenStream getTokeniser() {
		return tokeniser;
	}

	public void setTokeniser(ITokenStream tokeniser) {
		this.tokeniser = tokeniser;
	}

	@Override
	protected Iterable<String> toBitStream(IDocument doc) {
		ITokenStream tokens = tokeniser.factory(doc.getContents());
		List<String> words = new ArrayList<String>();
		for (Tkn token : tokens) {
			words.add(token.getText());
		}
		return words;
	}

}
