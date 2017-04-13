package com.winterwell.maths.classifiers;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import com.winterwell.maths.timeseries.DataUtils;

import no.uib.cipr.matrix.Vector;

/**
 * Naive k-Nearest Neighbour algorithm. Iterates over the whole of the training
 * data, finds the k nearest neighbours, then takes the most common
 * classification among said nearest neighbours, weighted by the inverse of each
 * distance.
 * 
 * This will perform horribly for large training sets.
 * 
 * @author miles
 * 
 * @param <X>
 */
public class KNearestNeighbours<X> extends AKNearestNeighbours<Vector, X>
		implements IClassifier<X>, ITrainable.Supervised<Vector, X> {

	public KNearestNeighbours(int k) {
		super(k);
	}

	@Override
	public boolean canPClassify() {
		return false;
	}

	@Override
	public X classify(Vector x) {
		return super.classify(x);
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
	protected double distance(Vector x, Vector y) {
		return DataUtils.dist(x, y);
	}

	@Override
	public int getDim() {
		return trainingData.get(0).size();
	}

	@Override
	public IDiscreteDistribution<X> pClassify(Vector x)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
