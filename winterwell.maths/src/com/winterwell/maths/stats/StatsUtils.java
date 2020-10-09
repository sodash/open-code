package com.winterwell.maths.stats;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.apache.commons.math3.stat.descriptive.moment.Variance;

import com.winterwell.maths.NumericalException;
import com.winterwell.maths.WinterwellMaths;
import com.winterwell.maths.datastorage.IDataSet;
import com.winterwell.maths.matrix.MatrixUtils;
import com.winterwell.maths.stats.distributions.ADistribution;
import com.winterwell.maths.stats.distributions.Gaussian;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.stats.distributions.d1.ADistribution1D;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ExtraDimensionsDataStream;
import com.winterwell.maths.timeseries.ExtraDimensionsDataStream.KMatchPolicy;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.vector.X;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Mutable;
import com.winterwell.utils.Mutable.Dble;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

import gnu.trove.set.hash.TIntHashSet;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.LowerSymmDenseMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

public class StatsUtils extends MathUtils {

	protected StatsUtils() { // static
		assert false;
	}
	
	// /**
	// * Value for when we fail to calculate co-variance: NaN
	// * This happens if too much data gets discarded, or if there wasn't
	// * enough to begin with.
	// * Warning NaN != NaN! test via Double.isNaN()
	// */
	// public static final double COVARIANCE_FAILURE = Double.NaN;

	/**
	 */
	private static final double UNSET = Double.NEGATIVE_INFINITY;

	/** true variance */
	private static final Variance variance = new Variance();

	/**
	 * marker wart for "this is a probability distribution".
	 * E.g. the field P_length is a distro for P(length=x)
	 */
	public static final String WART_PROB = "P_";

	static {
		variance.setBiasCorrected(false);
	}

	/**
	 * Pearson product-moment correlation coefficient. = covar(x,y) /
	 * (std-dev(x)*std-dev(y))
	 * 
	 * @return -1 to 1. 0 indicates no linear correlation, (-)1 indicates
	 *         perfect (anti)correlation.
	 */
	public static double correlation(double[] xs, double[] ys) {
		double covar = covar(xs, ys);
		double sdx = Math.sqrt(var(xs));
		double sdy = Math.sqrt(var(ys));
		return covar / (sdx * sdy);
	}

	/**
	 * Calculate the correlation coefficient between two time series streams.
	 * 
	 * @param x
	 * @param y
	 * @param matchingPolicy
	 *            Determines how to handle timestamp mismatches
	 * @param discardLevel
	 *            Can be null. Returns the max discards (as a [0-1] fraction)
	 *            from a stream. E.g. a value of 0.25 indicates that 25% of one
	 *            of the streams got discarded.
	 * @return covariance, or NaN if it could not be calculated
	 */
	public static double correlation(IDataStream x, IDataStream y,
			KMatchPolicy matchingPolicy, Mutable.Dble discardLevel) {
		// copy & paste code from covar() except for one line :(
		ExtraDimensionsDataStream xy = new ExtraDimensionsDataStream(
				matchingPolicy, Arrays.asList(x, y));
		ArrayList<Datum> xys = DataUtils.toList(xy, -1);
		x.close();
		y.close();
		if (xys.size() < 2)
			// can't calculate variance
			return Double.NaN;
		double[] discards = xy.getDiscardRatios();
		discardLevel.value = Math.max(discards[0], discards[1]);
		double[] xs = DataUtils.toArray(xys, 0);
		double[] ys = DataUtils.toArray(xys, 1);
		double cv = correlation(xs, ys);
		return cv;
	}

	/**
	 * 
	 * @param xs
	 *            Must be at least 2 elements
	 * @param ys
	 *            Must be at least 2 elements. Must have same length as xs.
	 * @return
	 */
	public static double covar(double[] xs, double[] ys) {
		assert xs.length == ys.length && xs.length > 1 : xs.length + " v "
				+ ys.length;
		double mx = mean(xs);
		double my = mean(ys);
		// build an array to use Jakarta's apparently more accurate 2-pass
		// algorithm
		double[] dx2s = new double[xs.length];
		for (int i = 0, n = xs.length; i < n; i++) {
			double x = xs[i], y = ys[i];
			dx2s[i] = (x - mx) * (y - my);
		}
		double cv = mean(dx2s);
		return cv;
	}

	/**
	 * Calculate the covariance matrix. Supports interruptions!
	 * 
	 * @param dataset
	 * @param matchingPolicy
	 *            Determines how to handle timestamp mismatches -- only relevant
	 *            if the dataset is several IDataStreams (with their own timestamps) joined together.
	 * @param tempStorage
	 *            Can be null. If not null, this file will be used to initialise
	 *            the matrix (if it exists), and the matrix will be saved to
	 *            this file after every row is calculated. This is *not* deleted
	 *            at the end of the run.
	 * @return covariance matrix If the calculation failed for a given cell,
	 *         this will be set to Double.NaN.
	 * @testedby  StatsUtilsTest#testCovar()}
	 */
	public static Matrix covar(IDataSet dataset, KMatchPolicy matchingPolicy,
			File tempStorage) {
		WinterwellMaths.init();
		DenseMatrix covar;
		if (tempStorage != null && tempStorage.isFile()
				&& tempStorage.length() != 0) {
			covar = FileUtils.load(tempStorage);
		} else {
			covar = new DenseMatrix(dataset.numColumns(), dataset.numColumns());
			// impossible variance value
			Arrays.fill(covar.getData(), UNSET);
		}
		File temp2 = FileUtils.createTempFile("temp2", "matrix");
		List<FailureException> exs = new ArrayList<FailureException>();
		boolean edits = false;
		for (int row = 0; row < dataset.numColumns(); row++) {
			Log.report("...covar row " + row);
			for (int col = row; col >= 0; col--) {
				double cv = covar.get(row, col);
				// already done?
				if (cv != UNSET) {
					continue;
				}
				// calculate
				edits = true;
				try {
					// do the work
					cv = covar2(dataset, row, col, matchingPolicy);

					assert row != col || cv >= 0 : cv;
					covar.set(row, col, cv);
					if (row != col) {
						covar.set(col, row, cv);
					}
				} catch (FailureException e) {
					Log.report(e.getMessage(), Level.WARNING);
					exs.add(e);
					covar.set(row, col, Double.NaN);
					if (row == col) {
						break; // quit early from this loop
					}
					covar.set(col, row, Double.NaN);
				}
			}
			if (tempStorage == null) {
				continue;
			}
			// save each row
			if (!edits) {
				Log.report("...no change row " + row);
				continue;
			}
			Log.report("...saving covar row " + row);
			// use a temp file to minimise the chance of a damaging interruption
			FileUtils.save(covar, temp2);
			FileUtils.move(temp2, tempStorage);
			edits = false;
		}
		// exceptions?
		if (!exs.isEmpty())
			// just throw the 1st one
			throw exs.get(0);
		// give 'em a DenseMatrix
		return covar.copy();
	}

	/**
	 * Calculate the covariance between two 1D time series streams.
	 * 
	 * @param x
	 * @param y
	 * @param matchingPolicy
	 *            Determines how to handle timestamp mismatches
	 * @param discardLevel
	 *            Can be null. Returns the max discards (as a [0-1] fraction)
	 *            from a stream. E.g. a value of 0.25 indicates that 25% of one
	 *            of the streams got discarded.
	 * @return covariance, or NaN if it could not be calculated
	 */
	public static double covar(IDataStream x, IDataStream y,
			KMatchPolicy matchingPolicy, Mutable.Dble discardLevel) {
		assert x.getDim() == 1 : x;
		assert y.getDim() == 1 : y;
		// Need to handle different time stamps: based on matching policy
		ExtraDimensionsDataStream xy = new ExtraDimensionsDataStream(
				matchingPolicy, Arrays.asList(x, y));
		ArrayList<Datum> xys = DataUtils.toList(xy, -1);
		x.close();
		y.close();
		if (xys.size() < 2)
			// can't calculate variance
			return Double.NaN;
		double[] discards = xy.getDiscardRatios();
		discardLevel.value = Math.max(discards[0], discards[1]);
		double[] xs = DataUtils.toArray(xys, 0);
		double[] ys = DataUtils.toArray(xys, 1);
		double cv = covar(xs, ys);
		return cv;
	}

	/**
	 * @param data
	 *            Must not be empty
	 * 
	 * @return The covariance between each dimension. Can contain NaN values if
	 *         there isn't enough data for a cell. Warning: slow -- O(n.d^2) --
	 *         for high dimensions!
	 * @testedby testCovarMatrix
	 */
	public static Matrix covar(Iterable<? extends Vector> data) {
		// get the data into a list
		List<Vector> xs = (List) Containers.getList(data);
		if (xs.isEmpty())
			throw new IllegalArgumentException("No data in " + data);
		int dim = xs.get(0).size();
		LowerSymmDenseMatrix matrix = new LowerSymmDenseMatrix(dim);
		for (int i = 0; i < dim; i++) {
			double[] xVals = DataUtils.get1DArr(xs, i);
			for (int j = 0; j <= i; j++) {
				double[] yVals = DataUtils.get1DArr(xs, j);
				assert i != j
						|| DataUtils.equals(new DenseVector(xVals), yVals);

				// avoid modifying xVals itself
				double[] _xVals, _yVals;
				// filter out 0,0 -- _not_ for the diagonal
				// if (skipZeroes) {
				// ArrayList xVals2 = new ArrayList();
				// ArrayList yVals2 = new ArrayList();
				// for(int k=0; k<xVals.length; k++) {
				// if (xVals[k]==0 && yVals[k]==0) {
				// continue;
				// }
				// xVals2.add(xVals[k]);
				// yVals2.add(yVals[k]);
				// }
				// _xVals = MathUtils.toArray(xVals2);
				// _yVals = MathUtils.toArray(yVals2);
				// } else {
				_xVals = xVals;
				_yVals = yVals;
				// }

				// no data?
				if (_xVals.length < 2) {
					matrix.set(i, j, Double.NaN); // :(
					continue;
				}

				// calculate this cell
				double cv = covar(_xVals, _yVals);

				// sanity check: variance is always +ive
				assert i != j || cv >= 0 : i + " " + j + " " + cv + " from "
						+ _xVals.length;

				matrix.set(i, j, cv);
			}
		}
		return matrix;
	}

	/**
	 * calculate one of the covariance values
	 * 
	 * @param dataset
	 * @param row
	 * @param col
	 * @return the covar for these dimensions
	 */
	static double covar2(IDataSet dataset, int row, int col,
			KMatchPolicy matchingPolicy) {
		// we could cache x and reset it... but let's delegate any
		// caching to the dataset
		IDataStream x = dataset.getDataStream1D(row);
		if (row == col) {
			double[] xs = DataUtils.toArray(x, 0);
			x.close();
			if (xs.length < 2)
				throw new FailureException("Not enough data for row " + row
						+ ": pts=" + xs.length);
			double v = var(xs);
			return v;
		}
		IDataStream y = dataset.getDataStream1D(col);
		Dble discardLevel = new Dble();
		double cv = covar(x, y, matchingPolicy, discardLevel);
		if (discardLevel.value > 0.2)
			throw new FailureException("Too much data discarded for " + row
					+ "," + col + ": " + Printer.toString(discardLevel));
		return cv;
	}

	public static IDistribution1D get1D(final IDistribution dist, final int dim) {
		return new ADistribution1D() {
			@Override
			public double density(double x) {
				// We could marginalise the other dims but that's non trivial
				// and should be done explicitly
				if (dist.getDim() != 1)
					throw new UnsupportedOperationException();
				return dist.density(new X(x));
			}

			@Override
			public double getMean() {
				return dist.getMean().get(dim);
			}

			@Override
			public double getStdDev() {
				return Math.sqrt(getVariance());
			}

			@Override
			public double getVariance() {
				return dist.getVariance().get(dim);
			}

			@Override
			public double prob(double min, double max) {
				// We could marginalise the other dims but that's non trivial
				// and should be done explicitly
				if (dist.getDim() != 1)
					throw new UnsupportedOperationException();
				return dist.prob(new X(min), new X(max));
			}

			@Override
			public Double sample() {
				return dist.sample().get(dim);
			}

			@Override
			public void setRandomSource(Random seed) {
				dist.setRandomSource(seed);
			}

		};
	}

	/**
	 * 
	 * @param covariance
	 * @param row
	 * @param col
	 * @return a value in [-1, 1]. Always 1 if row=col. Warning: Can be NaN if
	 *         one of the inputs is NaN!
	 */
	public static double getCorrelationCoeff(Matrix covariance, int row, int col)
			throws NumericalException {
		if (row == col)
			return 1;
		double sd1 = covariance.get(row, row);
		double sd2 = covariance.get(col, col);
		double cv = covariance.get(row, col);
		// Not a number? Note: sd1/sd2 could be NaN even if cv isn't due to
		// behaviour od "skipZeroes"
		if (Double.isNaN(cv) || Double.isNaN(sd1) || Double.isNaN(sd2))
			return Double.NaN;
		if (!MathUtils.isFinite(sd1) || !MathUtils.isFinite(sd2)
				|| !MathUtils.isFinite(cv))
			throw new NumericalException();

		// covariance can be -ive but var is always +ive
		assert sd1 >= 0 : sd1;
		assert sd2 >= 0 : sd2;

		// avoid numerical badness
		if (MathUtils.isTooSmall(sd1) || MathUtils.isTooSmall(sd2))
			return 0;

		// OK - calculate
		double corel = cv / (Math.sqrt(sd1) * Math.sqrt(sd2));

		// cap at [-1, 1] - this avoids some rounding errors
		if (Math.abs(corel) > 1) {
			// assert MathUtils.equalish(corel, 1) actually too big numbers also
			// cause error
			// || MathUtils.equalish(sd1, 0) || MathUtils.equalish(sd2, 0) :
			// corel+" from "+cv+" with "+sd1+","+sd2;
			corel = MathUtils.forceIn(corel, -1, 1);
		}
		assert corel >= -1 && corel <= 1 : corel;
		return corel;
	}

	public static LowerSymmDenseMatrix getCorrelationMatrix(Matrix covariance) {
		LowerSymmDenseMatrix corel = new LowerSymmDenseMatrix(
				covariance.numRows());
		for (MatrixEntry me : covariance) {
			double cor = getCorrelationCoeff(covariance, me.row(), me.column());
			corel.set(me.row(), me.column(), cor);
		}
		return corel;
	}

	public static IDistribution getND(final IDistribution1D base) {
		return new ADistribution() {
			@Override
			public double density(Vector x) {
				assert x.size() == 1;
				return base.density(x.get(0));
			}

			@Override
			public Matrix getCovar() {
				return MatrixUtils.asMatrix(base.getVariance());
			}

			@Override
			public int getDim() {
				return 1;
			}

			@Override
			public Vector getMean() {
				return DataUtils.asVector(base.getMean());
			}

			@Override
			public Vector getVariance() {
				return DataUtils.asVector(base.getVariance());
			}

			@Override
			public double prob(Vector topLeft, Vector bottomRight) {
				assert topLeft.size() == 1 && bottomRight.size() == 1;
				return base.prob(topLeft.get(0), bottomRight.get(0));
			}

			@Override
			public Vector sample() {
				double x = base.sample();
				return DataUtils.asVector(x);
			}

			@Override
			public void setRandomSource(Random seed) {
				base.setRandomSource(seed);
			}
		};
	}

	/**
	 * Calculate the log likelihood of the data given the distribution.
	 * 
	 * @param data
	 * @param dist
	 * @return The log likelihood of the data (quite probably infinite if any
	 *         data point has prob 0)
	 */
	public static double logLikelihood(List<? extends Vector> data,
			IDistribution dist) {
		double ll = 0;
		for (Vector vector : data) {
			ll += Math.log(dist.density(vector));
		}
		return ll;
	}

	/**
	 * Use {@link MathUtils#toArray(Collection)} to work with Lists.
	 * 
	 * @param xs
	 * @return the mean of the xs
	 */
	public static double mean(double[] xs) {
		return org.apache.commons.math3.stat.StatUtils.mean(xs);
	}

	/**
	 * @param pts
	 *            Must not be empty
	 * @return mean of points
	 * @testedby  StatsUtilsTest#testMean()}
	 */
	public static Vector mean(List<? extends Vector> pts) {
		int dim = pts.get(0).size();
		Vector mean = DataUtils.newVector(dim);
		// is this sparse?
		if (mean instanceof SparseVector) {
			for (Vector pt : pts) {
				mean.add(pt);
			}
			mean.scale(1.0 / pts.size());
			return mean;
		}
		// normal - each dim
		for (int i = 0; i < dim; i++) {
			double[] xs = DataUtils.get1DArr(pts, i);
			double m = mean(xs);
			mean.set(i, m);
		}
		return mean;
	}

	/**
	 * Calculate the average likelihood of the data points given the
	 * distribution. Can be used to judge how well a distribution matches the
	 * data. This is not as meaningful mathematically as we might hope, but it
	 * has the advantage over the true likelihood of not being very close to
	 * zero all the time.
	 * 
	 * @param data
	 * @param dist
	 * @return The average likelihood of the data
	 */
	public static double meanLikelihood(List<? extends Vector> data,
			IDistribution dist) {
		double sum = 0;
		for (Vector vector : data) {
			sum += dist.density(vector);
		}
		return sum / data.size();
	}

	/**
	 * Scale so that the vector values sum to 1. This is an L1 normalisation, as
	 * opposed to the usual Euclidean L2 normalisation. Since this method often
	 * used with the output of density functions and since these can return
	 * zeros and positive infinities, we try really hard to do something
	 * sensible with these cases. It's possible this should happen elsewhere.
	 * 
	 * @param vector
	 *            All values must be >=0
	 * @see DataUtils#normalise(Vector)
	 */
	public static void normaliseProbVector(double[] vector)
			throws FailureException {
		double total = 0;
		for (double d : vector) {
			assert d >= 0;
			// If we encounter an infinity try to do something vaguely sensible
			if (!MathUtils.isFinite(d)) {
				for (int i = 0; i < vector.length; i++) {
					vector[i] = MathUtils.isFinite(vector[i]) ? 0 : 1;
				}
				normaliseProbVector(vector);
				return;
			}
			total += d;
		}
		// Already a probability vector
		if (total == 1)
			return;
		// Total was zero -- treat as uniform
		if (total == 0) {
			// Printer.out("Warning! Normalising zero vector");
			for (int i = 0; i < vector.length; i++) {
				vector[i] = 1.0 / vector.length;
			}
			return;
		}
		// Total overflowed - try making everything smaller
		if (Double.isInfinite(total)) {
			for (int i = 0; i < vector.length; i++) {
				vector[i] = vector[i] / vector.length;
			}
			normaliseProbVector(vector);
			return;
		}
		// Regular case
		for (int i = 0; i < vector.length; i++) {
			vector[i] = vector[i] / total;
		}
	}

	public static List<Vector> sample(IDistribution dist, int n) {
		ArrayList<Vector> sample = new ArrayList<Vector>(n);
		for (int i = 0; i < n; i++) {
			sample.add(dist.sample());
		}
		return sample;
	}

	public static double[] sample(IDistribution1D dist, int n) {
		double[] sample = new double[n];
		for (int i = 0; i < n; i++) {
			sample[i] = dist.sample();
		}
		return sample;
	}

	/**
	 * @param xs
	 *            Must not be empty. All values must be finite
	 * @return the variance of xs. Returns 0 for a single-value (i.e. length =
	 *         1) sample.
	 *         <p>
	 *         Uses the true variance of the values, which is biased if using a
	 *         small sample to estimate the variance of the population. See
	 *         Jakarta's {@link Variance} for details.
	 * 
	 * 
	 * @testedby  StatsUtilsTest#testVar()}
	 */
	public static double var(double[] xs) {
		if (xs.length == 0)
			throw new IllegalArgumentException("empty array");
		// We're using the sample (i.e. biased) estimator...
		// if (xs.length == 1) throw new
		// IllegalArgumentException("1-element array = infinite variance");
		double v = variance.evaluate(xs);
		if (!MathUtils.isFinite(v)) {
			// was the infinity an overflow or an infinity in the data?
			for (double x : xs) {
				if (!MathUtils.isFinite(x))
					throw new IllegalArgumentException(
							"Infinite/NaN value in xs: " + x);
			}
			// No infinite value - guess it overflowed!
			// Report and cap at max
			Log.report("Variance overflowed", Level.FINE);
			return Double.MAX_VALUE;
		}
		return v;
	}

//	public static Vector var(IDataStream pts) {
//		if (pts.isEmpty())
//			throw new IllegalArgumentException("empty stream");
//		ListDataStream list = new ListDataStream(pts);
//		return var(list.getList());
//	}

	/**
	 * @param pts
	 *            Must not be empty
	 * @return the variance in each dimension.
	 * @testedby  StatsUtilsTest#testVar()}
	 */
	public static Vector var(List<? extends Vector> pts) {
		int dim = pts.get(0).size();
		Vector var = DataUtils.newVector(dim);

		// sparse?
		if (var instanceof SparseVector) {
			TIntHashSet used = new TIntHashSet();
			for (Vector v : pts) {
				for (VectorEntry ve : v) {
					used.add(ve.index());
				}
			}
			for (int i : used.toArray()) {
				// TODO this is still inefficient
				double[] xs = DataUtils.get1DArr(pts, i);
				double v = var(xs);
				var.set(i, v);
			}
			return var;
		}

		// normal - each dim
		for (int i = 0; i < dim; i++) {
			double[] xs = DataUtils.get1DArr(pts, i);
			double v = var(xs);
			var.set(i, v);
		}
		return var;
	}

	public static double weightedMean(double[] weights, double[] data) {
		assert weights.length == data.length;
		double total = 0, totalWeight = 0;
		for (int i = 0; i < weights.length; i++) {
			assert weights[i] >= 0;
			totalWeight += weights[i];
			total += weights[i] * data[i];
		}
		return total / totalWeight;
	}

	public static Vector weightedMean(double[] weights,
			List<? extends Vector> data) {
		assert weights.length == data.size() : weights.length + " vs "
				+ data.size();
		assert weights.length != 0;
		Vector mean = DataUtils.newVector(data.get(0).size());
		double total = 0;
		for (int i = 0; i < weights.length; i++) {
			Vector x = data.get(i);
			double w = weights[i];
			mean.add(w, x);
			total += w;
		}
		if (total == 0)
			throw new FailureException("No non-zero weights");
		mean.scale(1 / total);
		return mean;
	}

	/**
	 * 
	 * @param weights
	 *            Must be >= zero. Can be greater than 1
	 * @param data
	 * @return the variance of the data, with weighted contributions.
	 *         <p>
	 *         <b>Bias:</b> If this is a small sample, then this sample variance
	 *         will be a biased estimator of the true variance. The correction
	 *         formula is: var' = ((sum weights) / (sum weights - 1)) * var
	 * 
	 */
	public static double weightedVar(double[] weights, double[] data) {
		assert weights.length == data.length;
		double total = 0, totalSq = 0, totalWeight = 0;
		for (int i = 0; i < weights.length; i++) {
			double di = data[i];
			double wi = weights[i];
			assert wi >= 0;
			totalWeight += wi;
			total += wi * di;
			totalSq += wi * di * di;
		}
		double mean = total / totalWeight;
		double result = (totalSq / totalWeight) - (mean * mean);
		// NB Rounding errors can result in a negative value -- so we clamp
		// TODO implement a more accurate two-pass algorithm
		return result < 0 ? 0 : result;
	}

	public static Vector weightedVar(double[] weights,
			List<? extends Vector> data) {

		int dim = data.get(0).size();
		Vector var = DataUtils.newVector(dim);
		for (int i = 0; i < dim; i++) {
			double[] di = DataUtils.get1DArr(data, i);
			double vi = weightedVar(weights, di);
			assert vi >= 0 : vi;
			var.set(i, vi);
		}
		return var;
	}

	/**
	 * Scale values so that they sum to 1.
	 * 
	 * @param freqs This will be modified
	 * @return 
	 * @return true if OK, false if the total was <= 0 (hence cannot be normalised)
	 */
	public static boolean normalise(double[] freqs) {
		double total = MathUtils.sum(freqs);
		if (total <= 0) return false;
		if (total == 1) return true; // nothing to do
		double norm = 1.0 / total;
		for (int i = 0; i < freqs.length; i++) {
			freqs[i] = freqs[i] * norm;
		}
		return true;
	}

	/**
	 * Is it a Gaussian? Great. If not, approximate with a Gaussian.
	 * @param dist Must not be null
	 * @return May be dist or a new Gaussian
	 */
	public static Gaussian toGaussian(IDistribution dist) {
		assert dist!=null;
		if (dist instanceof Gaussian) return (Gaussian) dist;
		Vector m = dist.getMean();
		Matrix covar = dist.getCovar();
		return new Gaussian(m,covar);
	}

	public static double harmonicMean(double... xs) {
		double m1x = 0;
		for (double d : xs) {
			// 0 => 1/infinity later => 0
			if (d==0) return 0;
			m1x += 1/d;
		}
		return xs.length / m1x;
	}

	/**
	 * Find the median of an array of doubles. Partially sorts the array in-place.
	 * @param xs
	 * @return
	 */
	public static double median(double[] xs) {
		double index = (xs.length / 2.0) - 1;
		if (Math.floor(index) != index) {
			// odd number of elements
			return select((int) Math.ceil(index), xs);
		}
		// even number of elements
		double m1 = select((int) index, xs);
		double m2 = minOfSubRange(xs, (int) index + 1, xs.length);
		return (m1 + m2) / 2;
	}

	private static double minOfSubRange(double[] xs, int start, int end) {
		double min = Double.POSITIVE_INFINITY;
		for (int i = start; i < end; i++) {
			if (xs[i] < min) {
				min = xs[i];
			}
		}
		return min;
	}

	/**
	 * Find the index'th-lowest member of xs, counting from zero.
	 * So select(0, xs) == min(xs) and select(xs.length - 1, xs) == max(xs).
	 *
	 * This partially sorts the array in-place.
	 */
	public static double select(int index, double[] xs) {
		int lo = 0;
		int hi = xs.length;
		Random r = Utils.getRandom();
		while (hi - lo > 1) {
			assert lo <= index : "index " + index + " should be at least " + lo;
			assert index < hi : "index " + index + " should be less than " + hi;
			// Pick a random pivot, to prevent quadratic behaviour if the list is already sorted
			int pivot_index = r.nextInt(hi - lo) + lo;
			int mid = partition(xs, lo, hi, pivot_index);
			if (index < mid) {
				hi = mid;
			} else {
				lo = mid;
			}
		}
		return xs[lo];
	}

	/**
	 * Partition xs[lo:hi) about the pivot_index'th element.
	 * Afterwards, it looks like this:
	 *
	 *     [x0, x1, x2,..., pivot, y0, y1, y2...]
	 *
	 * where x0, x1 etc are all less than pivot, and y0, y1 etc are all >= pivot.
	 * You might think that [ x <= pivot ] and [ x > pivot ] is the right place to split,
	 * but then if the array is constant we'll loop forever.
	 *
	 * @return the index of the first element after pivot after partitioning (ie y0).
	 */
	private static int partition(double[] xs, int lo, int hi, int pivot_index) {
		double pivot = xs[pivot_index];
		// ensure xs[lo+1, hi) contains the elts to be partitioned
		xs[pivot_index] = xs[lo];
		int i = lo + 1;
		int mid = hi;
		while (i < mid) {
			// invariant: xs[mid:hi] is all >= pivot
			// invariant: xs[lo+1:i] is all < pivot
			// We do it this way round so if the array is constant, the pivot moves to the
			// bottom, and so the array always shrinks by at least one element.
			double x = xs[i];
			if (x >= pivot) {
				mid--;
				xs[i] = xs[mid];
				xs[mid] = x;
			} else {
				i++;
			}
		}
		assert i == mid;
		xs[lo] = xs[i-1];
		xs[i-1] = pivot;
		return mid;
	}

}
