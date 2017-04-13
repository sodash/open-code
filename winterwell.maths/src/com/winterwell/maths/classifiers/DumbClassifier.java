package com.winterwell.maths.classifiers;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.maths.timeseries.Datum;

import no.uib.cipr.matrix.Vector;

/**
 * A classifier which just selects most-likely (default) or randomly based on their frequency, in
 * the training data. Useful as a baseline for comparison.
 * <p>
 * Note: This is NOT the best dumb classifier. To get the best dumb classifier,
 * do 
 * 
 * @author daniel
 * 
 */
public final class DumbClassifier<X> extends AClassifier<X> implements
		IClassifier<X>, ITrainable.Supervised<Vector, X> {

	private final int dim;

	private final ObjectDistribution<X> histogram = new ObjectDistribution<X>();

	public DumbClassifier(int dim) {
		this.dim = dim;
	}

	@Override
	public boolean canPClassify() {
		return true;
	}
	
	boolean rnd;
	
	public void setRandomSelection(boolean rnd) {
		this.rnd = rnd;
	}
	
	/**
	 * @return the most likely item from the distribution.
	 * Or if {@link #setRandomSelection(boolean)} is true, a random choice
	 * based on frequency of training data.
	 */
	@Override
	public X classify(Vector x) {
		return rnd? histogram.sample() : histogram.getMostLikely();
	}

	@Override
	public void finishTraining() {
		histogram.normalise();
		super.finishTraining();
	}

	@Override
	public int getDim() {
		return dim;
	}

	@Override
	public boolean isReady() {
		return ! histogram.isEmpty();
	}

	@Override
	public IDiscreteDistribution<X> pClassify(Vector x) {
		return histogram;
	}

	@Override
	public void resetup() {
		histogram.resetup();
	}

	@Override
	public String toString() {
		return DumbClassifier.class.getSimpleName();
	}

	/**
	 * Convenience for training from Datums whose labels are used as the tags for
	 * {@link #train1(Vector, Object)}
	 */
	@Override
	public void train(Iterable<? extends Vector> data) {
		super.train(data);
	}

	/**
	 * Convenience for training from Datums whose labels are used as the tags for
	 * {@link #train1(Vector, Object)}
	 */
	@Override
	public void train1(Vector x) {
		if (x instanceof Datum) {
			Object tag = ((Datum) x).getLabel();
			train1(x, (X) tag, 1);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void train1(Vector ignored, X tag, double weight) {
		histogram.addProb(tag, weight);
	}
}
