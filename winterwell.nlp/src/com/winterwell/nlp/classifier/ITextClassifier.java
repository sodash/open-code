package com.winterwell.nlp.classifier;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.classifiers.IClassifier;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.nlp.corpus.IDocument;

/**
 * A text cousin to {@link IClassifier}.
 * <p>
 * Why two separate interfaces (and classes)? 'cos this one can bind together
 * string based processing (e.g. tokenisation, stemming, etc.) with mathematical
 * models.
 * 
 * @author daniel
 * 
 * @param <X>
 *            the tag class
 */
public interface ITextClassifier<X> extends ITrainable.Supervised<IDocument, X> {

	/**
	 * Classify the supplied string. May return null e.g. if the classifier has
	 * not been trained.
	 */
	X classify(IDocument text);

	/**
	 * Probabilistic classification
	 * 
	 * @param x
	 * @return a distribution over the classes. This does not need to be
	 *         normalised! Can be empty, never null.
	 */
	IFiniteDistribution<X> pClassify(IDocument text)
			throws UnsupportedOperationException;
}
