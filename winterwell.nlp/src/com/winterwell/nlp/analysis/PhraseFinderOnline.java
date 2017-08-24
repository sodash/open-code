package com.winterwell.nlp.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.datastorage.HalfLifeMap;
import com.winterwell.maths.datastorage.IPruneListener;
import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.SentenceStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.WeirdException;
import com.winterwell.utils.web.WebUtils2;

/**
 * A simple *lossy* common phrase finder.
 *
 * This would be better/easier if it could do several passes over the data.
 *
 * @author daniel
 * @testedby {@link PhraseFinderOnlineTest}
 */
public class PhraseFinderOnline extends ATrainableBase<IDocument, Object> implements
ITrainable.Unsupervised.Weighted<IDocument>, IPhraseFinder {

	private static final String LOGTAG = "PhraseFinder";
	private ObjectDistribution<String> common1;
	private ObjectDistribution<List<String>> common2;
	private ObjectDistribution<List<String>> common3;

	double LONGER_PHRASE_BONUS = 4;

	/**
	 * How many phrases to find
	 */
	private final int n;
	private boolean oncePerDocument = true;
	private int overlapPenalty = 3;

	@Deprecated // TODO delete in 2016
	private transient Set<String> stopwords;
	
	final ITokenStream tokeniser;
	Map<List<String>, String> words2raw;
	/**
	 * Track raw->eg, 'cos this is the lookup the end-user wants to do
	 */
	Map<String, IDocument> raw2eg;
	private boolean trackExamples;

	/**
	 * @param tokeniser TODO we'd like a tokeniser that respects phrase boundary markers.
	 * @param n
	 *            How many phrases?
	 */
	public PhraseFinderOnline(ITokenStream tokeniser, int n) {		
		this.tokeniser = tokeniser;
		this.n = n;
		assert n > 0 : n;
		assert n > 0 : "Invalid top N " + n;
	}

	@Override
	public void finishTraining() {
		// does nothing -- getPhrases() does the work		
	}

	/**
	 * @return an example for each common phrase.
	 */
	public Map<String, IDocument> getExamples() {
		if (raw2eg==null) {
			Log.escalate(new WeirdException("No raw2eg map in "+this));
			return Collections.EMPTY_MAP;
		}
		ObjectDistribution<String> phrases = getPhrases();
		Map<String, IDocument> trend2eg = new ArrayMap<String, IDocument>(
				phrases.size());
		for (String p : phrases.keySet()) {
			IDocument info = raw2eg.get(p);
			if (info==null) {
				Log.d(LOGTAG, "No eg doc for "+p);
				continue;
			}
			trend2eg.put(p, info);
		}
		return trend2eg;
	}

	@Override
	public IDocument getExample(String phrase) {
		return raw2eg.get(phrase);
	}

	@Override
	public boolean isReady() {
		return ! common1.isEmpty();
	}

	/**
	 * WARNING This will edit the distributions by pruning!
	 * @return
	 */
	@Override
	public ObjectDistribution<String> getPhrases() {
		if (common1.isEmpty()) {
			Log.d(LOGTAG, "Empty "+ReflectionUtils.getSomeStack(6));
			return new ObjectDistribution();
		}
		// Guard against not enough data 
		ObjectDistribution<String> common1Copy = new ObjectDistribution<>(common1);
		// It's not a phrase if it only occurs once
		common1.pruneBelow(1.1);
		if (common1.isEmpty()) {
			// ...but better to report something than a mysterious blank which looks buggy
			Log.d(LOGTAG, "Using "+common1Copy.getTotalWeight()+" solo-count items as prune-below-1.1 = empty");
			common1 = common1Copy;
		}
		common1.prune(2*n);
		common2.pruneBelow(1.1);
		common2.prune(2*n);
		common3.pruneBelow(1.1);
		common3.prune(2*n);

		ObjectDistribution<String> candidatePhrases = new ObjectDistribution();
		for (Map.Entry<String, Double> me : common1.asMap().entrySet()) {
			List<String> p1 = Arrays.asList(me.getKey());
			String k = getPhrases2(p1);
			candidatePhrases.setProb(k, me.getValue());
		}
		for (List<String> w : common2) {
			String k = getPhrases2(w);
			double v = common2.prob(w);
			v = v * LONGER_PHRASE_BONUS;
			candidatePhrases.setProb(k, v);
		}
		for (List<String> w : common3) {
			String k = getPhrases2(w);
//			System.out.println(k);
			double v = common3.prob(w);
			// A bit of a hack -- a 3 word phrase isn't quite such a boost on a 2 word one
			v = v * LONGER_PHRASE_BONUS * (LONGER_PHRASE_BONUS/2);
			candidatePhrases.setProb(k, v);
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
		ObjectDistribution<String> _phrases = new ObjectDistribution();
		String[] _candidatePhrases = candidatePhrases.toArray(new String[0]);
		while (_phrases.size() < n && ! candidatePhrases.isEmpty()) {
			String best = candidatePhrases.getMostLikely();
			double prob = candidatePhrases.prob(best);
			if (prob == 0) {
				break;
			}
//			assert best.split(" ").length < 5 : best;
			_phrases.setProb(best, prob);
			// remove it
			candidatePhrases.setProb(best, 0);
			// downgrade the remaining
			for (String p : _candidatePhrases) {
				if (overlap(best, p)) {
					double pv = candidatePhrases.prob(p);
					candidatePhrases.setProb(p, pv / overlapPenalty);
				}
			}
		}
		return _phrases;
	}

	/**
	 * ?? It would be more efficient to do this lazily.
	 * @param phrase The canonical internal phrase, e.g [dan, smell]
	 * @return The phrase to show, e.g. "Dan _smells!_"
	 */
	private String getPhrases2(List<String> phrase) {
		if (words2raw!=null) {
			String k = words2raw.get(phrase);
			if (k!=null) { // TODO fix this, but see issue #5421
//				// Remove dodgy punctuation (c.f. issue #4814)
//				k = StrUtils.removePunctuation(k);
				return k;
			}
		}
		String k = StrUtils.join(phrase, " ");
		return k;
	}

//	public Set<String> getStopWords() {
//		return stopwords;
//	}

	/**
	 * If true, skip this phrase
	 * @param phrase word in phrase
	 * @param rawForm 
	 * @return
	 */
	protected boolean ignorePhrase(List<String> phrase, String rawForm) {
		String[] bits = rawForm.split(" ");
		if (bits.length > 5) {
			return true;
		}
		return false;
	}

	/**
	 *
	 * @param p1
	 *            space-separated tokens
	 * @param p2
	 *            space-separated tokens
	 * @return true if p1 overlaps with p2
	 */
	boolean overlap(String p1, String p2) {
		String[] _p1 = p1.split(" ");
		String[] _p2 = p2.split(" ");
		for (String w : _p1) {
			for (String w2 : _p2) {
				if (w.equals(w2))
					return true;
			}
		}
		return false;
	}

	@Override
	public void resetup() {
		noTrainingDataCollection();
		HalfLifeMap<String, Double> map1 = new HalfLifeMap(10*n);
		HalfLifeMap<List<String>, Double> map2 = new HalfLifeMap(100*n);
		HalfLifeMap<List<String>, Double> map3 = new HalfLifeMap(1000*n);

		common1 = new ObjectDistribution<String>(map1, false);
		common2 = new ObjectDistribution<List<String>>(map2, false);
		common3 = new ObjectDistribution<List<String>>(map3, false);

		if ( ! trackExamples) {
			raw2eg = null; words2raw = null;
			return;
		}

		raw2eg = new HashMap();
		words2raw = new HashMap();
		// wire up pruners
		map1.addListener(new IPruneListener<String, Double>() {
			@Override
			public void pruneEvent(List<Entry<String, Double>> pruned) {
				for (Entry<String, Double> entry : pruned) {
					List<String> p1 = Arrays.asList(entry.getKey());
					String raw = words2raw.remove(p1);
					raw2eg.remove(raw);
				}
			}
		});
		IPruneListener<List<String>, Double> pruner = new IPruneListener<List<String>, Double>() {
			@Override
			public void pruneEvent(List<Entry<List<String>, Double>> pruned) {
				for (Entry<List<String>, Double> entry : pruned) {
					String raw = words2raw.remove(entry.getKey());
					raw2eg.remove(raw);
				}
			}
		};
		map2.addListener(pruner);
		map3.addListener(pruner);
		
		return;
	}

	/**
	 * 4 by default. Most users over-ride this! Should be higher when handling
	 * high-volume, lower for low-volume.
	 * If 1 or less, only single words are tracked.
	 */
	@Override
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
	 * ??How much to penalise phrases for overlapping with each other.
	 *
	 * @param i
	 */
	public void setOverlapPenalty(int i) {
		overlapPenalty = i;
	}

//	/**
//	 * By default, does not use any stopwords!
//	 *
//	 * @param stopwords
//	 */
//	public void setStopWords(Set<String> stopwords) {
//		this.stopwords = stopwords;
//	}

	@Override
	public void setTrackExamples(boolean b) {
		this.trackExamples = b;
		if ( ! trackExamples) {
			raw2eg = null; words2raw = null;
		}
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
		if (weight==0) return;
		String rawText = WebUtils2.getPlainText(x.getContents());
		// split on phrase boundaries, then get words
		SentenceStream sentencer = new SentenceStream();
		ITokenStream sentences = sentencer.factory(rawText);		
		for (Tkn s : sentences) {			
			ITokenStream tokens = tokeniser.factory(s.getText());
			List<Tkn> words = Containers.getList(tokens);
			train1b(x, rawText, words, weight);
		}
	}

	/**
	 * Sub-method of {@link #train1(IDocument, double)}.
	 * Use this directly if you already have a tokenisation.
	 * @param x
	 * @param rawText 
	 * @param words
	 * @param weight
	 */
	public void train1b(IDocument x, String rawText, List<Tkn> words, double weight) {
		train1c(x, rawText, words, 1, common1, weight);
		// If no bonus, then longer phrases will almost always lose to shorter ones.
		if (LONGER_PHRASE_BONUS<=1) return;
		// Strong weights should not be allowed to combine full-strength with the longer-phrase-bonus.
		// Otherwise the strong-weighted documents too easily win.
		double rweight = weight;
		if (weight>1) {
			rweight = 1 + (weight-1)*0.5;
		}
		// Train 2 word phrases
		train1c(x, rawText, words, 2, common2, rweight);
		// downgrade string weights again for 3-word-phrases
		if (rweight>1) {
			rweight = 1 + (rweight-1)*0.5;
		}
		// Train 3 word phrases
		train1c(x, rawText, words, 3, common3, rweight);
	}

	@SuppressWarnings("unchecked")
	private void train1c(IDocument x, String rawText, List<Tkn> words, int phraseLength, ObjectDistribution commonPhrases, double weight) {
		assert commonPhrases!=null;
		assert weight >= 0 : weight;
		// common 2-word phrases
		HashSet<List<String>> done = new HashSet();
		// Mimic the effect of StripXmlTokensier
//		if () we use a strip xml tokeniser??

		assert phraseLength!=1 || commonPhrases==common1;

		for(int i=0, in = 1 + words.size()-phraseLength; i < in; i++) {
			// Copy out the phrase
			final List<String> phrase = new ArrayList(phraseLength);
			assert phrase.size() <= phraseLength : phrase;
			for(int pi=0; pi<phraseLength; pi++) {
				Tkn w = words.get(i + pi);
				phrase.add(w.getText());
			}
			// Filter? Note: we've already stop-word filtered in the tokeniser (or if we haven't, that was a user setting)
			// Reverse map back to examples
			// TODO how can we get unsafe strings??
			int s = words.get(i).start;
			int e = words.get(i+phraseLength-1).end;
			if (e > rawText.length()) {
				Log.e(LOGTAG, "unsafe string: "+x);
			}
			String rawForm = StrUtils.substring(rawText, s, e); // ?? use String.substring once we can
			if (ignorePhrase(phrase, rawForm)) continue;
			if (oncePerDocument) {
				if (done.contains(phrase)) continue;
				done.add(phrase);
			}
			// Train
			if (phraseLength==1) {
				commonPhrases.train1(phrase.get(0), weight);
			} else {
				commonPhrases.train1(phrase, weight);
			}
			// Reverse map back to examples
			if (raw2eg!=null && rawForm != null) {
				words2raw.put(phrase, rawForm);
				raw2eg.put(rawForm, x);
			}
		}
	}

}


class PhraseInfo {

	public PhraseInfo(IDocument eg, String rawForm) {
		this.eg = eg;
		this.rawForm = rawForm;
	}

	IDocument eg;
	String rawForm;
}

