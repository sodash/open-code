package com.winterwell.nlp.classifier;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.stats.distributions.cond.ExplnOfDist;
import com.winterwell.maths.stats.distributions.cond.ISitnStream;
import com.winterwell.maths.stats.distributions.cond.Sitn;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.utils.containers.Pair2;

public interface IStreamClassifier<Tok> extends ITextClassifier<String> {

	IFiniteDistribution<String> pClassify(IDocument text);

	/**
	 * 
	 * @param text
	 * @param tokenProbs
	 *            null or an empty list. If not null, the probabilities for each
	 *            Sitn (often each token) will be put into the list. This is
	 *            useful for seeing what the classifier is upto.
	 * @return the normalised probability of each model generating the text
	 *         (applying the prior).
	 */
	IFiniteDistribution<String> pClassify(IDocument text, ExplnOfDist tokenProbs);

	IFiniteDistribution<String> pClassify2(ISitnStream<Tok> stream, ExplnOfDist expln);
}