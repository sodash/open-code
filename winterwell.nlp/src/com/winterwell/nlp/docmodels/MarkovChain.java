package com.winterwell.nlp.docmodels;

import gnu.trove.TIntArrayList;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.winterwell.utils.MathUtils;

import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Vector.Norm;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.SparseVector;
import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.maths.datastorage.IPruneListener;
import com.winterwell.maths.datastorage.Index;
import com.winterwell.maths.datastorage.Vectoriser;
import com.winterwell.maths.matrix.SparseMatrix;
import com.winterwell.maths.stats.distributions.discrete.VectorDistribution;
import com.winterwell.nlp.classifier.DocVectoriser;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;

/**
 * A non-hidden Markov model, which outputs the likelihood of such a sequence
 * (out of n-length sequences).
 * 
 * The thinking is, to use these in place of the sequence-blind NaiveBayes
 * NBModels.
 * 
 * TODO more testing!
 * 
 * TODO in the markov vs bag mixture weighting: something based on number of
 * positive views of this transition. E.g. Given 5 views of David->Cameron,
 * makes that hi-prob without making David->Smith low-prob.
 * 
 * TODO use a generic trained prior rather than uniform prior
 * 
 * @see MultiResolutionDocModel
 * 
 * @testedby {@link MarkovChainTest}
 * @author daniel
 * 
 */
public class MarkovChain extends ADocModel implements IPruneListener {
	
	/**
	 * gives our pseudo-count to handle low data volumes. We treat this as a
	 * mixture model at each step in the chain. would be a bit faster to use
	 * VectorDistribution, but no built-in pseudo
	 */
	private final WordFreqDocModel bgProbs;

	/**
	 * for doing inline normalisation. count[i] = count of transitions from
	 * state i (where is is a prev-code as given by {@link #getPrevCode(int[])})
	 */
	Vector count = new SparseVector(Integer.MAX_VALUE);

	/**
	 * Affects how fast the pure markov model takes over from the
	 * NaiveBayes-with-pseudocount model.
	 */
	private double inertia = 10;

	/**
	 * TODO The n in n-gram: what length of memory to use.
	 */
	int n = 1;

	int OUT_OF_SEQN = Integer.MAX_VALUE - 17;

	private boolean repeatsInSample = true;

	final Vector start = new SparseVector(Integer.MAX_VALUE);

	/**
	 * cached total of start values
	 */
	int startCount;

	/**
	 * i = index of tag P(row|col), ie. row=new, col=prev-code (old).
	 * 
	 * @see #getPrevCode(int[]) for column values
	 */
	final SparseMatrix trans = new SparseMatrix();

	final private DocVectoriser vectoriser;

	/**
	 * convenience for a "default" model
	 */
	public MarkovChain() {
		this(new DocVectoriser(new WordAndPunctuationTokeniser(),
				new Index<String>(), Vectoriser.KUnknownWordPolicy.Add));
		// vectoriser.setUnknownWord(Vectoriser.ADD_UNKNOWN_WORDS);
	}

	/**
	 * Note: *does* call init()
	 * 
	 * @param vectoriser
	 */
	public MarkovChain(DocVectoriser vectoriser) {
		super(vectoriser.getTokeniser(), vectoriser.getIndex());
		this.vectoriser = vectoriser;
		// use the pipeline from the vectoriser
		bgProbs = new WordFreqDocModel(getTokenizer(), vectoriser.getIndex());
		// needs *some* training before use
		pleaseTrainFlag = true;
		// catch pruning events
		vectoriser.getIndex().addListener(this);
		init();
	}

	// private double pseudoCount = 2;

	@Override
	public void finishTraining() {
		// all done inline -- except for normalisation
	}

	public WordFreqDocModel getBgProbs() {
		return bgProbs;
	}

	@Override
	public String getDescription() {
		if (trainingCount == 0)
			return "[untrained]";
		return "eg. " + sample();
	}

	/**
	 * Convert the previous n words into an int code. If n=1 (ie a standard
	 * Markov Chain), then the code is just the contents.<br>
	 * TODO Warning: may overlap Will that matter??
	 * 
	 * @param prev
	 * @return state-code for accessing transitions
	 */
	int getPrevCode(int[] prev) {
		int c = 0;
		for (int wi : prev) {
			c = c * 3 + wi;
		}
		return c;
	}

	public Vector getStart() {
		return start;
	}

	private int[] getStartingPrev() {
		int[] prev = new int[n];
		Arrays.fill(prev, OUT_OF_SEQN);
		return prev;
	}

	/**
	 * i = index of tag P(row|col), ie. row=new, col=old
	 * <p>
	 * Editing this directly will mess up the cached weightings. Call
	 * {@link #pruneToIndex()} to re-calculate.
	 */
	public SparseMatrix getTransitionMatrix() {
		return trans;
	}

	public DocVectoriser getVectoriser() {
		return vectoriser;
	}

	@Override
	public double logProb(IDocument x) {
		// for debugging
		// double bglp = bgProbs.logProb(x);

		assert isReady() || startCount == 0;
		assert vectoriser.isUnknownOK() : vectoriser;
		if (startCount == 0)
			return Double.NEGATIVE_INFINITY;
		TIntArrayList vs = vectoriser.toIndexList(x);
		if (vs.isEmpty())
			// undefined really
			return 0;

		int prevWord = vs.get(0);
		List<String> words = vectoriser.inverseIndexList(vs); // for debugging
																// help
		double startFreq = start.get(prevWord);
		double lp = logProb2_adjustedLogProb(startFreq, startCount, prevWord);
		if (lp == Double.NEGATIVE_INFINITY)
			// zero probability!
			return Double.NEGATIVE_INFINITY;
		int[] prev = getStartingPrev();
		shuffleAdd(prev, vs.get(0));
		for (int i = 1; i < vs.size(); i++) {
			int vi = vs.get(i);
			// pi = transition prob from prev to vi
			int prevCode = getPrevCode(prev);
			double trueFreq = trans.get(vi, prevCode);
			double countTrans = count.get(prevCode);
			double lpi = logProb2_adjustedLogProb(trueFreq, countTrans, vi);
			if (lpi == Double.NEGATIVE_INFINITY)
				// zero probability!
				return Double.NEGATIVE_INFINITY;
			lp += lpi;
			assert MathUtils.isFinite(lp);
			shuffleAdd(prev, vi);
		}
		assert MathUtils.isFinite(lp) : lp + "	" + x;
		return lp;
	}

	private double logProb2_adjustedLogProb(double trueFreq, double trueTotal,
			int wordIndex) {
		assert trueFreq >= 0 && trueTotal >= 0 && wordIndex >= 0;
		// the naive-bayes mixture element
		String word = vectoriser.getIndex().get(wordIndex);
		double bgp = bgProbs.probWord(word);
		if (trueTotal == 0)
			return Math.log(bgp);
		// merge true-freq prob with generic bag-of-words with pseudo count
		double mix = trueTotal / (trueTotal + inertia);
		// normalise
		double pi = trueFreq / trueTotal;
		pi = mix * pi + (1 - mix) * bgp;
		// log
		assert MathUtils.isFinite(pi);
		return Math.log(pi);
	}

	@Override
	public double prob(IDocument x) {
		double lp = logProb(x);
		if (lp == Double.NEGATIVE_INFINITY)
			return 0;
		return Math.exp(lp);
	}

	// /**
	// * Use these for creating transition states
	// */
	// static int[] primes = new int[]{2,3,5,7,11,13,17,19,23};

	@Override
	public void pruneEvent(List prunedIndexes) {
		pruneToIndex();
	}

	@Override
	public void pruneToIndex() {
		IIndex<String> index = vectoriser.getIndex();
		// prune start distribution
		for (VectorEntry ve : start) {
			if (index.get(ve.index()) != null) {
				continue;
			}
			ve.set(0);
		}
		if (start instanceof SparseVector) {
			((SparseVector) start).compact();
		}
		startCount = (int) start.norm(Norm.One);
		// prune transitions
		count.zero();
		for (MatrixEntry me : trans) {
			if (index.get(me.row()) != null && index.get(me.column()) != null) {
				count.add(me.column(), me.get());
				continue;
			}
			me.set(0);
		}
		if (count instanceof SparseVector) {
			((SparseVector) count).compact();
		}
		trans.compact();
		// prune background
		bgProbs.pruneToIndex();
	}

	@Override
	public void resetup() {
		super.resetup();
		count.zero();
		start.zero();
		startCount = 0;
		trans.zero();
		bgProbs.resetup();
	}

	@Override
	public IDocument sample(int len) {
		if (len == 0)
			return new SimpleDocument("");
		StringBuilder sb = new StringBuilder();
		IIndex<String> index = vectoriser.getIndex();
		Set<Integer> used = new HashSet();

		int[] prev = getStartingPrev();
		{ // start
			int si = sample2_startWord();
			String word = index.get(si);
			sb.append(word + " ");
			used.add(si);
			shuffleAdd(prev, si);
		}

		// go on
		for (int i = 1; i < len; i++) {
			int wi = IIndex.UNKNOWN;
			for (int j = 0; j < 100; j++) { // only try so hard to avoid repeats
				wi = sample2_nextWord(prev);
				if (repeatsInSample || !used.contains(wi)) {
					break;
				}
			}
			String word = wi == IIndex.UNKNOWN ? UNKNOWN_WORD : index.get(wi);
			if (wi != IIndex.UNKNOWN) {
				used.add(wi);
			}
			sb.append(word + " ");
			shuffleAdd(prev, wi);
			if (wi == OUT_OF_SEQN) {
				break;
			}
		}
		// TODO special case the end??

		return new SimpleDocument(sb.toString().trim());
	}

	int sample2_nextWord(int[] prev) {
		IIndex<String> index = vectoriser.getIndex();
		int prevCode = getPrevCode(prev);
		double cnt = count.get(prevCode);
		double mix = cnt / (cnt + inertia);
		if (random().nextDouble() <= mix) {
			// sample transition
			Vector col = trans.getColumn(prevCode);
			VectorDistribution dist = new VectorDistribution(col);
			try {
				Integer next = dist.sample();
				return next;
			} catch (Exception e) {
				// this means dist had zero size - fallback to bgProbs
			}
		}
		// sample from the background bag
		String word = bgProbs.sampleWord();
		assert word != null : bgProbs;
		return index.indexOf(word);
	}

	int sample2_startWord() {
		double mix = startCount / (startCount + inertia);
		if (random().nextDouble() > mix) {
			String word = bgProbs.sampleWord();
			IIndex<String> index = vectoriser.getIndex();
			int si = index.indexOf(word);
			return si;
		}
		VectorDistribution startDist = new VectorDistribution(start);
		Integer si = startDist.sample();
		return si;
	}

	/**
	 * Affects how fast the pure markov model takes over from the
	 * NaiveBayes-with-pseudocount model. A high inertia value means the
	 * NaiveBayes will predominate.
	 */
	public void setInertia(double inertia) {
		this.inertia = inertia;
	}

	/**
	 * TODO The n in n-gram: what length of memory to use. 0 (which is not
	 * allowed) would be bag-of-words aka naive-bayes, 1 = vanilla markov chain
	 * 
	 */
	public void setN(int n) {
		assert !isReady();
		assert n > 0;
		this.n = n;
	}

	/**
	 * Hack: block repeated choices in samples
	 * 
	 * @param b
	 */
	public void setRepeatsInSample(boolean b) {
		repeatsInSample = b;
	}

	@Deprecated
	// hack!
	public void setStartCount(int startCount) {
		this.startCount = startCount;
	}

	/**
	 * Shuffle the history along and add the new element
	 * 
	 * @param prev
	 *            Drop the 1st entry
	 * @param wi
	 *            Add this to the end
	 * @return
	 */
	private void shuffleAdd(int[] prev, int wi) {
		for (int i = 1; i < prev.length; i++) {
			prev[i - 1] = prev[i];
		}
		prev[prev.length - 1] = wi;
	}

	@Override
	public synchronized void train1(IDocument x) {
		// ITokenStream tokens = tokenizer.factory(x.getText());
		// if ( ! tokens.hasNext()) return;
		TIntArrayList vs = vectoriser.toIndexList(x);
		if (vs.isEmpty())
			return;
		// add in the end-of-sequence marker
		vs.add(OUT_OF_SEQN);

		// train the start vector
		int wi = vs.get(0);
		start.add(wi, 1);
		startCount++;

		// train the transitions
		int[] prev = getStartingPrev();
		prev[prev.length - 1] = wi;
		for (int i = 1; i < vs.size(); i++) {
			int prevCode = getPrevCode(prev);
			count.add(prevCode, 1); // keep counts up to date
			wi = vs.get(i);
			trans.add(wi, prevCode, 1);
			shuffleAdd(prev, wi);
		}

		// also train bag of words
		bgProbs.train1(x);

		// ready for use
		pleaseTrainFlag = false;
	}

}
