package com.winterwell.utils.containers;

import java.io.Serializable;

/**
 * A closed real (double-valued) range.
 * 
 * @see IntRange
 * @author Daniel
 * 
 */
public final class Range implements Serializable, Comparable<Range> {
	public static final Range REALS = new Range(Double.NEGATIVE_INFINITY,
			Double.POSITIVE_INFINITY);

	private static final long serialVersionUID = 1L;

	public final double high;
	public final double low;

	/**
	 * Create a new Range. The inputs do not need to be in order. The inputs do
	 * not need to be finite (but NaN is not allowed)
	 */
	public Range(double a, double b) {
		assert !Double.isNaN(a) && !Double.isNaN(b) : a + ", " + b;
		if (a < b) {
			low = a;
			high = b;
		} else {
			low = b;
			high = a;
		}
	}

	/**
	 * Sort of a lexicographic compare: first low-to-low, then high-to-high.
	 * E.g. we get the ordering: [0,10], [1,2], [1,3]
	 * <p>
	 * If the ranges are disjoint or a partition, this will produce the
	 * intuitive order, e.g. [0,1], [1,2], [2, 4], [4,5], etc.
	 * 
	 */
	@Override
	public int compareTo(Range o) {
		if (low == o.low) {
			if (high == o.high)
				return 0;
			return high < o.high ? -1 : 1;
		}
		return low < o.low ? -1 : 1;
	}

	/**
	 * Is x inside this range? Includes the end values.
	 */
	public boolean contains(double x) {
		return x >= low && x <= high;
	}

	/**
	 * Like union, but not quite, as the union of two Ranges might not be a Range. 
	 * The max() of two ranges contains both (and potentially the gap in between, which is how it differs from union).
	 * 
	 * @param b
	 * @return [min(), max()]
	 */
	public Range max(Range b) {
		return new Range(Math.min(low, b.low), Math.max(high, b.high));
	}

	/**
	 * @param x
	 * @return [low+x, high+x]
	 */
	public Range plus(double x) {
		return new Range(low + x, high + x);
	}

	/**
	 * aka width
	 * 
	 * @return high - low
	 */
	public double size() {
		return high - low;
	}

	public Range times(double x) {
		return new Range(low * x, high * x);
	}

	@Override
	public String toString() {
		return "[" + low + "," + high + "]";
	}

	/**
	 * @param x
	 * @return x, capped to fall inside range (so x if inside, high if too-high,
	 *         low if too-low)
	 */
	public double cap(double x) {
		return Math.min(high, Math.max(x, low));
	}
}
