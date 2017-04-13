package com.winterwell.maths;

/**
 * Info on a 1D grid. This is a "bucket selector". It uses uniformly sized buckets.
 * 
 * @testedby {@link GridInfoTest}
 * @author daniel
 * 
 */
public final class GridInfo implements IGridInfo {

	private final double gridSize;

	public final double max;

	public final double min;

	public final int numBuckets;

	public GridInfo(double min, double max, int numBuckets) {
		this.min = min;
		this.max = max;
		gridSize = (max - min) / numBuckets;
		this.numBuckets = numBuckets;
	}

	@Override
	public int getBucket(double x) {
		int i = (int) ((x - min) / gridSize);
		if (i < 0)
			return 0;
		i = Math.min(i, numBuckets - 1);
		return i;
	}

	@Override
	public double getBucketBottom(int bucket) {
		assert bucket <= numBuckets : bucket;
		return min + gridSize * bucket;
	}

	@Override
	public double getBucketMiddle(int bucket) {
		assert bucket <= numBuckets : bucket;
		return min + gridSize * (bucket + 0.5);
	}

	public double getBucketSize() {
		return gridSize;
	}
	
	@Override
	public double getBucketWidth(int bucket) {
		return getBucketSize();
	}

	@Override
	public double getBucketTop(int bucket) {
		assert bucket <= numBuckets : bucket;
		return min + gridSize * (bucket + 1);
	}

	@Override
	public double[] getEndPoints() {
		double[] pts = new double[numBuckets];
		for (int i = 0; i < pts.length; i++) {
			pts[i] = getBucketTop(i);
		}
		return pts;
	}

	@Override
	public double getMax() {
		return max;
	}

	@Override
	public double[] getMidPoints() {
		double[] pts = new double[numBuckets];
		for (int i = 0; i < pts.length; i++) {
			pts[i] = getBucketMiddle(i);
		}
		return pts;
	}

	@Override
	public double getMin() {
		return min;
	}

	@Override
	public double[] getStartPoints() {
		double[] pts = new double[numBuckets];
		for (int i = 0; i < pts.length; i++) {
			pts[i] = getBucketBottom(i);
		}
		return pts;
	}

	@Override
	public int size() {
		return numBuckets;
	}

	@Override
	public String toString() {
		return "GridInfo[" + min + ", " + max + " /" + numBuckets + "]";
	}

}