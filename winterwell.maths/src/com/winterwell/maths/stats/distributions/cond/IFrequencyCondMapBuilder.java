package com.winterwell.maths.stats.distributions.cond;

import java.util.Map;

/**
 * @author miles
 * 
 * Interface to be implemented by factory objects for FrequencyCondDistribution,
 * which needs to be able to construct three possibly distinct types of Map:
 * 
 *  - a top-layer map from contexts to marginal maps
 *  - a "generic" map for cases where we don't know the marginal distribution
 *  - individual marginal distributions.
 *  
 *  Yes, I know this is hilarious overkill.
 */
public interface IFrequencyCondMapBuilder<C> {
	
	public Map newDistributionMap();
	public Map newGenericMap();
	public Map newMarginalMap(C context);
}
