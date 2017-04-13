package com.winterwell.maths.classifiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;

/**
 * Status: NeuralNet is broken! Wrap a feed forward neural net as a classifier
 * 
 * @author daniel
 * 
 * @param <X>
 */
public class NeuralNetClassifier<X> implements IClassifier<X>,
		ITrainable.Supervised<Vector, X> {

	final NeuralNet net;
	final List<X> values;

	public NeuralNetClassifier(int dimIn, Collection<? extends X> values) {
		net = new NeuralNet(dimIn, values.size());
		this.values = new ArrayList<X>(values);
	}

	@Override
	public boolean canPClassify() {
		return false;
	}

	@Override
	public X classify(Vector x) {
		Vector out = net.apply(x);
		double bestV = Double.NEGATIVE_INFINITY;
		int bestI = -1;
		for (int i = 0; i < out.size(); i++) {
			double v = out.get(i);
			if (v > bestV) {
				bestV = v;
				bestI = i;
			}
		}
		return values.get(bestI);
	}

	@Override
	public List<X> classifySeqn(List<? extends Vector> seqn) {
		List<X> tags = new ArrayList<X>(seqn.size());
		for (Vector xi : seqn) {
			X ti = classify(xi);
			tags.add(ti);
		}
		return tags;
	}

	@Override
	public void finishTraining() {
		net.finishTraining();
	}

	@Override
	public int getDim() {
		return net.layers[0].numInputs();
	}

	@Override
	public boolean isReady() {
		return net.isReady();
	}

	@Override
	public IDiscreteDistribution<X> pClassify(Vector x)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resetup() {
		net.resetup();
	}

	@Override
	public void train1(Vector x, X tag, double weight) {
		DenseVector output = new DenseVector(values.size());
		int i = values.indexOf(tag);
		assert i != -1;
		output.set(i, 1);
		net.train1(x, output, weight);
	}

}
