package com.winterwell.maths.datastorage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.SingletonDistribution;
import com.winterwell.utils.log.KErrorPolicy;

/**
 * Holds part of a distribution -- typically only one key
 * 
 * Use-case: e.g. distributed distributions across a cluster.
 *  
 * @author daniel
 * @testedby  PartialDistributionTest}
 * @param <K>
 * @param <V>
 */
public class PartialDistribution<T> implements IFiniteDistribution<T> {

	Map<T,Double> base;
	KErrorPolicy errorPolicy = KErrorPolicy.THROW_EXCEPTION;
	private boolean sealed;
	/**
	 * unknown by default
	 */
	private int size = -1;
	private double weight;
	
	public PartialDistribution(Map<T,Double> base) {	
		this.base = base;
	}
	
	public PartialDistribution() {	
		base = new HashMap();
	}
	
	/**
	 * An immutable single-local-item distribution.
	 * 
	 * @see SingletonDistribution (which is not the same: that is a one-item distro, 
	 * whilst this is one item from a possibly larger distro).
	 * 
	 * @param item
	 * @param prob
	 * @param size
	 * @param totalWeight
	 */
	public PartialDistribution(T item, double prob, int size, double totalWeight) {
		base = Collections.singletonMap(item, prob);
		setSize(size);
		this.weight = totalWeight;
		setSealed(true);
	}
	
	public void setErrorPolicy(KErrorPolicy errorPolicy) {
		this.errorPolicy = errorPolicy;
	}
	
	public PartialDistribution<T> setSize(int size) {
		this.size = size;
		return this;
	}
	
	@Override
	public int size() {
		return size;
	}
	
	public double getTotalWeight() {
		return weight;
	}

	public void setSealed(boolean sealed) {
		this.sealed = sealed;
	}
	
	@Override
	public T getMostLikely() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double logProb(T x) {
		return Math.log(prob(x));
	}

	@Override
	public double normProb(T obj) {
		return prob(obj) / getTotalWeight(); 
	}

	@Override
	public double prob(T x) {
		Double v = base.get(x);
		if (v==null) {
			switch(errorPolicy) {
			case THROW_EXCEPTION: throw new IllegalArgumentException("This key is not known: "+x);
			}
		}
		return v;
	}

	@Override
	public void setProb(T obj, double value) throws UnsupportedOperationException {
		if (sealed) {
			if ( ! base.containsKey(obj)) {
				throw new IllegalArgumentException("Cannot add new key to sealed map: "+obj);
			}
		}
		base.put(obj, value);	
	}

	@Override
	public boolean isNormalised() {
		return false;
	}

	@Override
	public void normalise() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public T sample() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRandomSource(Random randomSrc) {
		// ignore
	}

	/**
	 * @deprecated You should probably avoid this! It is NOT the whole distribution.
	 */
	@Override
	public Map<T, Double> asMap() {
		return base;
	}

	@Override
	public List<T> getMostLikely(int n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
}
