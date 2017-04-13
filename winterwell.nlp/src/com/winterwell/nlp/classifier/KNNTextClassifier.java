package com.winterwell.nlp.classifier;

import com.winterwell.maths.classifiers.AKNearestNeighbours;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.nlp.similarity.ICompareText;

/**
 * k-nearest neighbours text classifier. Takes the value of k, some training
 * data and an ICompareText as a constructor parameter.
 * 
 * @author miles
 * 
 * @param <X>
 *            The type of sets into which the texts are classified.
 */
public class KNNTextClassifier<X> extends AKNearestNeighbours<IDocument, X>
		implements ITextClassifier<X> {

	@Override
	public void resetup() {
		super.resetup();
	}
	private final ICompareText comparator;

	public KNNTextClassifier(int k, ICompareText comparator) {
		super(k);
		this.comparator = comparator;
	}

	public X classify(String text) {
		return classify(new SimpleDocument(null, text, null));
	}

	@Override
	protected double distance(IDocument x, IDocument y) {
		return 1 - comparator.similarity(x.getContents(), y.getContents());
	}

	@Override
	public IFiniteDistribution<X> pClassify(IDocument text)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
