/**
 * 
 */
package com.winterwell.maths.matrix;

import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.maths.timeseries.DataUtils;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A crude alternative to eigenvector decomposition: Simply choose the
 * dimensions with the highest variance.
 * 
 * @author daniel
 * 
 */
public class CrudeDimensionReduction extends ATrainableBase<Vector, Object>
		implements ITrainable.Unsupervised<Vector> {

	private int dims;
	private int inputDim;

	private Matrix matrix;

	public CrudeDimensionReduction(int dims) {
		this.dims = dims;
	}

	@Override
	public void finishTraining() {
		if (dims >= inputDim) {
			matrix = new IdentityMatrix(inputDim);
			super.finishTraining();
			return;
		}

		Vector var = StatsUtils.var(trainingData);
		List<Integer> sorted = DataUtils.getSortedIndices(var, dims);
		// assert sorted.size() == dims; sparse vectors can be lower

		matrix = MatrixUtils.newMatrix(dims, inputDim);
		for (int i = 0; i < sorted.size(); i++) {
			Integer s = sorted.get(i);
			matrix.set(i, s, 1);
		}
		super.finishTraining();
	}

	public Matrix getMatrix() {
		return matrix;
	}

	@Override
	public boolean isReady() {
		return super.isReady();
	}

	@Override
	public void resetup() {
		super.resetup();
	}

	@Override
	public void train(Iterable<? extends Vector> data) {
		super.train(data);
	}

	@Override
	public synchronized void train1(Vector x) {
		super.train1(x);
		if (inputDim == 0) {
			inputDim = x.size();
		} else {
			assert inputDim == x.size() : inputDim + " vs " + x;
		}
	}

}
