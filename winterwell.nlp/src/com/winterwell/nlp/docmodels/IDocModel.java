package com.winterwell.nlp.docmodels;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import com.winterwell.nlp.corpus.IDocument;

public interface IDocModel extends IDiscreteDistribution<IDocument>,
		ITrainable.Unsupervised<IDocument> {

	IIndex getWordIndex();

	IDocument sample(int len);

}
