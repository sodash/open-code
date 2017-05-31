package com.winterwell.maths.datastorage;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.winterwell.maths.stats.distributions.discrete.AFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.log.KErrorPolicy;

/**
 * ?? Or should this be a Distribution?? Probably!
 * @author daniel
 *
 * @param <K>
 * @param <V>
 */
public class PartialDistribution<T> implements IFiniteDistribution<T> {

	Map<T,Double> base;
	KErrorPolicy errorPolicy = KErrorPolicy.THROW_EXCEPTION;
	private boolean sealed;
	private int size;
	private double weight;
	
	public void setErrorPolicy(KErrorPolicy errorPolicy) {
		this.errorPolicy = errorPolicy;
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
