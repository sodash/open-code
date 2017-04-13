package com.winterwell.nlp.analysis;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.IDocument;

/**
 * Allow for different implementations.
 * @author daniel
 */
public interface IPhraseFinder extends ITrainable.Unsupervised<IDocument> {

	ObjectDistribution<String> getPhrases();

	IDocument getExample(String phrase);

	void setTrackExamples(boolean b);

	/**
	 * How much to prefer longer phrases by. Some implementations may ignore this.
	 * @param i
	 */
	void setLongerPhraseBonus(double i);

}
