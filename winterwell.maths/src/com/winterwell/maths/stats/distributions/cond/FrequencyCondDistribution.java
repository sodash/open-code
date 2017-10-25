package com.winterwell.maths.stats.distributions.cond;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
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

	static final protected String GENERIC = "!generic";

	/**
	 * Override to use e.g. HalfLifeMap.
	 * @return a new Map
	 */
	protected final IFrequencyCondMapBuilder mapBuilder;
	
	protected final Map<C, ObjectDistribution<X>> dists;

	protected double pseudoCount = 2;

	public FrequencyCondDistribution() {
		this(HashMap::new);
	}

	/**
	 * @param newMap factory function used for the top-level context->marginal map, the
	 * secondary marginal->prob maps, and the "generic" map.
	 */
	public FrequencyCondDistribution(Supplier<Map> newMap) {
		this(new IFrequencyCondMapBuilder() {

			@Override
			public Map newDistributionMap() {
				return newMap.get();
			}

			@Override
			public Map newGenericMap() {
				return newMap.get();
			}

			@Override
			public Map newMarginalMap(Object context) {
				return newMap.get();
			}			
		});
	}

	/**
	 * @param mapBuilder factory object for the various Maps we need to construct
	 */
	public FrequencyCondDistribution(IFrequencyCondMapBuilder mapBuilder) {
		assert mapBuilder != null;
		this.mapBuilder = mapBuilder;
		dists = mapBuilder.newDistributionMap();
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
		return getCreateMarginal((C)GENERIC);
	}

	/**
	 * get/create marginal distro for context.
	 * @param context
	 * @return The distro which holds the data for this context -- edit to train!
	 */
	public ObjectDistribution<X> getCreateMarginal(C context) {
		ObjectDistribution<X> dist = dists.get(context);
		if (dist == null) {
			if (context == GENERIC) {
				dist = new ObjectDistribution<X>(mapBuilder.newGenericMap(), false);
			} else {
				dist = new ObjectDistribution<X>(mapBuilder.newMarginalMap(context), false);
			}
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
		ObjectDistribution<X> dist = getCreateMarginal(context);
		dist.train1(x, weight);
		// everything trains generic
		getGeneric().train1(x, weight);
	}

}
