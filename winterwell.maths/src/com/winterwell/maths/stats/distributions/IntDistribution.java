package com.winterwell.maths.stats.distributions;

import java.util.Arrays;

import com.winterwell.maths.UnitGridInfo;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.d1.ADistribution1D;
import com.winterwell.maths.stats.distributions.d1.HistogramData;
import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Range;

/**
 * TODO refactor to be {@link HistogramData} with {@link UnitGridInfo}.
 * 
 * Assigns weight to (relatively few) positive integers (inc zero).
 * Uses an array covering 0 - max.
 * 
 * @see HistogramData
 * @author Daniel
 * 
 */
public final class IntDistribution extends ADistribution1D {

	private final double[] vector;

	public double getTotalWeight() {
		double tw = MathUtils.sum(vector);
		return tw;
	}
	
	/**
	 * 
	 * @param vector
	 *            The probability weight for each integer. This will be copied
	 *            and normalised.
	 */
	public IntDistribution(double[] vector) {
		this.vector = Arrays.copyOf(vector, vector.length);
		boolean ok = StatsUtils.normalise(this.vector);
		normalised = ok;
	}
	
	public IntDistribution(int[] vector) {
		this(MathUtils.toDoubleArray(vector));
	}

	/**
	 * Zero except at integer values which are infinite.
	 * 
	 * @Deprecated You probably want prob() instead.
	 */
	@Override
	@Deprecated
	// you probably want prob
	public double density(double x) {
		int i = (int) x;
		if (i != x || i < 0 || i >= vector.length)
			return 0;
		return Double.POSITIVE_INFINITY; //
	}

	public IDiscreteDistribution<Integer> getDiscreteDistribution() {
		throw new TodoException();
	}

	@Override
	public double getMean() {
		double m = 0;
		for (int i = 0; i < vector.length; i++) {
			m += i * vector[i];
		}
		return m;
	}

	@Override
	public Range getSupport() {
		return new Range(0, vector.length - 1);
	}

	@Override
	public double getVariance() {
		double m = getMean();
		double v = 0;
		for (int i = 0; i < vector.length; i++) {
			v += (i - m) * (i - m);
		}
		return v;
	}

	@Override
	public double prob(double min, double max) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	public double prob(int x) {
		if (x < 0 || x >= vector.length)
			return 0;
		return vector[x];
	}

	@Override
	public Double sample() {
		double cp = random().nextDouble();
		double sum = 0;
		for (int i = 0; i < vector.length; i++) {
			sum += vector[i];
			if (sum > cp)
				return Double.valueOf(i);
		}
		// Possible to get here, but should happen only with prob zero
		return 0.0;
	}

	public double[] toArray() {
		return vector;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + Printer.toString(vector);
	}

}
