package com.winterwell.maths.classifiers;

import java.util.List;

import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;

import no.uib.cipr.matrix.Vector;

/**
 * A common interface for vector-based classifiers.
 * For Object-based classifiers, use an {@link IFiniteDistribution}
 * 
 * @author daniel
 * @see ITextClassifier
 * @param <X>
 */
public interface IClassifier<X> {

	/**
	 * @return true if the pClassify method is supported. This slightly baroque
	 *         mechanism is used as more convenient and less ugly than having a
	 *         second extension interface and doing instanceof tests.
	 */
	boolean canPClassify();

	/**
	 * May return null e.g. if the classifier has not been trained
	 */
	X classify(Vector x);

	/**
	 * May include nulls if the classifier is unsure.
	 * <p>
	 * The base method is just to apply {@link #classify(Vector)} repeatedly,
	 * but subclasses can use smarter methods, e.g. viterbi sequencing in Markov
	 * chains.
	 */
	@Deprecated
	List<X> classifySeqn(List<? extends Vector> seqn);

	/**
	 * @return the dimension for input vectors
	 */
	int getDim();

	/**
	 * Probabilistic classification
	 * 
	 * @param x
	 *            All elements must be finite
	 * @return a distribution over the classes
	 */
	public IDiscreteDistribution<X> pClassify(Vector x)
			throws UnsupportedOperationException;

}
