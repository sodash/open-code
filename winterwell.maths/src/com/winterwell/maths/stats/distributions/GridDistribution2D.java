package com.winterwell.maths.stats.distributions;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.ITrainable;
import com.winterwell.maths.chart.Distribution2DChart;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.TodoException;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;

/**
 * A frequency-count distribution based on equal-sized n-dim buckets.
 * 
 * <h3>Lifecycle</h3>
 * <ol>
 * <li>Create
 * <li>Use {@link #count(double)} to add data.
 * <li> {@link #normalise()}. After this you cannot add more data.
 * <li>Use {@link #getMean()} and other functions.
 * </ol>
 * 
 * Plot with: {@link Distribution2DChart}
 * 
 * @author daniel
 */
public class GridDistribution2D extends ADistribution implements
		ITrainable.Unsupervised<Vector> {

	private final Matrix backing;

	private final GridInfo[] gridInfo;

	public GridDistribution2D(GridInfo xygrid) {
		this(xygrid, xygrid);
	}

	public GridDistribution2D(GridInfo xgrid, GridInfo ygrid) {
		gridInfo = new GridInfo[] { xgrid, ygrid };
		backing = new DenseMatrix(gridInfo[0].numBuckets,
				gridInfo[1].numBuckets);
	}

	/**
	 * 
	 * @param xy
	 *            If this is outside the grid, it is capped at the min or max
	 */
	public void count(double[] xy) {
		assert xy.length == 2;
		count(new XY(xy[0], xy[1]));
	}

	/**
	 * @param xy
	 *            If this is outside the grid, it is capped at the min or max
	 */
	public void count(Vector xy) {
		assert xy.size() == 2;
		assert !normalised;
		int r = gridInfo[0].getBucket(xy.get(0));
		int c = gridInfo[1].getBucket(xy.get(1));
		backing.add(r, c, 1);
	}

	/**
	 * May not be normalised!
	 */
	@Override
	public double density(Vector x) {
		int r = gridInfo[0].getBucket(x.get(0));
		int c = gridInfo[1].getBucket(x.get(1));
		return backing.get(r, c);
	}

	@Override
	public void finishTraining() {
		pleaseTrainFlag = false;
	}

	public Matrix getBackingData() {
		return backing;
	}

	@Override
	public Matrix getCovar() {
		assert isNormalised();
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public int getDim() {
		return 2;
	}

	@Override
	public Vector getMean() {
		assert isNormalised();
		XY mean = new XY(0, 0);
		for (MatrixEntry e : backing) {
			double w = e.get();
			XY v = getVector(e.row(), e.column());
			mean.add(w, v);
		}
		return mean;
	}

	@Override
	public Vector getVariance() {
		assert isNormalised();
		Vector mean = getMean();
		double[] var = new double[2];
		for (MatrixEntry e : backing) {
			double w = e.get();
			XY d = getVector(e.row(), e.column());
			d.add(-1, mean);
			var[0] += w * d.x * d.x;
			var[1] += w * d.y * d.y;
		}
		return new XY(var[0], var[1]);
	}

	private XY getVector(int row, int column) {
		double x = gridInfo[0].getBucketMiddle(row);
		double y = gridInfo[1].getBucketMiddle(column);
		return new XY(x, y);
	}

	@Override
	public boolean isReady() {
		return super.isReady();
	}

	@Override
	public void normalise() {
		if (normalised)
			return;
		int totalWeight = 0;
		for (MatrixEntry e : backing) {
			double x = e.get();
			assert x >= 0;
			totalWeight += x;
		}
		assert totalWeight != 0;
		for (MatrixEntry e : backing) {
			e.set(e.get() / totalWeight);
		}
		normalised = true;
	}

	@Override
	public void resetup() {
		super.resetup();
		backing.zero();
	}

	/**
	 * Sample from this distribution
	 * 
	 * @return an x selected by random weight
	 */
	@Override
	public Vector sample() {
		assert isNormalised();
		double p = random().nextDouble();
		double sum = 0;
		for (MatrixEntry e : backing) {
			sum += e.get();
			if (sum > p) {
				double x = gridInfo[0].getBucketBottom(e.row()) + 0.5
						* gridInfo[0].getBucketSize();
				double y = gridInfo[1].getBucketBottom(e.column()) + 0.5
						* gridInfo[1].getBucketSize();
				return new XY(x, y);
			}
		}
		// What? must be a rounding issue. Return anything
		assert p > 0.99 && p <= 1;
		return new XY(gridInfo[0].max, gridInfo[1].max);
	}

	@Override
	public void train(Iterable<? extends Vector> data) {
		for (Vector vector : data) {
			count(vector);
		}
	}

	@Override
	public void train1(Vector x) {
		count(x);
	}

	@Override
	public void train1(Vector x, Object tag, double weight) {
		super.train1(x, tag, weight);
	}
}
