package com.winterwell.maths.classifiers;

import java.util.Arrays;

import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * TODO Simple hyperplane learner.
 * 
 * 
 * @author daniel
 * 
 */
public class Winnow extends AClassifier<Boolean> implements
		IClassifier<Boolean> {

	private final double alpha = 2;
	private double threshold = 0.5;

	private final double[] w;

	public Winnow(int dim) {
		threshold = dim / 2;
		w = new double[dim];
		Arrays.fill(w, 1);
	}

	@Override
	public boolean canPClassify() {
		return false;
	}

	@Override
	public Boolean classify(Vector x) {
		double v = new DenseVector(w, false).dot(x);
		return v > threshold;
	}

	@Override
	public int getDim() {
		return w.length;
	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public ObjectDistribution<Boolean> pClassify(Vector x)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resetup() {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	public void train1(Datum x) {
		// The update rule is (loosely):
		//
		Boolean target = (Boolean) x.getLabel();
		if (target == null)
			return;
		Boolean output = classify(x);
		// * If an example is correctly classified, do nothing
		if (Utils.equals(target, output))
			return;
		// * If an example is predicted to be 1 but the correct result was 0,
		// all of the weights involved in the mistake are multiplied by 1/alpha
		// (demotion step).
		// * If an example is predicted to be 0 but the correct result was 1,
		// all of the weights not involved in the mistake are multiplied by
		// alpha (promotion step).
		double m = target ? 1 / alpha : alpha;
		for (VectorEntry ve : x) {
			if (ve.get() == 0) {
				continue;
			}
			w[ve.index()] = w[ve.index()] * m;
		}
	}
}
