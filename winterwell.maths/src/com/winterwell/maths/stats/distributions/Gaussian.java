package com.winterwell.maths.stats.distributions;

import static com.winterwell.maths.matrix.MatrixUtils.multiply;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.matrix.MatrixUtils;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.TodoException;

import no.uib.cipr.matrix.DenseCholesky;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.UpperTriangDenseMatrix;
import no.uib.cipr.matrix.Vector;

/**
 * A full-blown multi-variate Gaussian.
 * 
 * @see http://en.wikipedia.org/wiki/Multivariate_normal_distribution
 * 
 */
public final class Gaussian extends ADistribution implements IGaussian,
		ITrainable.Unsupervised<Vector> {

	private final Matrix covar;
	private final DenseMatrix invCovar;
	private final Vector mean;
	private final Vector minusMean;
	private final double norm;
	private transient MultivariateNormalDistribution mnd;

	/**
	 * Construct a multi-variate Gaussian from another Gaussian. The two will
	 * not share any structure. This provides a convenient way of creating
	 * Gaussians via the more constrained versions (e.g. {@link GaussianBall}).
	 * 
	 * @param g
	 */
	public Gaussian(IGaussian g) {
		this(g.getMean().copy(), g.getCovar().copy());
	}

	
	
	@Override
	public String toString() {
		return "Gaussian["+mean+", covar=" + covar + "]";
	}



	/**
	 * Construct a new Gaussian.
	 * 
	 * @param mean
	 *            the mean vector
	 * @param covar
	 *            the covariance matrix, this must be SPD or things will behave
	 *            oddly
	 */
	public Gaussian(Vector mean, Matrix covar) {
		// Check arguments
		int size = mean.size();
		assert covar.isSquare() : "Covariance matrix is not square";
		assert covar.numRows() == size : "Mean and covariance have different dimensions";

		// Store arguments
		this.mean = mean;
		this.covar = covar;

		// Precompute some stuff that we'll need;
		this.minusMean = mean.copy().scale(-1.0);

		DenseCholesky cd = DenseCholesky.factorize(covar);
		if ( ! cd .isSPD()) {
			// No inverse or normalisation
			invCovar = null;
			norm = -1; 
			return;
		}

		this.invCovar = Matrices.identity(size);
		cd.solve(this.invCovar);

		// The determinant of the original matrix is the square product of the
		// diagonal entries
		UpperTriangDenseMatrix u = cd.getU();
		double detCovar = 1.0;
		for (int i = 0; i < size; i++) {
			detCovar *= u.get(i, i);
		}
		detCovar = detCovar * detCovar;
		this.norm = 1.0 / Math.sqrt(Math.pow(2 * Math.PI, size) * detCovar);

		// yes we are normalised
		normalised = true;
	}

	@Override
	public double density(Vector x) {
		x = x.copy(); // Don't destroy x
		x.add(minusMean);
		Vector y = x.copy();
		invCovar.mult(x, y);
		double a = x.dot(y);
		double exp = Math.exp(-0.5 * a);
		double p = exp * norm;
		return p;
	}

	@Override
	public void finishTraining() {
		throw new TodoException();
	}

	/**
	 * The covariance matrix -- without a copy; changes will "write through" to this Gaussian
	 */
	@Override
	public Matrix getCovar() {
		return covar;
	}

	@Override
	public int getDim() {
		return mean.size();
	}

	@Override
	public Vector getMean() {
		return mean;
	}

	@Override
	public Vector getVariance() {
		return MatrixUtils.getDiagonal(covar);
	}

	@Override
	public boolean isReady() {
		return super.isReady();
	}

	@Override
	public void resetup() {
		super.resetup();
		mnd = null;
	}

	@Override
	public Vector sample() {
		// Use Apache Commons implementation.
		// Which does an eigenvector breakdown of the covariance, samples each eigen-dimension, then matrix transforms the output.
//		Random rnd = random(); TODO
//		RandomGenerator rng = new JDKRandomGenerator(rnd.);
		if (mnd==null) {
			// NB: cache this for efficiency
			double[] means = DataUtils.toArray(getMean());
			double[][] covariances = DataUtils.toArray(getCovar());
			mnd = new MultivariateNormalDistribution(means, covariances);
		}
		double[] x = mnd.sample();
		return DataUtils.newVector(x);
	}

	@Override
	public void train(double[] weights, Iterable<? extends Vector> wdata) {
		super.train(weights, wdata);
	}

	@Override
	public void train(Iterable<? extends Vector> data) {
		super.train(data);
	}

	@Override
	public void train1(Vector x) {
		super.train1(x);
	}

	@Override
	public void train1(Vector x, Object tag, double weight) {
		super.train1(x, tag, weight);
	}

	/**
	 * The product of two Gaussian PDFs is itself Gaussian.
	 * Note: this is a product over the same underlying variables -- this is NOT the product of two separate random variables.
	 * @param B
	 * @return P(x) = P(this=x).P(B=x)
	 */
	public Gaussian product(Gaussian B) {
		// from http://www.tina-vision.net/docs/memos/2003-003.pdf
		// let C = A.B
		// then Varc^-1 = Vara^-1 + Varb^-1
		// and c = Varc (Vara^-1.a + Varb^-1.b)
		throw new TodoException();
//		return new Gaussian(c, varc);
	}

	/**
	 * @param A
	 * @return a new Gaussian, for A applied to this 
	 */
	public Gaussian apply(Matrix A) {
		// NB: These mean and covar formulae hold for any distribution, not just Gaussians...
		// A.x
		Vector newMean = MatrixUtils.apply(A, getMean());
		// A.V.A^t
		Matrix newCovar = MatrixUtils.newMatrix(getDim(), getDim());		
		Matrix AX = multiply(A, getCovar());		
		AX.transBmult(A, newCovar);
		// ...But "still a Gaussian" is specific to Gaussians
		return new Gaussian(newMean, newCovar);
	}
}
