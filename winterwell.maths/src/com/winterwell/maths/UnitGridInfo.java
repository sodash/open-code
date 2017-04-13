package com.winterwell.maths;

public class UnitGridInfo implements IGridInfo {

	@Override
	public int getBucket(double x) {
		if (x<0) return 0;
		if (x >= max) return (int) (max-1);
		return (int) x;
	}

	@Override
	public double getBucketBottom(int bucket) {
		return bucket;
	}

	@Override
	public double getBucketMiddle(int bucket) {
		return bucket+0.5;
	}

	@Override
	public double getBucketTop(int bucket) {
		return bucket + 1;
	}

	@Override
	public double[] getEndPoints() {
		double[] sps = new double[size()];
		for (int i = 0; i < sps.length; i++) {
			sps[i] = i+1;			
		}
		return sps;
	}

	public UnitGridInfo(int max) {
		assert max > 0 : max;
		this.max = max;
	}
	
	final double max;
	
	@Override
	public double getMax() {
		return max;
	}

	@Override
	public double[] getMidPoints() {
		double[] sps = new double[size()];
		for (int i = 0; i < sps.length; i++) {
			sps[i] = i+0.5;			
		}
		return sps;
	}

	@Override
	public double getMin() {
		return 0;
	}

	@Override
	public double[] getStartPoints() {
		double[] sps = new double[size()];
		for (int i = 0; i < sps.length; i++) {
			sps[i] = i;			
		}
		return sps;
	}

	@Override
	public int size() {
		// e.g. max=0 is [0-1) with 1 bucket
		// max=1 is [0-1)[1,2) with 2 buckets
		return (int) getMax();
	}

	@Override
	public double getBucketWidth(int bucket) {
		return 1;
	}

}
