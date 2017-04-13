package com.winterwell.maths.stats.algorithms;

import java.util.Arrays;
import java.util.List;

import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.IVectorTransform;

import no.uib.cipr.matrix.Vector;

/**
 * Normalise the data so it has mean=0, variance=1
 * @author daniel
 *
 */
public class NormaliseTransform implements IVectorTransform {

	@Override
	public String toString() {
		return "NormaliseTransform [mean=" + mean + ", sd=" + Arrays.toString(sd) + "]";
	}

	private Vector mean;
	private double[] sd;

	public NormaliseTransform(List<? extends Vector> data) {
		// normalise it -- seems to improve but does not remove the numerical issues. ^Dan April 2016
		mean = StatsUtils.mean(data);
		Vector var = StatsUtils.var(data);
		sd = DataUtils.toArray(var);
		for (int i = 0; i < sd.length; i++) {
			sd[i] = Math.sqrt(var.get(i));
		}
	}

	public NormaliseTransform(Vector mean, double[] stdDev) {
		this.mean = mean;
		this.sd = stdDev;
	}

	@Override
	public Vector apply(Vector x) {
		Vector x2 = x.copy().add(-1, mean);
		for(int i=0; i<x2.size(); i++) {
			double x2i = sd[i]==0? 0 : x2.get(i) / sd[i];
			x2.set(i, x2i);
		}
		return x2;
	}

	@Override
	public double apply1D(double x) throws UnsupportedOperationException {
		assert mean.size() == 1 : mean.size();
		double x0 = x - mean.get(0);
		double nx0 = sd[0]==0? 0 : x0 / sd[0];
		return nx0;
	}
	
	@Override
	public IVectorFn inverse() {	
		return new IVectorFn() {
			@Override
			public Vector apply(Vector transformed) {
				Vector x2 = transformed.copy();
				for(int i=0; i<x2.size(); i++) {
					double x2i = sd[i]==0? 0 : x2.get(i) * sd[i];
					x2.set(i, x2i);
				}
				x2.add(mean);
				return x2;		
			}

			@Override
			public double apply1D(double x) throws UnsupportedOperationException {
				assert mean.size() == 1 : mean;
				double x2 = sd[0]==0? 0 : x * sd[0];
				return x2 + mean.get(0);
			}

		};
	}

}
