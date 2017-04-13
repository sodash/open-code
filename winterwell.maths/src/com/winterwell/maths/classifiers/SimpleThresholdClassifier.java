package com.winterwell.maths.classifiers;

import java.util.List;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.utils.MathUtils;

import gnu.trove.TDoubleArrayList;

/**
 * Classify two classes based on one threshold in one-dimensional data.
 * <p>
 * <h2>Algorithm</h2>
 * - Pass through, collect min, max, mean, var info for each class. - Pick a
 * grid size - Pass through, build cumulative count functions - Use bisection to
 * pick the best threshold bucket - TODO repeat within the chosen bucket using a
 * finer grid
 * 
 * <p>
 * Memory: O(k), where k is the grid size. <br>
 * Time: O(n + k) <br>
 * Weaknesses: This can be fooled into a local minimum (consider when the two
 * cumulative count functions keep alternating as to which is the highest). This
 * is unlikely except in situations where a threshold classifier would perform
 * badly anyway.
 * 
 * @author daniel
 */
public class SimpleThresholdClassifier<X> {

	private double errorRate;

	private int highLabel;

	Object[] labels = new Object[2];

	private int lowLabel;

	private double[] max;

	private double[] mean;

	private double[] min;

	private int NUM_BUCKETS = 1000;

	private double threshold;

	private int[] totals;

	private boolean trained;
	private double[] var;

	public X classify(Datum d) {
		assert trained;
		Object x = d.x() > threshold ? labels[highLabel] : labels[lowLabel];
		return (X) x;
	}

	/**
	 * 
	 * @param label
	 * @return min, max, mean, var, total for this label
	 */
	public double[] getDistributionInfo(Object label) {
		int lh;
		if (label.equals(labels[lowLabel])) {
			lh = lowLabel;
		} else {
			assert label.equals(labels[highLabel]) || labels[highLabel] == null : label;
			lh = highLabel;
		}
		return new double[] { min[lh], max[lh], mean[lh], var[lh], totals[lh] };
	}

	/**
	 * Saying threshold = bucket-bottom
	 * 
	 * @param bucket
	 * @param counts
	 * @param totals
	 * @return
	 */
	int getError(int bucket, int[][] counts, int[] totals) {
		int[] lowHigh = getError2(bucket, counts, totals);
		return Math.min(lowHigh[0], lowHigh[1]);
	}

	/**
	 * TODO can just do one error rate - we know which way to go by which count
	 * is higher
	 * 
	 * @param bucket
	 * @param totals
	 * @return [label-0 low, label-0 high]
	 */
	int[] getError2(int bucket, int[][] counts, int[] totals) {
		// Everything one label?
		if (bucket == 0)
			return new int[] { totals[0], totals[1] };
		int lowErrs, highErrs;
		{ // 0s low
			int true0s = counts[0][bucket - 1];
			int false0s = totals[0] - true0s;
			int false1s = counts[1][bucket - 1];
			lowErrs = false0s + false1s;
		}
		{ // 0s high
			int true1s = counts[1][bucket - 1];
			int false1s = totals[1] - true1s;
			int false0s = counts[0][bucket - 1];
			highErrs = false0s + false1s;
		}
		return new int[] { lowErrs, highErrs };
	}

	public double getErrorRate() {
		return errorRate;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setNumBuckets(int numBuckets) {
		this.NUM_BUCKETS = numBuckets;
	}

	public synchronized void train(List<Datum> data) {
		assert !trained;
		// Split by label
		double[][] splitData = train2_splitData(data);
		// Sort (not needed)
		// Arrays.sort(splitData[0]);
		// Arrays.sort(splitData[1]);
		// Collect info
		min = new double[2];
		max = new double[2];
		mean = new double[2];
		var = new double[2];
		totals = new int[2];
		for (int i = 0; i < 2; i++) {
			if (splitData[i].length == 0) {
				// one label just doesn't exist - make up a data point
				splitData[i] = new double[] { Double.MAX_VALUE };
				NUM_BUCKETS = 2; // cut down on the work!
			}
			min[i] = MathUtils.min(splitData[i]);
			max[i] = MathUtils.max(splitData[i]);
			mean[i] = StatsUtils.mean(splitData[i]);
			var[i] = StatsUtils.var(splitData[i]);
			totals[i] = splitData[i].length;
		}
		// Pick grid max, min, size
		// TODO use mean, var for when the outliers are misleading
		GridInfo gridInfo = new GridInfo(Math.min(min[0], min[1]), Math.max(
				max[0], max[1]), NUM_BUCKETS);
		// Train!
		train2(gridInfo, splitData);
		trained = true;
	}

	private void train2(GridInfo gridInfo, double[][] splitData) {
		// Build cumulative count functions
		int[][] counts = new int[2][];
		for (int i = 0; i < 2; i++) {
			counts[i] = train3_countFn(gridInfo, splitData[i]);
		}
		// Bisect to find the best split
		int top = gridInfo.numBuckets;
		int bottom = 0;
		int topError = getError(top, counts, totals);
		int bottomError = getError(bottom, counts, totals);
		while (top > bottom) {
			int bucket = (top + bottom) / 2;
			if (bucket == bottom) {
				break;
			}
			int error = getError(bucket, counts, totals);
			if (error < topError) {
				top = bucket;
				topError = error;
				continue;
			}
			bottom = bucket;
			bottomError = error;
		}
		//
		int best = topError > bottomError ? bottom : top;
		threshold = gridInfo.getBucketBottom(best);
		double N = splitData[0].length + splitData[1].length;
		errorRate = Math.min(bottomError, topError) / N;
		assert errorRate <= 0.5 : errorRate;
		// which way?
		int[] lowHighErrs = getError2(best, counts, totals);
		if (lowHighErrs[0] > lowHighErrs[1]) {
			lowLabel = 1;
			highLabel = 0;
		} else {
			lowLabel = 0;
			highLabel = 1;
		}
	}

	private double[][] train2_splitData(List<Datum> data) {
		TDoubleArrayList data0 = new TDoubleArrayList(
				(int) (data.size() * 0.75));
		TDoubleArrayList data1 = new TDoubleArrayList(
				(int) (data.size() * 0.75));
		for (Datum d : data) {
			if (d.getLabel() == null) {
				continue;
			}
			if (labels[0] == null) {
				labels[0] = d.getLabel();
			}
			if (labels[0].equals(d.getLabel())) {
				data0.add(d.x());
			} else {
				if (labels[1] == null) {
					labels[1] = d.getLabel();
				}
				data1.add(d.x());
			}
		}
		double[][] splitData = new double[2][];
		splitData[0] = data0.toNativeArray();
		splitData[1] = data1.toNativeArray();
		return splitData;
	}

	private int[] train3_countFn(GridInfo gridInfo, double[] values) {
		// Build "density" function
		int[] bucketCounter = new int[gridInfo.numBuckets];
		for (double x : values) {
			int b = gridInfo.getBucket(x);
			bucketCounter[b]++;
		}
		// Build cumulative function
		int[] sumCounter = new int[gridInfo.numBuckets];
		int sum = 0;
		for (int i = 0; i < bucketCounter.length; i++) {
			sum += bucketCounter[i];
			sumCounter[i] = sum;
		}
		assert sumCounter[sumCounter.length - 1] == values.length;
		return sumCounter;
	}

}
