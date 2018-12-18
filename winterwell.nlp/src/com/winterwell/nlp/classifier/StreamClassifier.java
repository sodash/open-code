package com.winterwell.nlp.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.cond.Cntxt;
import com.winterwell.maths.stats.distributions.cond.ExplnOfDist;
import com.winterwell.maths.stats.distributions.cond.ICondDistribution;
import com.winterwell.maths.stats.distributions.cond.IHasSignature;
import com.winterwell.maths.stats.distributions.cond.ISitnStream;
import com.winterwell.maths.stats.distributions.cond.ListSitnStream;
import com.winterwell.maths.stats.distributions.cond.Sitn;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.containers.TopNList;
import com.winterwell.utils.log.Log;

/**
 * Use Bayes theorem to classify an ISitnStream.
 * This takes a set of ICondDistribution models, and turns them into a classifier.
 * It applies (well, recommends) String tags.
 * 
 * @author daniel
 * @param <Tok> The type of stream token -- probably Tkn or String
 * @testedby {@link StreamClassifierTest}
 */
public class StreamClassifier<Tok> implements ITextClassifier<String>, IStreamClassifier<Tok> {

	final Map<String, ICondDistribution.WithExplanation<Tok, Cntxt>> models;

	IFiniteDistribution<String> prior;

	ISitnStream<Tok> tokeniser;

	/**
	 * 
	 * @param prior
	 * @return this
	 */
	public StreamClassifier<Tok> setPrior(IFiniteDistribution<String> prior) {
		this.prior = prior;
		return this;
	}
	
	public StreamClassifier(Map<String, ? extends ICondDistribution<Tok, Cntxt>> models) 
	{
		this(new ListSitnStream(new ArrayList()), models);
	}
	
	/**
	 * Create a StreamClassifier with these models & a uniform prior.
	 * 
	 * @param tokeniser Turns IDocuments into Sitns. Never null  -- enter a dummy with the right signature if using {@link #pClassify2(ISitnStream, List)}.
	 * @param models tag -to-> model
	 */
	public StreamClassifier(ISitnStream<Tok> tokeniser,
			Map<String, ? extends ICondDistribution<Tok, Cntxt>> models) 
	{
		assert ! models.isEmpty();
		this.tokeniser = tokeniser;
		this.models = (Map) models;
		
		// create a uniform prior which can be trained
		ObjectDistribution<String> _prior = new ObjectDistribution<String>();
		for (String tag : models.keySet()) {
			_prior.put(tag, 1);
		}
		_prior.normalise();
		prior = _prior;
		
		// safety check on signature: the models must be contained within what the stream makes
		String[] sig = tokeniser.getContextSignature();
		assert sig != null : tokeniser;
		for (String tag : models.keySet()) {
			ICondDistribution<Tok, Cntxt> model = models.get(tag);
			if (model instanceof IHasSignature) {
				String[] msig = ((IHasSignature) model).getContextSignature();
				Map<String, Object> ffs = ((IHasSignature) model).getFixedFeatures();
				for (String msi : msig) {
					// Is it a fixed feature? If so, it can be absent from the Sitns
					if (ffs!=null && ffs.get(msi)!=null) continue;
					assert Containers.contains(msi, sig) : Printer.toString(msig)+" v "+Printer.toString(sig)+" + "+ffs+" models["+tag+"] = "+model;
				}				
			}
		}
	}
	
	public void setTokeniser(ISitnStream<Tok> tokeniser) {
		this.tokeniser = tokeniser;
	}
	
	public ISitnStream<Tok> getTokeniser() {
		return tokeniser;
	}

	@Override
	public String classify(IDocument text) {
		IFiniteDistribution<String> dist = pClassify(text);
		return dist.getMostLikely();
	}

	/**
	 * Finish training on the prior and the models.
	 */
	@Override
	public final void finishTraining() {
		if (prior instanceof ITrainable) {
			((ITrainable) prior).finishTraining();
		}
		for (Object m : models.values()) {
			if (m instanceof ITrainable) {
				ITrainable tm = (ITrainable) m;
				tm.finishTraining();
			}
		}
	}

	// public so I can get at it in ClassifierTests.
	public ICondDistribution<Tok, Cntxt> getModel(String tag) {
		ICondDistribution<Tok, Cntxt> model = models.get(tag);
		assert model != null : tag+" "+models;
		return model;
	}
	
	@Deprecated // debugging use only?
	public Map<String, ICondDistribution<Tok, Cntxt>> getModels() {
		return Collections.unmodifiableMap((Map)models);
	}

	public final IFiniteDistribution<String> getPrior() {
		return prior;
	}

	public List<String> getTags() {		
		return Containers.getList(models.keySet());
	}

	@Override
	public final boolean isReady() {
		if (prior instanceof ITrainable 
				&& ! ((ITrainable)prior).isReady()) 
		{
			return false;
		}
		for (Object m : models.values()) {
			ITrainable t = (ITrainable) m;
			if (!t.isReady())
				return false;
		}
		return true;
	}

	@Override
	public final IFiniteDistribution<String> pClassify(IDocument text) {
		return pClassify(text, null);
	}

	/**
	 * How interesting should a Sitn be?
	 * [0,1] 0 = never skip, 1 = always skip
	 */
	double skipThreshold;
	
	int topTokensFocus = 20;
	/**
	 * normally null. If set, it will pick top-tokens based on maximising P(topTokensTag)
	 */
	String topTokensTag;
	
	/**
	 * normally null. If set, it will pick top-tokens based on maximising P(topTokensTag)
	 * @return this
	 */
	public StreamClassifier<Tok> setTopTokensTag(String topTokensTag) {
		this.topTokensTag = topTokensTag;
		return this;
	}

	/**
	 * If set (not 0), then only use the top situations -- those that give the highest entropy from the models.
	 * Why?
	 * To counteract several weak markers over-whelming a strong marker.
	 * To slightly counteract the double-counting effect of correlated terms including repeated terms.		
	 * Default: 20.
	 * @return this
	 */
	public StreamClassifier<Tok> setTopTokensFocus(int only_top_tokens) {
		this.topTokensFocus = only_top_tokens;
		return this;
	}
	
	@Override
	public final IFiniteDistribution<String> pClassify(IDocument text, ExplnOfDist tokenProbs) 
	{
		assert text != null;
		// break the document up into a stream of Situations
		ISitnStream<Tok> stream = tokenise(text);	
		
		return pClassify2(stream, tokenProbs);
	}

	/**
	 * Does the work! 
	 * Normally use {@link #pClassify(IDocument)} or {@link #pClassify(IDocument, List)}.
	 * You can call this directly if not using IDocuments.
	 * 
	 * @param expln Can be null. If not null the {@link ExplnOfDist#map()} is used, with Sitn.toString() keys
	 * @param stream
	 * @return
	 */
	@Override
	public IFiniteDistribution<String> pClassify2(ISitnStream<Tok> stream, ExplnOfDist expln) {
		assert prior!=null && prior.size() != 0 : this+" "+prior; assert stream!=null : this+" null stream";
		// Allow explanations to collect from several classifiers
//		assert expln == null || expln.tokenProbs==null || expln.tokenProbs.isEmpty() : expln;

		// Work with Arrays
		final String[] tags = Containers.getList(prior).toArray(new String[0]);
		// This is the main P(tag|observations) which we're calculating
		double[] p = new double[tags.length];
		ICondDistribution.WithExplanation<Tok, Cntxt>[] modls = new ICondDistribution.WithExplanation[tags.length];		
		// ...Initialise p from prior		
		for (int i=0; i<tags.length; i++) {
			String tag = tags[i];		
			p[i] = prior.prob(tag);
		}
		// ...Initialise models		
		for (int i=0; i<tags.length; i++) {
			String tag = tags[i];		
			modls[i] = models.get(tag);
		}	
				
		TopNList<Pair2<Sitn,double[]>> top_tokens = null;
		if (topTokensFocus>0) top_tokens = new TopNList<>(topTokensFocus);
		// go through the stream
		for (Sitn<Tok> sitn : stream) {			
			ExplnOfDist explainOneSitn = null;
			if (expln!=null) {
				explainOneSitn = new ExplnOfDist();
				explainOneSitn.skipped = true; // set all as skipped, and switch the selected few to false later
			}
			
			// What do we think of this sitn?
			// for each model, what chance of producing this outcome?			
			double[] tag2pSitn = pClassify4_oneSitn(sitn, tags, p, modls, explainOneSitn);		
			// sanity check: NaN is allowed for pass, infinity is not
			for (double ps : tag2pSitn) {
				assert ! Double.isInfinite(ps) : Printer.out(tag2pSitn, tags, sitn);
			}
			// If a tag-model says "Pass" (NaN), then P(a and b) = P(a) * average-other-models-P(b)
			// So that "Pass" has no up or down effect.			
			boolean[] nanMask = new boolean[tags.length];
			int nans=0;
			for(int i=0; i<tag2pSitn.length; i++) {
				if (Double.isNaN(tag2pSitn[i])) {
					nanMask[i] = true;
					nans++;
				}
			}
			// all passed?
			if (nans==tag2pSitn.length || nans==tag2pSitn.length-1) {
				continue;
			}
			
			// Skip un-interesting tokens, where the distribution is almost uniform.
			// Why? In longer documents (and they don't have to be very long at all),
			// this protects against lots-of-boring-tokens adding up to a supposedly strong result.
			double distFromUniform = Double.NaN;
			if (skipThreshold!=0) {
				distFromUniform = distanceFromUniform(tag2pSitn, nanMask, nans);
				if (distFromUniform < skipThreshold) {													
					continue;
				}				
			}
			
			// Do we want an explanation? Then save the step-by-step info
			if (expln != null) {
				// NB: avoid NaNs which would upset ObjectDistribution
				IFiniteDistribution<String> sitnDistro = new ObjectDistribution<String>();
				for(int i=0; i<tags.length; i++) {
					if (MathUtils.isFinite(tag2pSitn[i])) {
						sitnDistro.setProb(tags[i], tag2pSitn[i]);
					}
				}
				expln.tokenProbs.add(new Pair2<Sitn, IFiniteDistribution<String>>(sitn, sitnDistro));
				expln.map().put(sitn.toString(), explainOneSitn);
			}
			
			// Focus on the top-n tokens? Another way to skip less interesting tokens, better suited to longer docs.
			if (top_tokens!=null) {
				double top_token_score; 
				if (topTokensTag==null) {
					// calculate if we didn't already
					if (Double.isNaN(distFromUniform)) distFromUniform = distanceFromUniform(tag2pSitn, nanMask, nans);
					top_token_score = distFromUniform;
				} else {
					int tagi = Containers.indexOf(topTokensTag, tags);
					double ptagi = tag2pSitn[tagi];
					if (Double.isNaN(ptagi)) {
						continue; // the focal tag passed? Then skpi this token.
					}
					double total = 0;
					for(int i=0; i<tag2pSitn.length; i++) {
						if (nanMask[i]) continue;
						total += tag2pSitn[i];
					}
					top_token_score = ptagi / total;
				}
				top_tokens.maybeAdd(new Pair2(sitn, tag2pSitn), top_token_score); 
				continue; // do the incorporation and normalisation later
			}
			
			// incorporate and normalise
			double[] p2 = pClassify3(p, tag2pSitn);
			if (p2==null) {
				// impossible token! skip it
				continue;
			}			
			explainOneSitn.skipped = false;
			// Set P for the next token to be consumed
			p = p2;
		} // all tokens consumed
		
		if (top_tokens==null) {
			// done
			return new ObjectDistribution(tags, p);
		}
		
		// If focusing on the top-n -- now incorporate and normalise the top n
		double[] ptop = p; // This should be the prior probs. If top_tokens is set, p is not modified in the loop above.
		for (Pair2<Sitn, double[]> sitn_tag2pSitn : top_tokens) {
			double[] p2 = pClassify3(ptop, sitn_tag2pSitn.second);
			if (p2==null) {
				// impossible token! skip it
				continue;
			}
			ptop = p2;
			// explain it
			if (expln==null) continue;
			ExplnOfDist explain1sitn = expln.map().get(sitn_tag2pSitn.first.toString());
			if (explain1sitn!=null) explain1sitn.skipped = false;
			else {
//				System.out.println(sitn_tag2pSitn);
			}
		}	
		// done
		return new ObjectDistribution(tags, ptop);
	}

	

	/**
	 * Do posterior[tag] = p[tag] . tag2pSitn[tag], and normalise. Handles NaNs as pass (which gets an average score)
	 * @param p
	 * @param tag2pSitn
	 * @return null if all probs are zero
	 */
	private double[] pClassify3(double[] p, double[] tag2pSitn) {
		assert p.length == tag2pSitn.length;
		double[] p2 = new double[p.length];
		// P(a and b) = P(a) * P(b) assuming independence after
		// conditioning on the context.
		double totalNonNanP=0; int nonNan=0;
		for (int i=0; i<p.length; i++) {
			double p_tag = p[i];
			double pSitn_tag = tag2pSitn[i];
			if (Double.isNaN(pSitn_tag)) {
				continue;
			}
			// Does this ever happen?? Use an assert?
			if ( ! MathUtils.isProb(pSitn_tag)) {
				Log.e("maths", "not a prob! "+pSitn_tag+" "+this);
			}			
			double p2i = p_tag * pSitn_tag;
			nonNan++;
			totalNonNanP += pSitn_tag;
			p2[i] = p2i;
		}
		// If a tag-model says "Pass" (NaN), then P(a and b) = P(a) * average-other-models-P(b)
		if (nonNan!=p.length && nonNan!=0) {
			double ppass = totalNonNanP/nonNan;
			for (int i=0; i<p.length; i++) {				
				double pSitn_tag = tag2pSitn[i];
				if (Double.isNaN(pSitn_tag)) {
					double p_tag = p[i];
					double p2i = p_tag * ppass;
					p2[i] = p2i;
				}
			}
		}
		
		// Normalise as we go.
		// This helps avoids values going down to zero.
		// It's similar to using sum-of-log-probs.
		// But actual probs are more informative than log-probs for seeing what's going on.
		boolean ok = StatsUtils.normalise(p2);
		if ( ! ok) {
			// all zero! Return null
			return null;
		}
		return p2;
	}

	/**
	 * 
	 * What metric should we use here? Euclidean? KL divergence? Earth-mover's distance? Entropy?
	 * TODO probably entropy is the "right" metric, but anything sensible will do.
	 * @param tag2pSitn array of probabilities NB: This might not be normalised
	 * @param nans number of trues in nanMask
	 * @param nanMask true => this tag passed by returning nan 
	 * @return a measure in [0,1] of how non-uniform this is, where 1 is non-uniform.
	 */
	double distanceFromUniform(double[] tag2pSitn, boolean[] nanMask, int nans) {
		// This measures the Earth-mover's distance L1
		double total = 0;
		for(int i=0; i<tag2pSitn.length; i++) {
			if (nanMask[i]) continue;
			total += tag2pSitn[i];
		}
		if (total==0) return 0;
		// The uniform probability
		double u = 1.0 / (tag2pSitn.length - nans);
		double sum = 0;
		for(int i=0; i<tag2pSitn.length; i++) {
			if (nanMask[i]) continue;
			double d = tag2pSitn[i] / total;
			assert MathUtils.isFinite(d) : d;
			sum += Math.abs(d-u);
		}
		// NB: The max sum can be is 2. Because each discrepancy from uniform will have a mirror which doubles the effect.
		// Imagine the distro {a:1, b:0, c:0} - sum would be 4/3. Now add more elements... the limit is 2.
		sum = sum/2;
		return sum;
	}
	
	/**
	 * How interesting should a Sitn be?
	 * If the posterior for a word is too uniform, just ignore that word.
	 * This avoids small wibbles in the models adding up to confident assertions.
	 * @param skipThreshold in [0,1). 0 (the default) => never skip
	 * @return this
	 */
	public StreamClassifier<Tok> setSkipThreshold(double skipThreshold) {
		assert MathUtils.isProb(skipThreshold);
		// The current algorithm in distanceFromUniform() caps the max distance at (n-1)/n
		assert models.size()==1 || skipThreshold < (models.size()-1.0) / models.size() : skipThreshold;
		this.skipThreshold = skipThreshold;
		return this;
	}

	/**
	 * 
	 * @param sitn
	 * @param tags
	 * @param pPrevious Just used to skip tags which are known 0s. This method does not include the values from pPrevious in its output.
	 * @param modls
	 * @param explain 
	 * @return array of P(sitn|tag)
	 */
	private double[] pClassify4_oneSitn(Sitn<Tok> sitn, 
			String[] tags, double[] pPrevious, ICondDistribution.WithExplanation<Tok, Cntxt>[] modls, ExplnOfDist explain) 
	{
		double[] pSitn_tags = new double[tags.length];
		// for each model, what chance of producing this outcome?
		for (int i=0; i<tags.length; i++) {
			// skip the known zeroes
			if (pPrevious[i] == 0) continue;			
			ICondDistribution.WithExplanation<Tok, Cntxt> model = modls[i];
			if (model==null) {
				// So the classifier didn't have that model? This can happen
				// with the rules-based disqualification				
				continue;
			}
			ExplnOfDist modelExpln = explain==null? null : new ExplnOfDist();
			// What does this model think?  P(outcome | context & model)
			double pSitn_tag = model.probWithExplanation(sitn.outcome, sitn.context, modelExpln);			
			if (modelExpln!=null) {
				explain.map().put(tags[i], modelExpln);
			}
			// sanity check: NaN is allowed for "pass", infinity is not
			assert ! Double.isInfinite(pSitn_tag) : sitn+" "+model;			
			// Does this ever happen?? Use an assert?
			if ( ! Double.isNaN(pSitn_tag) && ! MathUtils.isProb(pSitn_tag)) {
//				double pbad = model.prob(sitn.outcome, sitn.context); // uncomment to step debug
				Log.e("maths", "not a prob! "+pSitn_tag+" model:"+model+" "+this);
				// clamp small errors - hopefully these are just numerical artifacts 
				if (pSitn_tag < 0 && pSitn_tag > -0.0000001) pSitn_tag = 0;
				else if (pSitn_tag > 1 && pSitn_tag < 1.0000001) pSitn_tag = 1;
			}
			pSitn_tags[i] = pSitn_tag;
		}
		return pSitn_tags;
	}

	
	public ISitnStream<Tok> tokenise(IDocument text) {
		Object input;
		if (tokeniser.getFactoryTypes().contains(IDocument.class)) {
			input = text;
		} else {
			assert tokeniser.getFactoryTypes().contains(String.class);
			input = text.getTitleAndContents();
		}
		ISitnStream<Tok> stream = tokeniser.factory(input);
		return stream;
	}

	/**
	 * This will re-setup both the prior and the models!
	 */
	@Override
	public void resetup() {
		// setup prior
		if (prior instanceof ITrainable) ((ITrainable) prior).resetup();
		double p = 1.0 / models.size();
		for (String tag : models.keySet()) {
			prior.setProb(tag, p);
		}
		// setup models
		for (Object m : models.values()) {
			ITrainable tm = (ITrainable) m;
			tm.resetup();
		}
	}

	/**
	 * Replace/add a model.
	 * 
	 * @Warning: this does not alter the prior.
	 * @param tag
	 * @param model
	 *            Can be null for "remove this model"
	 */
	protected void setModel(String tag, ICondDistribution.WithExplanation<Tok, Cntxt> model) {
		assert tag != null;
		if (model == null) {
			models.remove(tag);
		} else {
			models.put(tag, model);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [models=" + models
				+ ",\n\tprior=" + prior + "]";
	}

	/**
	 * Train the prior (if not null and trainable)
	 * and the model for tag.
	 * NB: Doesn't otherwise change the StreamClassifier itself.
	 */
	@Override
	public void train1(IDocument x, String tag, double weight) {
		if (prior instanceof ITrainable) {
			((ITrainable.Unsupervised<String>)prior).train1(tag);	
		}		

		// get the model for this tag
		ICondDistribution<Tok, Cntxt> model = getModel(tag);
		ITrainable.Supervised<Cntxt, Tok> tm = (com.winterwell.maths.ITrainable.Supervised<Cntxt, Tok>) model;

		// go through the stream
		Iterable<Sitn<Tok>> stream = tokeniser.factory(x.getContents());

		// for easier debugging
		stream = Containers.getList(stream);

		for (Sitn<Tok> sitn : stream) {
			tm.train1(sitn.context, sitn.outcome, weight);
		}
	}

}
