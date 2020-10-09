package com.winterwell.maths.stats.distributions.discrete;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.winterwell.utils.BestOne;
import com.winterwell.utils.MathUtils;

/**
 * Store log-probabilities to help avoid zeroes
 * @author daniel
 * @testedby  LogProbDistributionTest}
 * @param <T>
 */
public class LogProbDistribution<T> extends AFiniteDistribution<T> {

	private final Map<T, Double> backing = new HashMap();
	
	@Override
	public void setProb(T obj, double value) {
		setLogProb(obj, Math.log(value));
	}
	
	@Override
	public void normalise() throws UnsupportedOperationException {
		if (backing.isEmpty()) {
			return; // empty?!
		}
		// defend against very small numbers
		double max = MathUtils.max(backing.values());
		assert max <= 0 : backing;
		double sum = 0; // this is likely negative
		for(Double v : backing.values()) sum += Math.exp(v - max);
		double lnsum = Math.log(sum);
		for(T k : backing.keySet()) {
			double ov = backing.get(k);
			double v = ov - lnsum - max;
			backing.put(k, v);
		}	
		normalised = true;
	}
	
	@Override
	public Iterator<T> iterator() throws UnsupportedOperationException {
		return backing.keySet().iterator();
	}

	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public double prob(T x) {
		Double v = backing.get(x);
		return v==null? 0 : Math.exp(v);
	}
	
	@Override
	public double logProb(T x) {
		Double v = backing.get(x);
		return v==null? Double.NEGATIVE_INFINITY : v;
	}

	public void setLogProb(T item, double logp) {
		backing.put(item, logp);
	}
	
	@Override
	public T getMostLikely() {
		BestOne<T> best = new BestOne<T>();
		for (T t : this) {
			best.maybeSet(t, logProb(t));
		}
		return best.getBest();
	}

}
