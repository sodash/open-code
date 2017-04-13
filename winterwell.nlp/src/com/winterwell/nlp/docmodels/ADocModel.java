package com.winterwell.nlp.docmodels;

import java.util.List;

import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.maths.datastorage.IPruneListener;
import com.winterwell.maths.stats.distributions.ADistributionBase;
import com.winterwell.maths.stats.distributions.d1.ExponentialDistribution1D;
import com.winterwell.maths.stats.distributions.discrete.AFiniteDistribution;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.utils.TodoException;

import com.winterwell.depot.IInit;

/**
 * A trainable model of a type of document.
 * 
 * <p>
 * Note: does not extend {@link AFiniteDistribution} 'cos the space of documents
 * is not finite.
 * 
 * @author daniel
 */
public abstract class ADocModel extends ADistributionBase<IDocument> implements
		IDocModel, IInit, IPruneListener {

	@Override
	public final int size() {
		return -1;
	}
	
	/**
	 * String to use for unknown words
	 */
	public static final String UNKNOWN_WORD = "??";

	protected boolean initFlag;

	protected ITokenStream tokenizer;

	/**
	 * Subclasses should maintain this!
	 */
	protected int trainingCount;

	protected final IIndex<String> wordIndex;

	/**
	 * Note: init() is not called
	 * 
	 * @param tokenizer
	 *            Can be null if that's how the subclass works.
	 * @param wordIndex
	 *            Can be null if that's how the subclass works.
	 */
	public ADocModel(ITokenStream tokenizer, IIndex wordIndex) {
		this.tokenizer = tokenizer;
		this.wordIndex = wordIndex;
	}

	@Override
	public void finishTraining() {
		super.finishTraining();
	}

	public String getDescription() {
		return "training-count: " + getTrainingCount();
	}

	public Object getExplanation(IDocument doc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IDocument getMostLikely() {
		throw new UnsupportedOperationException();
	}

	public ITokenStream getTokenizer() {
		return tokenizer;
	}

	/**
	 * @return number of training examples seen by this tag model
	 */
	public int getTrainingCount() {
		return trainingCount;
	}

	@Override
	public IIndex getWordIndex() {
		return wordIndex;
	}

	/**
	 * Attach to the index as a listener
	 */
	@Override
	public void init() {
		if (initFlag)
			return;
		initFlag = true;
		IIndex index = getWordIndex();
		if (index != null) {
			index.addListener(this);
		}
	}

	@Override
	public boolean isReady() {
		return super.isReady();
	}

	@Override
	public double logProb(IDocument x) {
		// over-ride if you can do better than this
		return Math.log(prob(x));
	}

	@Override
	public final void normalise() throws UnsupportedOperationException {
		// can't normalise - infinite space
		throw new UnsupportedOperationException();
	}

	@Override
	public double normProb(IDocument obj) {
		if (isNormalised())
			return prob(obj);
		throw new TodoException();
	}

	/**
	 * The probability for this document <i>given the size of space</i> e.g. the
	 * length of the document. Do NOT compare probs between docs of different
	 * sizes, or between models with different complexity!
	 * 
	 */
	@Override
	public abstract double prob(IDocument x);

	@Override
	public void pruneEvent(List prunedIndexes) {
		pruneToIndex();
	}

	/**
	 * Indexes can discard words. This adjusts the model to reocgnise that, e.g.
	 * dropping vector entries for words that are no longer there.
	 * 
	 * @param wordIndex
	 */
	public abstract void pruneToIndex();

	@Override
	public void resetup() {
		super.resetup();
		trainingCount = 0;
	}

	@Override
	public IDocument sample() {
		// TODO learn the length distribution!
		int len = (int) (1 + new ExponentialDistribution1D(0.2).sample());
		return sample(len);
	}

	@Override
	public IDocument sample(int len) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setProb(IDocument obj, double value)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void setTokenizer(ITokenStream tokenizer) {
		this.tokenizer = tokenizer;
	}

	@Override
	public void train(Iterable<? extends IDocument> data) {
		super.train(data);
	}

	@Override
	public synchronized void train1(IDocument x) {
		super.train1(x);
	}

}
