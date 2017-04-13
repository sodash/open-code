package com.winterwell.nlp.docmodels;

import com.winterwell.utils.MathUtils;

import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.IDocument;

/**
 * Weighted sum of several models. Can DiscreteMixtureModel replace this?
 * 
 * @author daniel
 * 
 */
public class MixtureDocModel extends ADocModel {

	/**
	 * You could view this as a convenient class for doing normalised
	 * weightings. Or you could view the meta-model as a probabilistic OR.
	 */
	ObjectDistribution<ADocModel> weights = new ObjectDistribution<ADocModel>();

	/**
	 * Note: *does* call init()
	 * 
	 * @param models
	 */
	public MixtureDocModel(ADocModel... models) {
		super(null, null);
		for (ADocModel aDocModel : models) {
			weights.setProb(aDocModel, 1);
		}
		weights.normalise();
		init();
	}

	@Override
	public void finishTraining() {
		for (ADocModel m : weights) {
			m.finishTraining();
		}
		weights.normalise();
	}

	/**
	 * Weights for the models. Adjusting these will adjust the model. You could
	 * view this as a convenient class for doing normalised weightings. Or you
	 * could view the mixture-model as a probabilistic OR.
	 */
	public ObjectDistribution<ADocModel> getWeights() {
		return weights;
	}

	@Override
	public IIndex<String> getWordIndex() {
		IIndex<String> index = null;
		for (ADocModel aDocModel : weights) {
			if (index == null) {
				index = aDocModel.getWordIndex();
			} else if (!index.equals(aDocModel.getWordIndex()))
				throw new IllegalStateException("Indexes do not match");
		}
		assert index != null;
		return index;
	}

	@Override
	public boolean isReady() {
		for (ADocModel m : weights) {
			if (!m.isReady())
				return false;
		}
		return weights.isNormalised();
	}

	@Override
	public double prob(IDocument x) {
		assert x != null;
		double p = 0;
		for (ADocModel model : weights) {
			double mp = model.prob(x);
			assert MathUtils.isFinite(mp) : mp + "	" + this;
			double weight = weights.prob(model);
			assert MathUtils.isFinite(weight) : weight + "	" + this;
			p += weight * mp;
		}
		return p;
	}

	@Override
	public void pruneToIndex() {
		for (ADocModel m : weights) {
			m.pruneToIndex();
		}
	}

	@Override
	public void resetup() {
		for (ADocModel m : weights) {
			m.resetup();
		}
		super.resetup();
	}

	@Override
	public IDocument sample() {
		return weights.sample().sample();
	}

	@Override
	public String toString() {
		return "MixtureDocModel [weights=" + weights + "]";
	}

	@Override
	public void train1(IDocument x) throws UnsupportedOperationException {
		// does not alter the weights!
		for (ADocModel m : weights) {
			m.train1(x);
		}
		trainingCount++;
	}

}
