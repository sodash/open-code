package com.winterwell.maths.matrix;

import java.util.Collections;
import java.util.List;

import com.winterwell.maths.timeseries.DataUtils;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Vector.Norm;

/**
 * Computes 1 eigenvalue/vector of an arbitrary matrix using the Method of
 * Powers (take a random vector & twat it). Based on the exposition at
 * http://distance-ed.math.tamu.edu/Math640/chapter6/node4.html
 * 
 * @testedby {@link MOPTest}
 */
public class MOP implements IEigenVectorFinder {

	/**
	 * Compute the eigenvector corresponding to the greatest eigenvalue. If
	 * there is no greatest eigenvalue, or the matrix is not diagonalisable then
	 * throw an IllegalArgumentException.
	 * 
	 * @param m
	 *            the matrix in question
	 * @return an array containing the eigenpair with the largest eigenvalue
	 */
	// TODO: Consider the API of @see no.uib.cipr.matrix.EVD
	@Override
	public List<Eigenpair> getEigenpairs(Matrix m) {
		if (!m.isSquare())
			throw new IllegalArgumentException("Matrix must be square");
		assert m.numColumns() != 0;
		Vector u = new DenseVector(m.numColumns());
		Vector v = new DenseVector(m.numColumns());
		Matrices.random(u);
		DataUtils.normalise(u);

		int count = 0;
		double error = 1, last_error = 100000;
		double threshold = 0.00000001;
		double lambda = 1.0, last_lambda = 10; // For performance reasons these
												// are actually 1/eigenvalue

		while (error > threshold) {
			m.mult(u, v);

			double normaliser = 1 / v.norm(Norm.Two);
			v.scale(normaliser);
			lambda = v.dot(u) * normaliser;

			// The sequence of lambdas converges to the eigenvalue
			// as u converges to the eigenvector, so looking at the
			// difference of successive terms is a decent way to
			// judge when to stop
			error = Math.abs(lambda - last_lambda);
			last_lambda = lambda;

			if (error > last_error)
				throw new IllegalArgumentException(
						"Divergence detected (iteration " + count + ", error="
								+ error + ")");
			last_error = error;

			// Swap u and v
			Vector tmp = u;
			u = v;
			v = tmp;
			count++;
		}

		// Check that we have found an eigenvector
		m.mult(u, v);
		v.scale(lambda);
		// System.out.println("Count: " + count + "\nLambda: " + lambda);
		// System.out.println("U: " + u + "\nV: " + v);

		if (DataUtils.dist(u, v) < 0.1)
			return Collections.singletonList(new Eigenpair(u, 1 / lambda));
		throw new IllegalArgumentException(
				"Matrix is not diagonalisable or no dominant eigenvalue");

	}

	/**
	 * This class can only find 1 eigenvector
	 */
	@Deprecated
	@Override
	public void setMaxEigenvectors(int max) {
		assert max == 1 : max;
	}

}
