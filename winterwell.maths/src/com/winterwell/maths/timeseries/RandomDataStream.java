package com.winterwell.maths.timeseries;

import java.util.List;

import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.GaussianBall;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

import no.uib.cipr.matrix.Vector;

/**
 * A data stream from sampling a distribution
 * 
 * @author daniel
 * @testedby  RandomDataStreamTest}
 */
public class RandomDataStream extends ADataStream {
	private static final long serialVersionUID = 1L;
	Time date;
	private final IDistribution dist;
	final private Dt dt;
	Object label;

	public RandomDataStream(IDistribution dist, Time start, Dt dt) {
		super(dist.getDim());
		this.dist = dist;
		date = start == null ? new Time() : start;
		this.dt = dt;
	}

	/**
	 * 
	 * @param dist
	 * @param start
	 *            Can be null, in which case the current time is used.
	 * @param dt
	 */
	public RandomDataStream(IDistribution1D dist, Time start, Dt dt) {
		this(StatsUtils.getND(dist), start, dt);
	}

	/**
	 * Convenience for quickly creating a data stream of the right
	 * dimensionality. Uses a Gaussian unit ball as its random point generator,
	 * and generates at 1 second intervals.
	 * 
	 * @param dim
	 *            Number of dimensions
	 */
	public RandomDataStream(int dim) {
		this(new GaussianBall(DataUtils.newVector(dim), 1), new Time(),
				new Dt(1, TUnit.SECOND));
	}

	/**
	 * @return the dist
	 */
	public IDistribution getDist() {
		return dist;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			Time myDate = date;

			@Override
			protected Datum next2() {
				Vector v = dist.sample();
				Datum d = new Datum(myDate, v, label);
				myDate = myDate.plus(dt);
				return d;
			}
		};
	}

	/**
	 * Equivalent to DataUtils.sample(this, numValues)
	 * 
	 * @param numValues
	 * @return sampled data
	 * @see DataUtils#sample(IDataStream, int)
	 */
	public final List<Datum> sample(int numValues) {
		return DataUtils.sample(this, numValues);
	}

	public void setLabel(Object label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + label + ":" + dist.toString();
	}

}
