package com.winterwell.maths.stats.distributions.discrete;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.containers.ArraySet;

/**
 * A true/false distribution.
 * 
 * @testedby {@link RandomChoiceTest}
 * @author daniel
 * @see Random01
 */
public final class RandomChoice implements IFiniteDistribution<Boolean> {

	private final double p;

	private Random random = Utils.getRandom();

	public RandomChoice() {
		this(0.5);
	}

	public RandomChoice(double pTrue) {
		p = pTrue;
		assert MathUtils.isProb(p) : p;
	}

	@Override
	public Boolean getMostLikely() {
		return p > 0.5;
	}

	@Override
	public List<Boolean> getMostLikely(int n) {
		return Arrays.asList(getMostLikely());
	}

	@Override
	public boolean isNormalised() {
		return true;
	}

	@Override
	public Iterator<Boolean> iterator() {
		return Arrays.asList(Boolean.TRUE, Boolean.FALSE).iterator();
	}

	@Override
	public double logProb(Boolean x) {
		return Math.log(prob(x));
	}

	@Override
	public void normalise() {
		return;
	}

	@Override
	public double normProb(Boolean x) {
		return prob(x);
	}

	@Override
	public double prob(Boolean x) {
		return x ? p : 1 - p;
	}

	@Override
	public Boolean sample() {
		return random.nextFloat() < p;
	}

	@Override
	public void setProb(Boolean obj, double value)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRandomSource(Random random) {
		this.random = random;
	}

	@Override
	public int size() {
		return 2;
	}

	@Override
	public Map<Boolean, Double> asMap() {
		final RandomChoice dist = this;		
		return new AbstractMap2<Boolean, Double>() {
			@Override
			public Double get(Object key) {
				return dist.prob((Boolean) key);
			}
			@Override
			public Double put(Boolean key, Double value) {
				double old = dist.prob(key);
				dist.setProb(key, value);
				return old;
			}
			@Override
			public Set<Boolean> keySet() throws UnsupportedOperationException {
				return new ArraySet(Boolean.TRUE, Boolean.FALSE); 
			}
		};
	}

	@Override
	public double getTotalWeight() {
		return 1;
	}

	public IDistribution1D as1D() {
		return new Dist1DFromDiscrete(this);
	}

	@Override
	public String toString() {
		return "RandomChoice[p=" + p + "]";
	}

}
