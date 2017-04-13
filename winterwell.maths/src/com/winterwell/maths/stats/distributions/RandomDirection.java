package com.winterwell.maths.stats.distributions;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.TodoException;

import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Vector.Norm;

/**
 * Generates normalised vectors in random directions.
 * 
 * @author Daniel
 * 
 */
public class RandomDirection extends ADistribution {

	private final int dim;

	public RandomDirection(int dim) {
		this.dim = dim;
		// save some memory
		noTrainingDataCollection();
	}

	@Override
	public double density(Vector x) {
		if (x.norm(Norm.Two) != 1)
			return 0;
		throw new TodoException();
	}

	@Override
	public int getDim() {
		return dim;
	}

	@Override
	public Vector getMean() {
		return DataUtils.newVector(dim);
	}

	@Override
	public Vector getVariance() {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public double prob(Vector minCorner, Vector maxCorner) {
		// TODO
		throw new TodoException();
	}

	/**
	 * A vector of length 1 pointing in a random direction.
	 */
	@Override
	public Vector sample() {
		Vector v = DataUtils.newVector(dim);
		for (int i = 0; i < dim; i++) {
			double vi = random().nextDouble() - 0.5;
			v.set(i, vi);
		}
		DataUtils.normalise(v);
		return v;
	}

}
