package com.winterwell.maths.stats.distributions;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.TodoException;

import no.uib.cipr.matrix.Vector;

/**
 * A rectangular block. The n-dimensional version of a uniform distribution.
 * 
 * @author Daniel
 * 
 */
public final class UniformCuboidDistribution extends ADistribution {

	/**
	 * ??Should we move this to VectorUtils?
	 * 
	 * @param x
	 * @param min
	 *            inclusive
	 * @param max
	 *            exclusive
	 * @return true if x is inside the hypercuboid
	 */
	static boolean isInside(Vector x, Vector min, Vector max) {
		assert x.size() == min.size() && x.size() == max.size();
		for (int i = 0; i < min.size(); i++) {
			assert min.get(i) <= max.get(i);
			// min is inclusive, max is exclusive
			if (x.get(i) < min.get(i))
				return false;
			if (x.get(i) >= max.get(i))
				return false;
		}
		return true;
	}

	private final double d;
	private final Vector max;

	private final Vector min;

	/**
	 * 
	 * @param min
	 *            inclusive
	 * @param max
	 *            exclusive
	 */
	public UniformCuboidDistribution(Vector min, Vector max) {
		assert min.size() == max.size();
		this.min = min;
		this.max = max;
		double volume = DataUtils.getVolume(min, max);
		d = volume == 0 ? Double.POSITIVE_INFINITY : 1.0 / volume;
	}

	@Override
	public double density(Vector x) {
		return isInside(x, min, max) ? d : 0;
	}

	@Override
	public int getDim() {
		return min.size();
	}

	@Override
	public Vector getMean() {
		Vector m = min.copy();
		m.add(max);
		m.scale(0.5);
		return m;
	}

	@Override
	public Vector getVariance() {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public double prob(Vector minCorner, Vector maxCorner) {
		assert minCorner.size() == maxCorner.size()
				&& minCorner.size() == min.size();
		double volume = 1;
		for (int i = 0; i < minCorner.size(); i++) {
			assert maxCorner.get(i) > minCorner.get(i);
			double maxi = Math.min(maxCorner.get(i), max.get(i));
			double mini = Math.max(minCorner.get(i), min.get(i));
			double w = maxi - mini;
			assert w >= 0 : minCorner + " " + maxCorner;
			volume *= w;
		}
		assert MathUtils.isFinite(d) : "TODO";
		return volume * d;
	}

	@Override
	public Vector sample() {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

}
