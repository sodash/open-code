package com.winterwell.maths.stats.distributions.cond;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;

/**
 * A bag of {@link ObjectDistribution}s. context C -maps-to-> distribution-over-X
 * @author daniel
 *
 * @param <X>
 * @param <C>
 */
public class FrequencyCondDistribution<X, C> extends
		AFiniteCondDistribution<X, C> implements
		ITrainable.CondUnsupervised<C, X>, 
		/* We can view a conditional distribution P(x|context) as a supervised learner, f(context) -> tag:x */
		ITrainable.Supervised<C, X> 
{

	private static final Object GENERIC = new Object();

	/**
	 * Override to use e.g. HalfLifeMap. This is used for both the top-level context->marginal map, and the
	 * secondary marginal->prob map.
	 * @return a new Map
	 */
	protected final Supplier<Map> newMap;
	
	protected final Map<C, ObjectDistribution<X>> dists;

	private double pseudoCount = 2;

	public FrequencyCondDistribution() {
		this(HashMap::new);
	}
	
	/**
	 * Override to use e.g. HalfLifeMap. This is used for both the top-level context->marginal map, and the
	 * secondary marginal->prob map.
	 * @return a new Map
	 */
	public FrequencyCondDistribution(Supplier<Map> newMap) {
		this.newMap = newMap;
		assert newMap!=null;
		dists = newMap.get();
//		generic = new ObjectDistribution<X>(newMap.get(), false).setPseudoCount(pseudoCount);
	}
	
	public void setPseudoCount(double pseudoCount) {
		this.pseudoCount = pseudoCount;
//		generic.setPseudoCount(pseudoCount);
		dists.values().forEach(d -> d.setPseudoCount(pseudoCount));
	}

	@Override
	public void finishTraining() {
		super.finishTraining();
	}

	@Override
	public IFiniteDistribution<X> getMarginal(C context) {
		ObjectDistribution<X> dist = dists.get(context);
		if (dist == null)
			return getGeneric();
		return dist;
	}

	private ObjectDistribution<X> getGeneric() {
		// cheat and use erasure
		return getMarginal2((C)GENERIC);
	}

	private ObjectDistribution<X> getMarginal2(C context) {
		ObjectDistribution<X> dist = dists.get(context);
		if (dist == null) {
			dist = new ObjectDistribution<X>(newMap.get(), false);
			dist.setPseudoCount(pseudoCount);
			dists.put(context, dist);
		}
		return dist;
	}

	@Override
	public Collection<C> getPossibleCauses(X outcome) {
		return dists.keySet();
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public double probWithExplanation(X outcome, C context, ExplnOfDist explain) {
		IFiniteDistribution<X> dist = getMarginal(context);
		double p = dist.normProb(outcome);
		if (explain!=null) {
			// What to say?
			explain.set("marginal possibilities:"+dist.size()+" count for "+outcome+": "+dist.prob(outcome));
		}
		return p;
	}
	
	@Override
	public double prob(X outcome, C context) {
		IFiniteDistribution<X> dist = getMarginal(context);
		double p = dist.normProb(outcome);
		return p;
	}

	@Override
	public void resetup() {
		super.resetup();
		dists.clear();
//		generic.resetup();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + getGeneric().toString();
	}

	@Override
	public void train1(C context, X x, double weight) {
		ObjectDistribution<X> dist = getMarginal2(context);
		dist.train1(x, weight);
		// everything trains generic
		getGeneric().train1(x, weight);
	}

}
