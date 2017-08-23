package com.winterwell.nlp.classifier;

import java.util.HashMap;
import java.util.Map;

import com.winterwell.maths.classifiers.BayesModelSelection;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.LogProbDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.docmodels.IDocModel;
import com.winterwell.nlp.docmodels.WordFreqDocModel;
import com.winterwell.utils.MathUtils;

/**
 * Use with {@link WordFreqDocModel} to get the "classic" Naive Bayes algorithm.
 * 
 * @see BayesModelSelection
 * @author daniel
 * 
 * @testedby {@link BayesDocModelClassifierTest}
 * @param <T>
 *            e.g. String for String tags
 * @deprecated Use {@link StreamClassifier} for preference
 */
@Deprecated
public class BayesDocModelClassifier<T> extends ATextClassifier<T> {

	final Map<T, IDocModel> models;
	
	ObjectDistribution<T> prior = new ObjectDistribution<T>();

	private boolean newModelsOnDemand;

	/**
	 * Make a default Naive-Bayes classifier. It will generate category models on demand during training.
	 */
	public BayesDocModelClassifier() {
		this(new HashMap());
		newModelsOnDemand = true;
	}
	
	public void setNewModelsOnDemand(boolean newModelsOnDemand) {
		this.newModelsOnDemand = newModelsOnDemand;
		if ( ! models.isEmpty()) throw new IllegalStateException("Please don't combine this with provifing models (sorry).");
	}
	
	public BayesDocModelClassifier(Map<T, IDocModel> models) {
		this.models = models;
	}

	@Override
	public T classify(IDocument text) {
		IFiniteDistribution<T> dist = pClassify(text);
		return dist.getMostLikely();
	}

	/**
	 * Finish training on the prior and the models.
	 */
	@Override
	public void finishTraining() {
		prior.finishTraining();
		for (IDocModel m : models.values()) {
			m.finishTraining();
		}
	}

	// public so I can get at it in ClassifierTests.
	public IDocModel getModel(T tag) {
		IDocModel model = models.get(tag);
		if (model==null && newModelsOnDemand) {
			model = newDocModel();
			models.put(tag, model);
		}
		assert model != null;
		return model;
	}

	protected IDocModel newDocModel() {
		return new WordFreqDocModel();
	}

	// Sod it, I need the whole lot.
	public Map<T, IDocModel> getModels() {
		return models;
	}

	public ObjectDistribution<T> getPrior() {
		return prior;
	}

	/**
	 * This allows custom priors
	 * 
	 * @param tag
	 * @return
	 */
	protected double getPriorProb(T tag) {
		return prior.prob(tag);
	}

	@Override
	public boolean isReady() {
		for (IDocModel m : models.values()) {
			if (!m.isReady())
				return false;
		}
		return true;
	}

	@Override
	public IFiniteDistribution<T> pClassify(IDocument text) {
		assert text != null;
		assert !prior.isEmpty() : prior;
		LogProbDistribution<T> posterior = new LogProbDistribution<T>();
		for (T tag : models.keySet()) {
			// T tag = (T) tg;
			double priorTag = getPriorProb(tag);
			assert MathUtils.isFinite(priorTag) : priorTag + "	" + this;
			IDocModel m = getModel(tag);
			assert m != null : tag + "\n" + models;
			// use log to avoid rounding to 0 errors
			double lp = m.logProb(text);
			if (Double.isInfinite(lp)) {
				continue;
			}			
			double logPosterior = lp + Math.log(priorTag);
			posterior.setLogProb(tag, logPosterior);
		}
		return posterior;
	}

	/**
	 * This will re-setup both the prior and the models!
	 */
	@Override
	public void resetup() {
		if (newModelsOnDemand) {
			// all from training
			prior.clear();
			models.clear();
			return;
		} 
		// setup prior as uniform
		prior.clear();
		double p = 1.0 / models.size();
		for (T tag : models.keySet()) {
			prior.setProb(tag, p);
		}
		// setup models
		for (IDocModel m : models.values()) {
			m.resetup();
		}		
	}

	public void setPrior(ObjectDistribution<T> prior) {
		assert prior != null;
		this.prior = prior;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [models=" + models
				+ ",\n\tprior=" + prior + "]";
	}

	@Override
	public void train1(IDocument x) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void train1(IDocument x, T tag) {
		prior.count(tag);
		IDocModel model = getModel(tag);
		model.train1(x);
	}

}
