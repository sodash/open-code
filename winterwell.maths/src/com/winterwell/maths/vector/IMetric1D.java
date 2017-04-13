package com.winterwell.maths.vector;

import no.uib.cipr.matrix.Vector;

/**
 * Defines distance on a 1D line. Plug in a different metric wherever this is
 * supported.
 * <p>
 * Target usage: clock time.
 * 
 * @author daniel
 * 
 */
public interface IMetric1D {

	/**
	 * @param x
	 * @return the canonical form of x, or just x if that doesn't make sense
	 *         here. <br>
	 *         Usage: e.g. to normalise on a clock system, -1 hour = 23 hours
	 */
	double canonical(double x);

	double dist(double a, double b);

	/**
	 * Embed the point into a higher-dimensional Euclidean space. E.g. time of
	 * day onto the unit circle (a la the clock face).
	 */
	Vector embed(double a) throws UnsupportedOperationException;

	/**
	 * Inverse of embed (many to one).
	 * 
	 * @param x
	 * @return the projection of x onto the embedding of this line.
	 * @throws UnsupportedOperationException
	 */
	double project(Vector x) throws UnsupportedOperationException;
}
