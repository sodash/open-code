package com.winterwell.nlp.classifier;


import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.nlp.corpus.IDocument;

public abstract class ATextClassifier<T> extends ATrainableBase<IDocument, T> implements ITextClassifier<T> {

	@Override
	public void finishTraining() {
		super.finishTraining();
	}
	
	@Override
	public void train1(IDocument x, T tag, double weight) {
		super.train1(x, tag, weight);
	}	
	
	@Override
	public T classify(IDocument text) {
		IFiniteDistribution<T> dist = pClassify(text);
		return dist.getMostLikely();
	}


	@Override
	public void resetup() {
		super.resetup();
	}

}
