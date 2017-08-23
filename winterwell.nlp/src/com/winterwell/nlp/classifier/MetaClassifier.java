package com.winterwell.nlp.classifier;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.IDocument;

import gnu.trove.TDoubleArrayList;

/**
 * Classify based on the output from several classifiers.
 * 
 * TODO Uses a weighted *sum* of the various outputs. This is not done on any
 * sound mathematical basis.
 * <p>
 * Related to http://en.wikipedia.org/wiki/Boosting This classifier does not
 * actually perform boosting. The use case is for combining classifiers with
 * quite different training data -- as opposed to reweighting the same data set
 * in boosting. NB - I think BrownBoost would be interesting to have.
 * 
 * @author daniel
 * 
 */
public class MetaClassifier<X> implements ITextClassifier<X> {

	List<ITextClassifier<X>> classifiers = new ArrayList<ITextClassifier<X>>();
	TDoubleArrayList weights = new TDoubleArrayList();

	/**
	 * @param classifier
	 * @param weight
	 *            The relative weight to give output from this classifier. In
	 *            boosting, this weight (alpha) is normally ln((1 - err)/err)
	 */
	public void addClassifier(ITextClassifier<X> classifier, double weight) {
		classifiers.add(classifier);
		weights.add(weight);
	}

	@Override
	public X classify(IDocument text) {
		IDiscreteDistribution<X> dist = pClassify(text);
		return dist.getMostLikely();
	}

	@Override
	public void finishTraining() {
		for (ITextClassifier<X> c : classifiers) {
			c.finishTraining();
		}
	}

	@Override
	public boolean isReady() {
		for (ITextClassifier<X> c : classifiers) {
			if (!c.isReady())
				return false;
		}
		return true;
	}

	@Override
	public IFiniteDistribution<X> pClassify(IDocument text) {
		assert classifiers.size() != 0;
		// TODO Uses a weighted *sum* of the various outputs.
		// This is not done on any sound mathematical basis!
		ObjectDistribution<X> dist = new ObjectDistribution<X>();
		for (int i = 0; i < classifiers.size(); i++) {
			ITextClassifier<X> classifier = classifiers.get(i);
			double weight = weights.get(i);
			try {
				IFiniteDistribution<X> distI = classifier.pClassify(text);
				// normalise so we can combine fairly with other distributions
				distI.normalise();
				for (X x : distI) {
					double px = distI.prob(x);
					dist.addProb(x, px * weight);
				}
			} catch (UnsupportedOperationException e) {
				// seems this classifier can't do (normalised) probability
				// distributions
				X tag = classifier.classify(text);
				dist.addProb(tag, weight);
			}
		}
		// note: this is not itself normalised
		return dist;
	}

	@Override
	public void resetup() {
		for (ITextClassifier<X> c : classifiers) {
			c.resetup();
		}
	}

	@Override
	public void train1(IDocument x, X tag, double weight) {
		for (ITextClassifier<X> c : classifiers) {
			c.train1(x, tag, weight);
		}
	}

}
