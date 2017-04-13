/**
 *
 */
package com.winterwell.maths.stats.distributions.discrete;

import java.util.Collections;
import java.util.Iterator;

import com.winterwell.maths.stats.distributions.d1.HistogramData;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.IntRange;
import com.winterwell.utils.containers.Range;

/**
 * A histogram over integers (actually they can be doubles too, which lets it implement {@link IDistribution1D}).
 * Backed by a HashMap via {@link ObjectDistribution}.
 * 
 * @see HistogramData
 * @author Joe Halliwell <joe@winterwell.com>
 * @testby {@link IntegerDistributionTest}
 */
public class IntegerDistribution extends ObjectDistribution<Double> implements
		IDistribution1D {

	private static final long serialVersionUID = 1L;

	private IntRange range;


	public final void count(int x) {
		train1((double) x);
	}

	/**
	 * This treats the probability mass as being spread across a unit-wide
	 * column. So density(x) = prob(round(x))
	 */
	@Override
	public double density(double x) {
		return prob(1.0 * Math.round(x));
	}

	@Override
	public double getConfidence(double totalWeight) {
		throw new TodoException();
	}

	/**
	 * @return the mean of this integer distribution. May be fractional.
	 */
	@Override
	public double getMean() {
		double mean = 0;
		double total = 0;
		for (Double i : this) {
			double weight = prob(i);
			total += weight;
			mean += i * weight;
		}
		if (total == 0)
			return 0; // Or throw an exception?
		return mean / total;
	}

	/**
	 * @return range if set, or the current [min, max] seen if not. null if
	 *         empty
	 */
	public IntRange getRange() {
		if (range != null)
			return range;
		if (isEmpty())
			return null;
		int a = MathUtils.min(keySet()).intValue();
		int b = MathUtils.max(keySet()).intValue();
		return new IntRange(a, b);
	}

	@Override
	public double getStdDev() {
		throw new TodoException();
	}

	@Override
	public Range getSupport() {
		assert ! isEmpty() : this;
		IntRange ir = getRange();
		return new Range(ir.low, ir.high);
	}

	@Override
	public double getVariance() {
		throw new TodoException();
	}

	/**
	 * iterates over ALL the numbers in the range (even the zero-prob ones) IN
	 * ORDER.
	 */
	@Override
	public Iterator<Double> iterator() {
		if (isEmpty())
			return Collections.EMPTY_SET.iterator();
		final IntRange _range = getRange();
		return new Iterator<Double>() {
			int current = _range.low;

			@Override
			public boolean hasNext() {
				return current <= _range.high;
			}

			@Override
			public Double next() {
				return (double) current++;
			}

			@Override
			public void remove() {
			}
		};
	}

	@Override
	public double prob(double min, double max) {
		throw new TodoException();
	}

	public final double prob(int x) {
		return prob((double) x);
	}

	/**
	 * Convenience for using {@link #setProb(Double, double)}
	 * 
	 * @param x
	 * @param p
	 */
	public void setProb(int x, double p) {
		Double d = (double) x;
		setProb(d, p);
	}

	/**
	 * If unset, the min/max seen will be used. Call this to make sure all the
	 * zero-prob elements are included (e.g. it's easy for zero to get lost).
	 * 
	 * @param range
	 */
	public void setRange(IntRange range) {
		this.range = range;
	}

	public void train1(int x) {
		Double d = (double) x;
		train1(d);
	}

}
