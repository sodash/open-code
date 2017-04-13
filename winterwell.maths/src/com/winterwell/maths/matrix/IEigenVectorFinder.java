package com.winterwell.maths.matrix;

import java.util.List;

import no.uib.cipr.matrix.Matrix;

/**
 * TODO which class to use when?
 * 
 * TODO an online lanczos method. As new data comes in, adjust the matrix (eg if
 * its a covariance matrix), then rehit the vectors a bit. Restart if badness.
 * 
 * TODO Iterated Restarted Lanczos method suitable for Hermitian matrices
 * http://en.wikipedia.org/wiki/Lanczos_algorithm
 * 
 * @author Daniel
 * 
 */
public interface IEigenVectorFinder {

	/**
	 * TODO can this modify the Matrix a?
	 * 
	 * Compute the largest few eigenpairs of a matrix NB The number of pairs
	 * returned will be all, unless over-ridden by
	 * {@link #setMaxEigenvectors(int)}.
	 * <p>
	 * Note: where people distinguish between left and right, these are right
	 * eigenvectors. Use a transposed matrix to get the left ones.
	 * 
	 * @param a
	 *            the problem matrix
	 * @return an ordered list of most to least significant eigenpairs
	 */
	public List<Eigenpair> getEigenpairs(Matrix a);

	/**
	 * Limit the number of eigenvectors that will be found. Often we are only
	 * interested in the first few eigenvectors.
	 * 
	 * @param max
	 */
	void setMaxEigenvectors(int max);

}
