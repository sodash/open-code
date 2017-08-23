package com.winterwell.nlp.docmodels;

import java.util.HashSet;
import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.maths.datastorage.Index;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.IndexedDistribution;
import com.winterwell.nlp.classifier.WordFreqDocModelTest;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.KErrorPolicy;

/**
 * The core of NaiveBayes.
 * <p>
 * Frequencies are stored as raw counts.
 * 
 * @author daniel
 * 
 * @testedby {@link WordFreqDocModelTest}
 */
public final class WordFreqDocModel extends ADocModel implements
		ITrainable.Unsupervised.Weighted<IDocument> {

	private IndexedDistribution<String> freqs;

	private double pseudoCount = 2;

	/**
	 * convenience for a "default" model
	 */
	public WordFreqDocModel() {
		this(new WordAndPunctuationTokeniser().setNormaliseToAscii(KErrorPolicy.ACCEPT).setLowerCase(true).setSwallowPunctuation(true), 
			new Index<String>());
	}

	/**
	 * Note: *does* call init()
	 * 
	 * @param tokenizer
	 * @param index
	 */
	public WordFreqDocModel(ITokenStream tokenizer, IIndex<String> index) {
		super(tokenizer, index);
		this.freqs = new IndexedDistribution<String>(index);
		// save some memory
		noTrainingDataCollection();
		// note: freqs will catch index pruning events
		init();
	}

	/**
	 * Nothing to do.
	 */
	@Override
	public void finishTraining() {
		//
	}

	@Override
	public String getDescription() {
		if (freqs.getTotalWeight() == 0) {
			assert trainingCount == 0 : this;
			return "[untrained]";
		}
		List<String> mostLikely = freqs.getMostLikely(10);

		// sanity check
		HashSet<String> set = new HashSet<String>(mostLikely);
		assert set.size() == mostLikely.size() : mostLikely;
		for (String word : mostLikely) {
			assert !Utils.isBlank(word) : mostLikely;
		}

		return "[words: " + Printer.toString(mostLikely, ", ") + "...]";
	}


	/**
	 * @param n
	 * @return the most likely words in this word-freq distribution
	 */
	public List<String> getMostLikely(int n) {
		return freqs.getMostLikely(n);
	}

	/**
	 * Includes the estimated sum of the pseudo counts for all words.
	 */
	double getTotalCount() {
		double vocab = getVocabSize();
		double totalCount = freqs.getTotalWeight() + vocab * pseudoCount;
		assert totalCount > 0 : getVocabSize() + "\n" + freqs;
		return totalCount;
	}

	private double getVocabSize() {
		return freqs.size();
	}

	/**
	 * Low-level access to the un-normalised word counts.
	 */
	public IFiniteDistribution<String> getWordCounts() {
		return freqs;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public double logProb(IDocument x) {
		assert x != null;
		double lp = 0;
		ITokenStream tokens = tokenizer.factory(x.getContents());
		int tokenCount = 0;
		for (Tkn token : tokens) {
			String word = token.getText();
			double p = probWord(word);
			if (p==0) {
				return Double.NEGATIVE_INFINITY;
			}
			lp += Math.log(p);
			assert MathUtils.isFinite(p);
			tokenCount++;
		}
		return lp;
	}

	@Override
	public double prob(IDocument x) {
		double lp = logProb(x);
		return Math.exp(lp);
	}

	/**
	 * prob inc pseudo-count
	 * 
	 * @param word
	 * @return
	 */
	public double probWord(String word) {
		double trueFreq = freqs.prob(word);
		double p = (trueFreq + pseudoCount) / getTotalCount();
		return p;
	}

	@Override
	public void pruneToIndex() {
		freqs.pruneToIndex();
	}

	@Override
	public void resetup() {
		freqs = new IndexedDistribution<String>(freqs.getIndex());
		super.resetup();
	}

	@Override
	public IDocument sample(int len) throws UnsupportedOperationException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			String w = sampleWord();
			sb.append(w + " ");
		}
		StrUtils.pop(sb, 1);
		return new SimpleDocument(sb.toString());
	}

	/**
	 * 
	 * @return
	 * @throws IllegalStateException
	 *             if there are no known words
	 */
	public String sampleWord() throws IllegalStateException {
		double mix = freqs.getTotalWeight() / getTotalCount();
		if (random().nextDouble() > mix) {
			// sample from the pseudo counts
			// Hack: random word - no nice way of doing this as we don't know
			// what indexes are valid
			IIndex<String> index = freqs.getIndex();
			for (int i = 0; i < 100; i++) { // give up eventually
				i = random().nextInt(index.size() * 4); // allow for *some* gaps
				String w = index.get(i);
				if (w != null)
					return w;
			}
			// Note: can fall through to sample-from-dist below
		}
		// sample from the dist
		return freqs.sample();
	}

	/**
	 * Pseudo-counts avoid the problem that unseen words would otherwise lead to
	 * zero-probability.
	 * 
	 * @param pseudoCount
	 *            the counting boost given to every word, seen or unseen.
	 *            Defaults to 2.
	 */
	public void setUnseenWordHandling(double pseudoCount) {
		this.pseudoCount = pseudoCount;
	}

	@Override
	public String toString() {
		return "WordFreqDocModel [freqs=" + freqs + ", tokenizer=" + tokenizer
				+ "]";
	}

	@Override
	public void train(double[] weights, Iterable<? extends IDocument> wdata) {
		int i = 0;
		for (IDocument doc : wdata) {
			train1(doc, weights[i]);
			i++;
		}
	}

	@Override
	public void train1(IDocument doc) {
		train1(doc, 1);
	}

	@Override
	public void train1(IDocument doc, double weight) {
		ITokenStream stream = tokenizer.factory(doc.getContents());
		for (Tkn token : stream) {
			freqs.addProb(token.getText(), weight);
		}
		trainingCount++;
	}

}
