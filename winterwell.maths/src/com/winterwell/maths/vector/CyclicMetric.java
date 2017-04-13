package com.winterwell.maths.vector;

import no.uib.cipr.matrix.Vector;

/**
 * A metric which loops, so 24 hours = 0 hours for example.
 * 
 * @testedby {@link CyclicMetricTest}
 * @author daniel
 * 
 */
public class CyclicMetric implements IMetric1D {

	final double max;

	/**
	 * 
	 * @param max
	 *            E.g. 24 for a 24 hour clock with hours as the unit.
	 */
	public CyclicMetric(double max) {
		this.max = max;
	}

	/**
	 * make positive and modulo
	 */
	@Override
	public double canonical(double a) {
		a = a % max;
		if (a < 0) {
			a = max + a;
		}
		return a;
	}

	@Override
	public double dist(double a, double b) {
		a = canonical(a);
		b = canonical(b);
		if (a == b)
			return 0;
		// find the higher
		double h, l;
		if (a > b) {
			h = a;
			l = b;
		} else {
			h = b;
			l = a;
		}
		double d1 = h - l;
		double d2 = l + max - h;
		return Math.min(d1, d2);
	}

	@Override
	public Vector embed(double a) throws UnsupportedOperationException {
		// make positive and modulo
		a = canonical(a);
		double r = a / max;
		r = r * 2 * Math.PI;
		return new XY(Math.sin(r), Math.cos(r));
	}

	public double getPeriod() {
		return max;
	}

	@Override
	public double project(Vector x) throws UnsupportedOperationException {
		assert x.size() == 2;
		double v = Math.atan2(x.get(0), x.get(1));
		double v2 = v / (2 * Math.PI);
		v2 *= max;
		if (v2 < 0) {
			v2 = max + v2;
		}
		return v2;
	}

}
