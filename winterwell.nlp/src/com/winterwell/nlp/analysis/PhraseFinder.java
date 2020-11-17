package com.winterwell.nlp.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;

/**
 * A simple common phrase finder.
 *  * 
 * @author daniel
 * @testedby  PhraseFinderTest}
 */
public class PhraseFinder extends ATrainableBase<IDocument, Object> implements
		ITrainable.Unsupervised<IDocument> , IPhraseFinder
{
	
	private static final List<String>[] ARRAY = new List[0];
	private ObjectDistribution<String> common1;
	private ObjectDistribution<List<String>> common2;

	private ObjectDistribution<List<String>> common3;

	double LONGER_PHRASE_BONUS = 4;

	/**
	 * How many phrases to find
	 */
	private int n;
	private boolean oncePerDocument = true;
	private int overlapPenalty = 3;
	private ObjectDistribution<String> phrases;
	private Set<String> stopwords = NLPWorkshop.get().getStopwords();
	final ITokenStream tokeniser;
	private Map<List<String>, List<String>> trend2words;
	private Map<List<String>, IDocument> words2doc;

	/**
	 * @param tokeniser
	 * @param n
	 *            How many phrases?
	 */
	public PhraseFinder(ITokenStream tokeniser, int n) {
		this.tokeniser = tokeniser;
		this.n = n;
		assert n > 0;
	}

	@Override
	public void finishTraining() {
		// wot no data?
		if (trainingData == null) {
			super.finishTraining();
			phrases = new ObjectDistribution<String>();
			return;
		}
		// tokenise
		List<List<String>> tokenised = new ArrayList<List<String>>();
		for (IDocument x : trainingData) {
			ITokenStream tokens = tokeniser.factory(x.getContents());
			ArrayList<String> words = new ArrayList<String>();
			for (Tkn token : tokens) {
				words.add(token.getText());
			}
			tokenised.add(words);
			if (trend2words != null) {
				words2doc.put(words, x);
			}
		}

		// find the most common words & phrases
		finishTraining2_mostCommon(tokenised);

		// pick 'em
		ObjectDistribution<List<String>> _phrases = finishTraining2_pickPhrases();

		// prune the trend-to-doc map?? Does this matter - it will hold some
		// memory,
		// but the PhraseFinder is itself likely to be short-lived.
		// if (trend2doc != null) {
		// for(String p : trend2doc.keySet().toArray(StringUtils.ARRAY)) {
		// if ( ! phrases.contains(p)) trend2doc.remove(p);
		// }
		// }

		// Scale the probs for the long ones back down so font-scaling isn't
		// nuts.
		phrases = new ObjectDistribution<String>();
		for (List<String> words : _phrases) {
			double v = _phrases.prob(words);
			// scale down
			for (int i = 1; i < words.size(); i++) {
				v = v / LONGER_PHRASE_BONUS;
			}
			// ok
			phrases.setProb(StrUtils.join(words, " "), v);
		}

		common1 = null;
		common2 = null;
		common3 = null;
		super.finishTraining();
	}

	private void finishTraining2_mostCommon(List<List<String>> tokenised) {
		int CANDIDATE_POOL = 2 * n;
		// Most common words
		common1 = new ObjectDistribution<String>();
		HashSet<String> thisDoc = new HashSet<String>();
		for (List<String> words : tokenised) {
			thisDoc.clear();
			for (String w : words) {
				if (stopwords.contains(w)) {
					continue;
				}
				if (ignoreWord(w)) {
					continue;
				}
				// avoid counting repetition within a document
				if (oncePerDocument) {
					if (thisDoc.contains(w)) {
						continue;
					}
					thisDoc.add(w);
				}
				List<String> p = Arrays.asList(w);
				common1.count(w);
				if (trend2words != null) {
					trend2words.put(p, words);
				}
			}
		}
		// It's not a phrase if it only occurs once
		common1.pruneBelow(1.1);
		common1.prune(CANDIDATE_POOL);
		// TODO this is not efficient if there are many docs!
		// Most common bigrams
		common2 = new ObjectDistribution<List<String>>();
		for (List<String> words : tokenised) {
			for (int i = 0; i < words.size() - 1; i++) {
				String w = words.get(i);
				if (!common1.contains(w)) {
					continue;
				}
				List<String> p = Arrays.asList(w, words.get(i + 1));
				common2.count(p);
				if (trend2words != null) {
					trend2words.put(p, words);
				}
			}
		}
		common2.pruneBelow(1.1);
		common2.prune(CANDIDATE_POOL);
		// Most common trigrams
		// TODO penalise phrase starting or ending with a stop word
		common3 = new ObjectDistribution<List<String>>();
		if (common2.isEmpty())
			return;
		for (List<String> words : tokenised) {
			for (int i = 0, n = words.size() - 2; i < n; i++) {
				String w1 = words.get(i);
				String w2 = words.get(i + 1);
				String w3 = words.get(i + 2);
				if (!common2.contains(Arrays.asList(w1, w2))) {
					continue;
				}
				if (stopwords.contains(w3)) {
					continue;
				}
				// TODO avoid counting repetition within a document
				List<String> p = Arrays.asList(w1, w2, w3);
				common3.count(p);
				if (trend2words != null) {
					trend2words.put(p, words);
				}
			}
		}
		common3.pruneBelow(1.1);
		common3.prune(CANDIDATE_POOL);
	}

	private ObjectDistribution<List<String>> finishTraining2_pickPhrases() {
		ObjectDistribution<List<String>> candidatePhrases = new ObjectDistribution<List<String>>();
		for (Map.Entry<String, Double> me : common1.asMap().entrySet()) {
			candidatePhrases.setProb(Arrays.asList(me.getKey()), me.getValue());
		}
		for (List<String> w : common2) {
			double v = common2.prob(w);
			v = v * LONGER_PHRASE_BONUS;
			String w2 = w.get(1);
			if (stopwords.contains(w2)) {
				continue;
			}
			candidatePhrases.setProb(w, v);
		}
		for (List<String> w : common3) {
			double v = common3.prob(w);
			v = v * LONGER_PHRASE_BONUS * LONGER_PHRASE_BONUS;
			candidatePhrases.setProb(w, v);
		}

		// not much choice?
		if (candidatePhrases.size() <= n)
			return candidatePhrases;

		// an initial prune before the (potentially expensive) looping below
		if (candidatePhrases.size() > 3 * n) {
			candidatePhrases.prune(3 * n);
		}

		// Add one-by-one, avoiding e.g. both "White" and "White House" being
		// common
		ObjectDistribution<List<String>> _phrases = new ObjectDistribution<List<String>>();
		List<String>[] _candidatePhrases = candidatePhrases.toArray(ARRAY);
		while (_phrases.size() < n && !candidatePhrases.isEmpty()) {
			List<String> best = candidatePhrases.getMostLikely();
			double prob = candidatePhrases.prob(best);
			if (prob == 0) {
				break;
			}
			_phrases.setProb(best, prob);
			candidatePhrases.setProb(best, 0);
			for (List<String> p : _candidatePhrases) {
				if (overlap(best, p)) {
					double pv = candidatePhrases.prob(p);
					candidatePhrases.setProb(p, pv / overlapPenalty);
				}
			}
		}
		return _phrases;
	}

	/**
	 * @return an example for each common phrase. Assumes:
	 *         {@link #finishTraining()} has been called.
	 */
	public Map<String, IDocument> getExamples() {
		assert isReady();
		Map<String, IDocument> trend2eg = new ArrayMap<String, IDocument>(
				phrases.size());
		for (String p : phrases.keySet()) {
			List<String> _p = Arrays.asList(p.split(" "));
			List<String> arr = trend2words.get(_p);
			IDocument doc = words2doc.get(arr);
			trend2eg.put(p, doc);
		}
		return trend2eg;
	}
	
	@Override
	public IDocument getExample(String phrase) {
		return getExamples().get(phrase);
	}

	public ObjectDistribution<String> getPhrases() {
		return phrases;
	}

	public Set<String> getStopWords() {
		return stopwords;
	}

	protected boolean ignoreWord(String w) {
		return false;
	}

	/**
	 * 
	 * @param p1
	 *            space-separated tokens
	 * @param p2
	 *            space-separated tokens
	 * @return
	 */
	boolean overlap(List<String> p1, List<String> p2) {
		for (String w : p1) {
			for (String w2 : p2) {
				if (w.equals(w2))
					return true;
			}
		}
		return false;
	}

	@Override
	public void resetup() {
		super.resetup();
		return;
	}

	/**
	 * 5 by default. Most users over-ride this! Should be higher when handling
	 * high-volume, lower for low-volume.
	 */
	public void setLongerPhraseBonus(double bonusMultiplier) {
		LONGER_PHRASE_BONUS = bonusMultiplier;
	}

	/**
	 * @param oncePerDocument
	 *            if true (the default), avoid counting repetition within a
	 *            document.
	 */
	public void setOncePerDocument(boolean oncePerDocument) {
		this.oncePerDocument = oncePerDocument;
	}

	/**
	 * TODO How much to penalise phrases for overlapping with each other.
	 * 
	 * @param i
	 */
	public void setOverlapPenalty(int i) {
		overlapPenalty = i;
	}

	/**
	 * By default, does not use any stopwords!
	 * 
	 * @param stopwords
	 */
	public void setStopWords(Set<String> stopwords) {
		this.stopwords = stopwords;
	}

	public void setTrackExamples(boolean b) {
		if (!b) {
			trend2words = null;
			words2doc = null;
			return;
		}
		trend2words = new HashMap<List<String>, List<String>>(512);
		words2doc = new HashMap<List<String>, IDocument>(64);
	}

	@Override
	public void train(Iterable<? extends IDocument> data) {
		super.train(data);
	}

	@Override
	public void train1(IDocument x) {
		super.train1(x);
	}

}
