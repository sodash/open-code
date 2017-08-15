package com.winterwell.maths.classifiers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;

import no.uib.cipr.matrix.Vector;

/**
 * Classify using separate models for each class, and picking the model with the
 * best likelihood.
 * <p>
 * Training depends on the underlying models supporting it.
 * 
 * @param <X>
 *            tag-type
 * 
 * @author daniel
 * 
 */
public class BayesModelSelection<X> extends AClassifier<X> implements
		IClassifier<X>, ITrainable.Supervised<Vector, X> {

	Map<X, IDistribution> models = new ArrayMap<X, IDistribution>();
	ObjectDistribution<X> prior = new ObjectDistribution<X>();

	public BayesModelSelection() {
		//
	}

	@Override
	public boolean canPClassify() {
		return true;
	}

	@Override
	public X classify(Vector x) {
		// Pick the most likely
		return pClassify(x).getMostLikely();
	}

	@Override
	public void finishTraining() {
		for (IDistribution m : models.values()) {
			((ITrainable) m).resetup();
		}
	}

	@Override
	public int getDim() {
		return models.values().iterator().next().getDim();
	}

	public Map<X, IDistribution> getModels() {
		return Collections.unmodifiableMap(models);
	}

	@Override
	public boolean isReady() {
		for (IDistribution m : models.values()) {
			if (m instanceof ITrainable) {
				if (!((ITrainable) m).isReady())
					return false;
			}
		}
		return true;
	}

	public void normalise() {
		prior.normalise();
	}

	@Override
	public ObjectDistribution<X> pClassify(Vector x) {
		assert DataUtils.isFinite(x) : x;
		assert prior.size() != 0;
		ObjectDistribution<X> posterior = new ObjectDistribution<X>();
		double total = 0;
		for (X k : models.keySet()) {
			IDistribution model = models.get(k);
			double p = model.density(x);
			total += p;
			posterior.setProb(k, p * prior.prob(k));
		}
		// No prob at all?! pick the prior
		if (total == 0)
			return prior.copy();
		return posterior;
	}

	@Override
	public void resetup() {
		for (IDistribution m : models.values()) {
			((ITrainable) m).resetup();
		}
	}

	// normalisation could get confused
	public void setModel(X label, IDistribution model, double prior) {
		this.prior.setProb(label, prior);
		models.put(label, model);
		assert model.getDim() == getDim();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + Printer.toString(models)
				+ " " + Printer.toString(prior);
	}

	@Override
	public void train(Iterable<? extends Vector> x)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void train1(Vector x) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void train1(Vector x, X tag, double weight) {
		prior.addProb(tag, weight);
		IDistribution m = models.get(tag);
		if (weight != 1 && m instanceof ITrainable.Unsupervised.Weighted) {
			((ITrainable.Unsupervised.Weighted) m).train(new double[]{weight}, Arrays.asList(x));
		} else {
			((ITrainable.Unsupervised<Vector>) m).train1(x);
		}
	}
}
