package com.winterwell.maths.stats.distributions.discrete;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Distribution over a finite set of possibilities. E.g. male/female or sentences-of-length-2
 * 
 * TODO Add forgotten/residue support. Perhaps merged with pseudo-count support??
 * There is a difference between prob-weight-assigned-to-stuff-we-saw-and-forgot
 * vs prob-weight-assigned-to-stuff-we-havent-seen -- but I think that the difference is in
 * the user's understanding.
 * 
 * @author daniel
 *
 * @param <X>
 */
public interface IFiniteDistribution<X> extends IDiscreteDistribution<X>,
		Iterable<X> {
	
	/**
	 * Provide a view of the distribution that implements the Map interface.
	 * For convenient plugging together with other code.
	 */
	Map<X,Double> asMap();

	/**
	 * @param n
	 * @return the n (or less) items with the highest probability. Ordering is
	 *         arbitrary for equal probs. Does not contain any zero-probability
	 *         items.
	 */
	List<X> getMostLikely(int n);

	/**
	 * An iterator over the elements of this distribution. Of course, these
	 * might be infinite, in which case this throws an exception.
	 */
	@Override
	public Iterator<X> iterator() throws UnsupportedOperationException;

	/**
	 * @return Number of elements (some of which may have zero probability) -1
	 *         for infinite.
	 */
	public int size();

	/**
	 * @return the un-normalised total probability weight. (i.e. divide by this
	 *         to get normalised probabilities). Can be zero
	 */
	double getTotalWeight();

}
