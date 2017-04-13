package com.winterwell.nlp.analysis;

import java.util.Map;
import java.util.Set;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.utils.TodoException;

/**
 * The Problem: If there's one popular tweet (many retweets), or it could also occur, though less likely, with
 * a key message which gets a lot of traffic as-is -- then that message's phrases will take all of the top-phrase
 * slots.
 * 
 * Possible solution: Track phrase-phrase correlation. 
 * 
 * Proposed solution: Learn several message clusters, and pick top-phrases from all of them.
 * 
 *  Use a single-pass online variant of EM.
 *  Each clutsre build its own phrases.
 *  Assign a message to a cluster & train that cluster (with some size penalty to avoid clusters dying, 'cos this is a one-pass setup)
 *  Pick from all clusters in rotation, or something, to get the top phrases.
 * 
 * @author daniel
 *
 */
public class ClusteredPhraseFinder extends ATrainableBase<IDocument, Object> implements
ITrainable.Unsupervised.Weighted<IDocument>, IPhraseFinder {


	private ITokenStream tokeniser;
	private int n;

	/**
	 * @param tokeniser
	 * @param n
	 *            How many phrases?
	 */
	public ClusteredPhraseFinder(ITokenStream tokeniser, int n) {
		this.tokeniser = tokeniser;
		this.n = n;
		assert n > 0 : n;
		assert n > 0 : "Invalid top N " + n;
	}

	@Override
	public void finishTraining() {
		throw new TodoException();
	}

	/**
	 * @return an example for each common phrase.
	 */
	public Map<String, IDocument> getExamples() {
		throw new TodoException();
	}

	@Override
	public IDocument getExample(String phrase) {
		throw new TodoException();
	}

	@Override
	public boolean isReady() {
		throw new TodoException();
	}

	/**
	 * WARNING This will edit the distributions by pruning!
	 * @return
	 */
	@Override
	public ObjectDistribution<String> getPhrases() {
		throw new TodoException();
	}




	@Override
	public void resetup() {
		return;
	}

	/**
	 * 5 by default. Most users over-ride this! Should be higher when handling
	 * high-volume, lower for low-volume.
	 */
	@Override
	public void setLongerPhraseBonus(double bonusMultiplier) {
	}


	/**
	 * ??How much to penalise phrases for overlapping with each other.
	 *
	 * @param i
	 */
	public void setOverlapPenalty(int i) {
	}

	/**
	 * By default, does not use any stopwords!
	 *
	 * @param stopwords
	 */
	public void setStopWords(Set<String> stopwords) {
	}

	@Override
	public void setTrackExamples(boolean b) {
	}

	@Override
	public void train(Iterable<? extends IDocument> data) {
		super.train(data);
	}

	@Override
	public void train1(IDocument doc) {
		train1(doc, 1);
	}

	@Override
	public final void train(double[] weights, Iterable<? extends IDocument> wdata) {
		int i=0;
		for (IDocument x : wdata) {
			double wi = weights[i];
			i++;
			train1(x, wi);
		}
	}

	@Override
	public void train1(IDocument x, double weight) {
	}


}
