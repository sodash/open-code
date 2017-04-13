package com.winterwell.maths.stats.distributions;

import java.util.List;
import java.util.Random;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.ITrainable.IHandleWeights;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.log.Log;

import no.uib.cipr.matrix.Vector;

/**
 * Take the cross-product of several 1D distributions.
 * 
 * This is trainable only if it's underlying components are.
 * 
 * @author daniel
 * 
 */
public class IndependentComponentsModel extends ADistribution implements
		ITrainable.Unsupervised<Vector>, IHandleWeights<Vector> {

	private final IDistribution1D[] basis;

	public IndependentComponentsModel(List<IDistribution1D> basis) {
		this.basis = basis.toArray(new IDistribution1D[0]);
	}

	@Override
	public double density(Vector x) {
		assert x.size() == basis.length;
		double p = 1;
		for (int i = 0; i < basis.length; i++) {
			double pi = basis[i].density(x.get(i));
			p *= pi;
		}
		return p;
	}

	@Override
	public void finishTraining() {
		for (IDistribution1D dist : basis) {
			if (dist instanceof ITrainable) {
				((ITrainable.Unsupervised<Vector>) dist).finishTraining();
			} else {
				Log.report("Not Trainable: " + dist);
			}
		}
	}

	@Override
	public int getDim() {
		return basis.length;
	}

	@Override
	public Vector getMean() {
		Vector mean = DataUtils.newVector(getDim());
		for (int i = 0; i < basis.length; i++) {
			double mi = basis[i].getMean();
			mean.set(i, mi);
		}
		return mean;
	}

	@Override
	public Vector getVariance() {
		Vector var = DataUtils.newVector(getDim());
		for (int i = 0; i < basis.length; i++) {
			double vi = basis[i].getVariance();
			var.set(i, vi);
		}
		return var;
	}

	@Override
	public boolean isReady() {
		for (IDistribution1D dist : basis) {
			if (dist instanceof ITrainable) {
				boolean ready = ((ITrainable) dist).isReady();
				if (!ready)
					return false;
			} else {
				Log.report("Not Trainable: " + dist);
			}
		}
		return true;
	}

	@Override
	public void resetup() {
		super.resetup();
		for (IDistribution1D dist : basis) {
			if (dist instanceof ITrainable) {
				((ITrainable) dist).resetup();
			} else {
				Log.report("Not Trainable: " + dist);
			}
		}
	}

	@Override
	public Vector sample() {
		Vector sample = DataUtils.newVector(getDim());
		for (int i = 0; i < basis.length; i++) {
			double si = basis[i].sample();
			sample.set(i, si);
		}
		return sample;
	}

	@Override
	public void setRandomSource(Random randomSrc) {
		for (IDistribution1D dist : basis) {
			dist.setRandomSource(randomSrc);
		}
	}

	@Override
	public void train(double[] weights, Iterable<? extends Vector> wdata) {
		for (IDistribution1D dist : basis) {
			if (dist instanceof IHandleWeights) {
				((IHandleWeights<Vector>) dist).train(weights, wdata);
			} else {
				if (dist instanceof ITrainable)
					throw new IllegalStateException(
							"Trainable, but not with weights: " + dist);
				Log.report("Not Trainable: " + dist);
			}
		}
	}

	@Override
	public void train(Iterable<? extends Vector> data) {
		for (IDistribution1D dist : basis) {
			if (dist instanceof ITrainable) {
				((ITrainable.Unsupervised<Vector>) dist).train(data);
			} else {
				Log.report("Not Trainable: " + dist);
			}
		}
	}

	@Override
	public void train1(Vector x) {
		for (IDistribution1D dist : basis) {
			if (dist instanceof ITrainable) {
				((ITrainable.Unsupervised<Vector>) dist).train1(x);
			} else {
				Log.report("Not Trainable: " + dist);
			}
		}
	}
}
