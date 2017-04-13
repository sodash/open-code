package com.winterwell.maths.stats.distributions;



import java.io.Serializable;
import java.util.logging.Level;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.log.Log;

import no.uib.cipr.matrix.Vector;

/**
 * As MeanVar1D (which it uses), calculates the mean and variance. Does not hold
 * training data, so you can merrily stream gigabytes of data through it. Can be
 * the true-mean or be forgetful. Also stores the min and max it's seen. Note:
 * Does not apply the n/(n-1) adjustment to variance for sample bias. Note2: To
 * be absolutely clear, n-dimensional in this case means an input stream with n
 * values, not mean and var over an n-dimensional array.
 * <p>
 * Use via {@link #train1(Double)}
 * 
 * TODO covariance -- at the moment just computes a vector of simple variances
 * 
 * @author Alisdair, generalised from Dan's MeanVar1D.
 * 
 */
public class MeanVar extends ADistribution implements
		ITrainable.Unsupervised<Vector>, Serializable {

	/**
	 * As MeanVar1D, calculates the mean and variance. Does not
	 * hold training data, so you can merrily stream gigabytes of data through
	 * it. Can be the true-mean or be forgetful. Also stores the min and max
	 * it's seen. Note: Does not apply the n/(n-1) adjustment to variance for
	 * sample bias.
	 * <p>
	 * Use via {@link #train1(Double)}
	 * 
	 * TODO covariance -- at the moment just computes a vector of simple variances
	 * @author Alisdair, generalised from Dan's MeanVar1D.
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long count;
	private Integer dim;

	Vector lossFactor;
	private Vector max;
	private Vector mean;
	private Vector mean2;

	private Vector min;

	public MeanVar(final Integer _dim) {
		this.dim = _dim;
		this.mean = DataUtils.newVector(this.dim);
		this.mean2 = DataUtils.newVector(this.dim);
		this.max = DataUtils.filledVector(this.dim, Double.NEGATIVE_INFINITY);
		this.min = DataUtils.filledVector(this.dim, Double.POSITIVE_INFINITY);
		this.setLossFactor(0);
		this.resetup();
	}

	@Override
	public double density(Vector x) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void finishTraining() {
		super.finishTraining();
	}

	public Long getCount() {
		return this.count;
	}

	@Override
	public int getDim() {
		return this.dim;
	}

	public Vector getMax() {
		return this.max;
	}

	@Override
	public Vector getMean() {
		return this.mean;
	}

	public Vector getMin() {
		return this.min;
	}

	public Vector getStdDev() {
		return DataUtils.power(this.getVariance(), 0.5);
	}

	@Override
	public Vector getVariance() {
		return this.mean2.copy().add(DataUtils.power(this.mean, 2.0).scale(-1));
	}

	@Override
	public boolean isReady() {
		return count != 0;
	}

	@Override
	public void resetup() {
		super.resetup();
		this.count = 0L;
	}

	@Override
	public Vector sample() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param lossFactor
	 *            value in [0, 1). If zero (the default) this will compute the
	 *            true mean. If >0, then that amount will be "lost" each round,
	 *            thus favouring more recent values (makes it into a form of
	 *            low-pass filter). Values close to 1 will work, but be so lossy
	 *            as to make little sense.
	 * 
	 * */
	public final void setLossFactor(double _lossFactor) {
		if (_lossFactor < 0 || _lossFactor >= 1)
			throw new IllegalArgumentException();
		this.lossFactor = DataUtils.filledVector(this.dim, _lossFactor);
	}

	@Override
	public String toString() {
		return getMean().toString() + " ± " + getStdDev().toString();
	}

	@Override
	public void train(Iterable<? extends Vector> data) {
		super.train(data);
	}

	@Override
	public synchronized void train1(final Vector x) {
		if (x.size() != this.dim)
			throw new IllegalArgumentException(
					"Vector must be of same dimension specified in constructor");
		this.mean = train2_updateMean(x, this.mean);
		this.mean2 = train2_updateMean(DataUtils.elementWiseMultiply(x, x), this.mean2);
		this.min = DataUtils.elementWiseMin(x, this.min);
		this.max = DataUtils.elementWiseMax(x, this.max);
		// increment count
		this.count += 1;
		// Reduce count?
		if (this.count == Long.MAX_VALUE) {
			this.count = this.count / 2;
			Log.report("stream count reached max (reset to half)", Level.FINE);
		}
	}

	/**
	 * Called before incrementing count. Pure functional - does not modify
	 * anything!
	 * 
	 * @param d
	 *            will not be modified
	 * @param average
	 *            will not be modified
	 * @return new average
	 */
	final Vector train2_updateMean(final Vector d, final Vector average) {
		// Note: Use of Vector.copy() below is because Vector methods mutate
		// the vector they are called on.
		assert DataUtils.isFinite(d);
		Vector plus_one = DataUtils.filledVector(this.dim, 1);
		Vector count_as_vector = DataUtils.filledVector(this.dim,
				this.count.doubleValue());
		// Original 1D code left here for reference.
		// Vector a = ((1 - lossFactor) * count) / (count + 1);
		Vector one_minus_loss_factor = this.lossFactor.copy().scale(-1)
				.add(plus_one);
		Vector a_numerator = DataUtils.elementWiseMultiply(one_minus_loss_factor,
				count_as_vector);
		Vector a_denominator = count_as_vector.add(DataUtils.filledVector(this.dim, 1));
		Vector a = DataUtils.elementWiseDivide(a_numerator, a_denominator);
		assert DataUtils.isFinite(a) : a + " " + this.lossFactor + " " + this.count;
		// double a1 = 1 - a;
		Vector a1 = a.copy().scale(-1).add(plus_one);
		// average = a * average + a1 * d;
		Vector average_out = DataUtils.elementWiseMultiply(a, average).add(
				DataUtils.elementWiseMultiply(a1, d));
		assert DataUtils.isFinite(average_out);
		return average_out;
	}
}
