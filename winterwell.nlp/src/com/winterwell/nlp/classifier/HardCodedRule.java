package com.winterwell.nlp.classifier;

import java.util.Collection;

import com.winterwell.utils.MathUtils;

import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.IDocument;

/**
 * TODO refactor CreoleClassifier.impossible to use this
 * Keep count of hits (active & right) & misses (active & wrong)
 * to allow merging the answers in with AndDist or similar.
 * @author daniel
 */
public abstract class HardCodedRule<T> extends ATextClassifier<T> {

	public void setSoften(boolean soften) {
		this.soften = soften;
	}
	
	protected int miss;
	protected int hit;

	/**
	 * If true (the default), soften the effect by the hit/miss ratio.
	 */
	boolean soften = true;
	
	protected Collection<T> tags;
	private String lang;
	
	public HardCodedRule(Collection<T> tags) {
		this.tags = tags;
	}
	
	/**
	 * *Can* return null for "no comment".
	 */
	@Override	
	public final IFiniteDistribution<T> pClassify(IDocument text) {
		if ( ! active(text)) return null;
		IFiniteDistribution<T> distro = pClassify2(text);
		if (distro==null) return null;
		if (soften) {
			return soften(distro);
		}
		return distro;
	}
	

	private IFiniteDistribution<T> soften(IFiniteDistribution<T> hard) {
		if (miss==0) return hard;
		if (hit==0) return null;
		// Should we take the low end of a confidence interval??
//		Range range = new AccuracyScore().getInterval(0.1, hit, miss+hit);		
		double effect = hit*1.0 / (hit+miss);
		double uniform = 1.0/tags.size();
		ObjectDistribution softened = new ObjectDistribution();
		for (T tag : tags) {
			double sp = effect*hard.prob(tag) + (1-effect)*uniform;
			softened.setProb(tag, sp);
		}
		return softened;
	}

	/**
	 * Called only if active() has returned true
	 * @param text
	 * @return distribution or null
	 */
	protected abstract IFiniteDistribution<T> pClassify2(IDocument text);
	
	/**
	 * @return [hits, misses]
	 */
	public double[] getScore() {
		return new double[]{hit,miss};
	}
	
	/**
	 * Convenience for {@link #train1(IDocument, Object, double)} with weight=1
	 * <p>
	 * If the rule matches this document, then adjust the hit/miss score.
	 * This is quite lenient -- it only counts as a miss if the rule gave near-zero weight to tag.
	 * But then, that fits how rules work.
	 */
	@Override
	public void train1(IDocument x, T tag) {
		train1(x, tag, 1);
	}
	
	
	/**
	 * If the rule matches this document, then adjust the hit/miss score.
	 * This is quite lenient -- it only counts as a miss if the rule gave near-zero weight to tag.
	 * But then, that fits how rules work.
	 */
	public void train1(IDocument x, T tag, double weight) {
		if (weight==0) {
			return;
		}
		assert weight > 0; 
		if ( ! active(x)) return;
		IFiniteDistribution<T> thinks = pClassify2(x);
		if (thinks==null) {			
			return; // odd -- the active() test should screen these cases out -- but no alarm
		}
		thinks.normalise();
		double p = thinks.prob(tag);
		// Count as a hit if the rule gives above-average weight to the tag
		if (p >= 1.0/thinks.size()) {
			hit += weight;
			return;
		}
		// Count as a miss if close to zero
		if (MathUtils.equalish(p, 0)) {
			// we got it wrong
			miss += weight;
		}		
	}
	
	/**
	 * @param x
	 * @return true if this rule is applicable to this document.
	 */
	abstract protected boolean active(IDocument x);

	@Override
	public void finishTraining() {		
	}

	public void setLang(String lang) {
		this.lang = lang;
	}
	
}
