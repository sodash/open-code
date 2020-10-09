package com.winterwell.maths.stats.algorithms;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.matrix.MatrixUtils;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.log.Log;

import gnu.trove.list.array.TDoubleArrayList;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixSingularException;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * ?? Perhaps replace the MTJ innards with the Apache Commons implementation?
 * Although MTJ is likely faster.
 * 
 * Ordinary Least Squares. Fit a line by minimising least squares. This is
 * equivalent to assuming Gaussian noise on a linear model and finding the MLE.
 * <p>
 * Includes an <code>always 1</code> input for the offset as the last dimension.
 * 
 * <h3>Fitting Exponential Models</h3>
 * This can also be used to fit an exponential: If y = b.e^a.x, then we fit
 * ln(y) = a.x + ln(b) -- ie {@link #train1(Vector, Double)} with ln(y), then we
 * have a=gradient, b=exp(offset). However be warned: this can be very sensitive
 * to outliers.
 * 
 * <h3>Polynomial Regression Too</h3>
 * Given a polynomial regression model: y = a_0 + a_1 x + a_2 x^2 + a_3 x^3 +
 * ... <br>
 * Conveniently, such models are linear from the point of view of estimation,
 * since the regression function is linear in terms of the unknown parameters
 * a0, a1, ... <br>
 * Therefore, for least squares analysis, polynomial regression can be
 * completely addressed using multiple linear regression. This is done by
 * treating x, x2, ... as being distinct independent variables -- ie
 * {@link #train1(Vector, Double)} with vector=[x, x^2, x^3...]
 * 
 * Should we have an AConditionalDistribution class to be the base for this?
 * 
 * @author daniel
 * @testedby  LinearRegressionTest}
 */
public class LinearRegression implements IPredictor,
		ITrainable.Supervised<Vector, Double> {

	/**
	 * y = a.x (where x has an _always 1_ tail-dimension added to give offset)
	 */
	Vector a;

	/**
	 * the level of error
	 */
	private Gaussian1D noise;

	/**
	 * training data
	 */
	List<Vector> expRows = new ArrayList<Vector>();

	TDoubleArrayList targets = new TDoubleArrayList();

	private boolean resilient;

	private String pickVars;

	/**
	 * This includes dropped vars
	 */
	private int numExpVars;

	/**
	 * If true, the model can vary it's settings and data to get around issues.
	 * @param resilient
	 */
	public void setResilient(boolean resilient) {
		this.resilient = resilient;
	}

	public void finishTraining() {
		if (expRows.size() < expRows.get(0).size())
			// TODO ignore some dims if this happens? Create fake points?
			throw new FailureException("Not enough data: " + expRows.size() + "pts, "
					+ expRows.get(0).size() + "dims");

//		// screen linearly dependent variables?
//		if (filterTroublesomeData) {
//			// TODO Do we have any linearly dependent variables?
//			// Test here for the easy ones, and let the fail identify otherwise??
//			// Note: a constant vector will count as linearly dependent 'cos we add in our own constant
//		}
		
		DenseMatrix XX = null;
		try {
			// no doubt we could do this faster if we really understood MTJ
			int n = targets.size();
			int dimX = expRows.get(0).size() + 1; // add in an always 1 input for the
											// offset
			Vector yx = DataUtils.newVector(dimX);
			for (int i = 0; i < n; i++) {
				Vector rowi = expRows.get(i);
				double yi = targets.get(i);
				for (int j = 0; j < dimX - 1; j++) {
					yx.add(j, yi * rowi.get(j));
				}
				yx.add(dimX - 1, yi); // x[dimX-1] is always 1
			}
			assert DataUtils.isSafe(yx) : yx;
			// make design matrix from data
			Matrix X = new DenseMatrix(n, dimX);
			for (int i = 0; i < n; i++) {
				Vector rowi = expRows.get(i);
				assert DataUtils.isSafe(rowi) : i+" "+rowi;
				for (int j = 0; j < dimX - 1; j++) {
					double input_ij = expRows.get(i).get(j);
					X.set(i, j, input_ij);
				}
				// always 1 column for the offset
				X.set(i, dimX - 1, 1);
			}
			assert DataUtils.isSafe(X) : X;
			// make X times transpose
			XX = new DenseMatrix(dimX, dimX);
			X.transAmult(X, XX);
			assert DataUtils.isSafe(XX) : X;
			// invert
			Matrix invXX = MatrixUtils.invert(XX, resilient);
			assert DataUtils.isSafe(invXX) : XX;
//			IdentityMatrix I = new IdentityMatrix(dimX);
//			DenseMatrix invXX = new DenseMatrix(dimX, dimX);
//			XX.solve(I, invXX);

			// DenseMatrix I2 = new DenseMatrix(dimX,dimX);
			// invXX.mult(XX, I2);
			// assert MatrixUtils.equalish(I, I2);

			a = DataUtils.newVector(dimX);
			invXX.mult(yx, a);
			
			// get the error term
			double[] errs = new double[targets.size()];
			for (int i = 0; i < errs.length; i++) {
				double ti = targets.get(i);
				// skip missing data
				if ( ! MathUtils.isSafe(ti)) continue;
				double predicted = predict(expRows.get(i));
				assert MathUtils.isSafe(predicted);
				errs[i] = ti - predicted;
			}
			double var = StatsUtils.var(errs);
			// sanity check
			double meanErr = StatsUtils.mean(errs);
			if ( ! MathUtils.approx(meanErr, 0)) {
				// Looks like this can happen with the pseudo-inverse
				String m = "Regression failed?! data-count: " + expRows.size()
							+ " " + meanErr + " sd=" + Math.sqrt(var);				
				if (Math.abs(meanErr) > 1000) throw new FailureException(m);
				Log.d("LinearRegression", m);
			}
			noise = new Gaussian1D(0, var);
			
			// safety check
			assert DataUtils.isSafe(a) : a;
			// Are the weights "reasonable" compared with the input / target scale?
			// Is this a useful safety check??
			double[] _targets = targets.toArray();
			double min = MathUtils.min(_targets);
			double max = MathUtils.max(_targets);
			double range = max - min;
			Vector aabs = DataUtils.abs(a.copy());
			for (VectorEntry ve : aabs) {
				if (ve.get() > 1000000*range && ve.get() > 10000*max) {
					// This looks unstable
					throw new FailureException("solution looks unstable: "+a);
				}
			}
			
//			// ditch training data
			targets.clear();
			expRows.clear();
		} catch (MatrixSingularException e) {
			if ( ! resilient) {
				throw new FailureException(e+" data-count: " + expRows.size());
			}
			// try again with some noise?
			// Which is a crude way to make it non-singular, but it should work.
			Vector var = StatsUtils.var(expRows);
			int n = var.size();
			double[] jitter = new double[n];
			for(int d=0; d<n; d++) {
				jitter[d] = Math.sqrt(var.get(d)/100);
			}
			Gaussian1D gaussian = new Gaussian1D(0, 1);
			resilient=false; // don't loop
			try {				
				for(Vector v : expRows) {
					for(int d=0; d<n; d++) {
						double jit = jitter[d] * gaussian.sample();
						v.add(d, jit);
					}
				}
				// try again
				finishTraining();
			} catch (Exception ex) {
				resilient=true;
				throw new FailureException(e+" data-count: " + expRows.size());
			}
		}
	}

	public Gaussian1D getNoise() {
		return noise;
	}

	/**
	 * @return vector of weights used to predict. The last value is the offset.
	 */
	public Vector getWeights() {
		return a;
	}

	@Override
	public boolean isReady() {
		return a != null;
	}

	public double logDensity(double y, Vector x) {
		double py = predict(x);
		double e = y - py;
		double ld = noise.logDensity(e);
		assert MathUtils.equalish(ld, Math.log(noise.density(e)));
		return ld;
	}

	@Override
	public double predict(Vector predictors) {
		assert predictors.size() + 1 == a.size() : predictors.size()+1 +" (inc offset 1) vs expected: "+a.size();
		double py = 0;
		// size mismatch, so do the dot-product here
		for (int i = 0; i < predictors.size(); i++) {
			py += a.get(i) * predictors.get(i);
		}
		// add in the offset
		py += a.get(predictors.size());
		return py;
	}	

	@Override
	public void resetup() {
		targets.clear();
		expRows.clear();
		a = null;
		noise = null;
		numExpVars=0;
	}

	@Override
	public String toString() {
		return "LinearRegression[weights=" + Printer.toString(a) + " data="
				+ expRows.size() + "]";
	}

	@Override
	public void train1(Vector x, Double y, double weightIgnored) {
		if (numExpVars==0) {
			numExpVars = x.size();
		} else {
			assert numExpVars==x.size();
		}
		this.targets.add(y);
		this.expRows.add(x);
		assert MathUtils.isSafe(y) : y;
		assert DataUtils.isSafe(x) : x;
	}


	/**
	 * Instead of training and fitting, you could just set the weights from some other source.
	 * @param weights
	 */
	public void setWeights(Vector weights) {
		a = weights;
	}

}
