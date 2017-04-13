package com.winterwell.maths.stats.distributions.discrete;

import java.util.Iterator;

import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * Wrap a vector as a distribution
 * 
 * @author daniel
 * 
 */
public final class VectorDistribution extends AFiniteDistribution<Integer> {

	private Vector probs;

	/**
	 * 
	 * @param probs
	 *            This can get edited
	 */
	public VectorDistribution(Vector probs) {
		assert probs != null;
		this.probs = probs;
	}

	@Override
	public void addProb(Integer i, double dp) {
		if (dp == 0)
			return;
		normalised = false;
		double old = probs.get(i);
		double np = old + dp;
		probs.set(i, np);
		cachedWeight += dp;
	}


	/**
	 * If you edit this, call {@link #recalcTotalWeight()}.
	 */
	public Vector getVector() {
		return probs;
	}

	@Override
	public Iterator<Integer> iterator() throws UnsupportedOperationException {
		final Iterator<VectorEntry> it = probs.iterator();
		return new Iterator<Integer>() {
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Integer next() {
				int i = it.next().index();
				return i;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public void normalise() throws UnsupportedOperationException {
		double w = getTotalWeight();
		if (w == 1)
			return;
		for (VectorEntry ve : probs) {
			double p = ve.get();
			double np = p / w;
			ve.set(np);
		}
		cachedWeight = 1;
		normalised = true;
	}

	@Override
	public double normProb(Integer x) {
		double w = getTotalWeight();
		double p = prob(x);
		return p / w;
	}

	@Override
	public double prob(Integer x) {
		if (x < 0 || x >= probs.size())
			return 0;
		return probs.get(x);
	}

	@Override
	public void setProb(Integer i, double p) {
		normalised = false;
		double old = probs.get(i);
		probs.set(i, p);
		cachedWeight += p - old;
	}

	@Override
	public int size() {
		return probs.size();
	}

}
