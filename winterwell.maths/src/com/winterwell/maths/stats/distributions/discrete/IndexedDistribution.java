package com.winterwell.maths.stats.distributions.discrete;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.maths.datastorage.IPruneListener;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.log.Log;

import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Vector.Norm;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * A distribution over an indexed set of elements using a SparseVector. Indexes
 * can be reused between such distributions, which when combined with
 * vector-based work should be quite efficient.
 * 
 * @author daniel
 * @testedby  IndexedDistributionTest} TODO speed tests versus
 *           {@link ObjectDistribution}
 * @param <T>
 */
public final class IndexedDistribution<T> extends AFiniteDistribution<T>
		implements IPruneListener, Serializable {

	private class IndexedDistIterator extends AbstractIterator<T> {
		final Iterator<VectorEntry> it = probs.iterator();

		@Override
		public T next2() {
			while (it.hasNext()) {
				VectorEntry n = it.next();
				int i = n.index();
				T obj = index.get(i);
				if (obj != null)
					return obj;
			}
			return null;
		}
	}

	private static final long serialVersionUID = 1L;

	private final IIndex<T> index;

	// could allow the user to set class of vector
	// TODO: use VectorDistribution to share code
	// TODO: SparseVector isn't serializable?!?
	private final Vector probs = new SparseVector(Integer.MAX_VALUE);

	public IndexedDistribution(IIndex<T> index) {
		assert index != null;
		this.index = index;
		index.addListener(this);
	}

	public void add(double alpha, IndexedDistribution<T> dist) {
		if (alpha == 0)
			return;
		assert alpha > 0 : alpha;
		if (dist.index.equals(index)) {
			probs.add(alpha, dist.probs);
			cachedWeight += alpha * dist.getTotalWeight();
			return;
		}
		for (T term : dist) {
			assert term != null;
			double op = alpha * dist.prob(term);
			int i = index.indexOfWithAdd(term);
			probs.add(i, op);
			cachedWeight += op;
		}
	}

	@Override
	public void addProb(T x, double dp) {
		if (dp == 0)
			return;
		normalised = false;
		int i = index.indexOfWithAdd(x);
		probs.add(i, dp);
		// double old = probs.get(i);
		// double np = old+dp;
		// probs.set(i, np);
		cachedWeight += dp;
	}

	/**
	 * Convenience for +1 to the frequency/unnormalised-prob of this element.
	 * The element is added to the index if necessary.
	 * 
	 * @param text
	 */
	public void count(T x) {
		addProb(x, 1);
	}

	public IIndex<T> getIndex() {
		return index;
	}

	/**
	 * @param n
	 * @return The n most likely elements in this distribution. Excludes zeroes
	 *         (if that's all there is).
	 * @testedby  IndexedDistributionTest#testGetMostLikely()}
	 */
	@Override
	public List<T> getMostLikely(int n) {
		List<Integer> is = DataUtils.getSortedIndices(probs, n);
		ArrayList<T> ts = new ArrayList<T>(is.size());
		for (Integer integer : is) {
			// exclude zeroes
			double p = probs.get(integer);
			if (p == 0.0) {
				continue;
			}
			T t = index.get(integer);
			if (t == null) {
				// assert t != null : integer+" from "+index;
				// this can happen with pruned indexex - but it's kind of
				// worrying!
				continue;
			}
			ts.add(t);
		}
		return ts;
	}

	/**
	 * @return the un-normalised total probability weight. (i.e. divide by this
	 *         to get normalised probabilities). Can be zero
	 */
	@Override
	public double getTotalWeight() {
		// this is a slow method - so cache the value
		if (cachedWeight > 0)
			return cachedWeight;
		cachedWeight = probs.norm(Norm.One);
		return cachedWeight;
	}

	/**
	 * Do NOT edit this!
	 * 
	 * @return
	 */
	public Vector getVector() {
		return probs;
	}

	/**
	 * @testedby  IndexedDistributionTest#testIterator()}
	 */
	@Override
	public final Iterator<T> iterator() {
		// use Vector.iterator() for efficiency in
		// the small distribution / large index case
		return new IndexedDistIterator();
	}

	@Override
	public void normalise() throws UnsupportedOperationException {
		double w = getTotalWeight();
		if (w == 1) {
			// Do nothing
		} else if (w == 0) {
			Log.w("ai.train", "Normalising zero vector!");
			// Return uniform vector?
		} else {
			for (VectorEntry ve : probs) {
				double p = ve.get();
				double np = p / w;
				ve.set(np);
			}
		}
		cachedWeight = 1;
		normalised = true;
	}

	@Override
	public double normProb(T x) {
		return prob(x) / getTotalWeight();
	}

	@Override
	public double prob(T x) {
		int i = index.indexOf(x);
		return probFromIndex(i);
	}
	
	public double probFromIndex(int i) {
		if (i == -1)
			return 0;
		double pi = probs.get(i);
		return pi;
	}

	@Override
	public void pruneEvent(List pruned) {
		for (Object p : pruned) {
			int i = (Integer) p;
			probs.set(i, 0);
		}
		if (probs instanceof SparseVector) {
			((SparseVector) probs).compact();
		}
		recalcTotalWeight();
	}

	public void pruneToIndex() {
		for (VectorEntry ve : probs) {
			if (index.get(ve.index()) != null) {
				continue;
			}
			ve.set(0);
		}
		if (probs instanceof SparseVector) {
			((SparseVector) probs).compact();
		}
		recalcTotalWeight();
	}

	@Override
	public void setProb(T x, double p) {
		normalised = false;
		int i = index.indexOfWithAdd(x);
		double old = probs.get(i);
		probs.set(i, p);
		cachedWeight += p - old;
	}

	/**
	 * This will try to return the number of non-zero probs. But depending on
	 * the underlying class of Vector used, it can fall back to the size of the
	 * index - which can be large and many or all of which may have
	 * zero-probability.
	 */
	@Override
	public int size() {
		if (probs instanceof SparseVector)
			return ((SparseVector) probs).getUsed();
		return index.size();
	}

	@Override
	public String toString() {
		T ml = getMostLikely();
		if (ml == null)
			return "[untrained]";
		int p = (int) (prob(ml) * 100);
		return getClass().getSimpleName() + "[most-likely=" + ml + " @" + p
				+ "%]";
	}

	

}
