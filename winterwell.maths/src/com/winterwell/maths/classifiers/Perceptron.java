/**
 *
 */
package com.winterwell.maths.classifiers;

import java.util.Random;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.utils.Utils;

import no.uib.cipr.matrix.Vector;

/**
 * A one layer neural net. This learns a hyper-plane.
 * 
 * The class output is Boolean, but the training data can use other labels via
 * {@link #setTrueLabel(Object)}.
 * 
 * @author Daniel
 * @testedby {@link PerceptronTest}
 */
public class Perceptron extends AClassifier<Boolean> implements
		IClassifier<Boolean>, ITrainable.Supervised<Vector, Boolean> {

	private static final int MAX_TRAINING_ITERATIONS = 25;

	double bias;

	Random r = Utils.getRandom();

	private Object trueLabel = Boolean.TRUE;
	Vector weights;

	public Perceptron(int dim) {
		weights = DataUtils.newVector(dim);
		resetup();
	}

	@Override
	public boolean canPClassify() {
		return false;
	}

	@Override
	public Boolean classify(Vector x) {
		double v = x.dot(weights) + bias;
		return v > 0;
	}

	@Override
	public void finishTraining() {
		for (int i = 0; i < MAX_TRAINING_ITERATIONS; i++) {
			// TODO test for convergence and exit early
			for (Vector x : trainingData) {
				Boolean pc = classify(x);
				boolean tc = ((Datum) x).isLabelled(trueLabel);
				if (pc == tc) {
					continue;
				}
				// adjust weights
				if (tc) {
					weights.add(x);
					bias++;
				} else {
					weights.add(-1, x);
					bias--;
				}
			}
		}
		super.finishTraining();
	}

	@Override
	public int getDim() {
		return weights.size();
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public IDiscreteDistribution<Boolean> pClassify(Vector x)
			throws UnsupportedOperationException {
		// we could treat activation levels as probabilities
		// but they aren't really
		throw new UnsupportedOperationException();
	}

	@Override
	public void resetup() {
		for (int i = 0, n = getDim(); i < n; i++) {
			// uniform in [-1,1]
			double v = 2 * r.nextDouble() - 1;
			weights.set(i, v);
		}
		bias = 2 * r.nextDouble() - 1;
	}

	/**
	 * The value that is considered to be true. All other values are considered
	 * to be false; This is the Boolean true by default. It can be set to allow
	 * the Perceptron to be used with non-boolean labelled Datums. E.g. you
	 * might set this to a particular string if the Datums labels are strings.
	 * 
	 * @param label
	 */
	public void setTrueLabel(Object label) {
		trueLabel = label;
	}

	@Override
	public void train(Iterable<? extends Vector> data) {
		super.train(data);
	}

	@Override
	public void train1(Vector x) {
		super.train1(x);
	}

	@Override
	public void train1(Vector x, Boolean tag, double weight) {
		super.train1(x, tag, weight);
	}
}
