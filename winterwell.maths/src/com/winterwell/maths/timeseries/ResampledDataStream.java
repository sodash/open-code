package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Increase/decrease the representatives depending on class. A slightly crude
 * form of resampling-to-alter-class-distribution. Can cope with infinite data
 * streams, but does not shuffle the data.
 * 
 * @testedby {@link ResampledDataStreamTest}
 * 
 * @author daniel
 * 
 */
public final class ResampledDataStream extends ADataStream {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final IDataStream base;

	private final List<Datum> boost = new ArrayList<Datum>();
	private final Map<? extends Object, ? extends Number> booster;

	/**
	 * Increase/decrease the representatives depending on class.
	 * 
	 * @param booster
	 *            The (average) number of samples to output for every input
	 *            sample of this label.<br>
	 *            If a value is 0, that label will be filtered out.<br>
	 *            If a value is <1, data will be stochastically filtered with a
	 *            % going through.<br>
	 *            If a value is unset, this is interpreted as 1. <br>
	 *            If a value is >1 multiple copies will be created. E.g. if a
	 *            label has value 2.5, this would give a guaranteed 2 copies and
	 *            a 50% chance of a 3rd copy.<br>
	 */
	public ResampledDataStream(IDataStream base,
			Map<? extends Object, ? extends Number> booster) {
		super(base.getDim());
		assert booster != null;
		this.base = base;
		this.booster = booster;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		final AbstractIterator<Datum> baseIt = base.iterator();
		return new AbstractIterator<Datum>() {
			@Override
			public double[] getProgress() {
				return baseIt.getProgress();
			}

			@Override
			protected Datum next2() {
				if (boost.size() != 0) {
					Datum b = boost.remove(0);
					return b;
				}
				if (!baseIt.hasNext())
					return null;
				Datum d = baseIt.next();
				// boost?
				Number bn = booster.get(d.getLabel());
				// interpret as 1
				if (bn == null)
					return d;
				double b = bn.doubleValue();
				if (b == 1)
					return d;
				// filter?
				if (b == 0)
					return next2();
				// copy
				int ib = (int) b;
				double rem = b - ib;
				for (int i = 0; i < ib; i++) {
					boost.add(d.copy());
				}
				// stochastic on remainder
				if (Utils.getRandomChoice(rem))
					return d;
				// filtered!
				return next2();
			}
		};
	}

	@Override
	public String toString() {
		return ResampledDataStream.class.getSimpleName() + ":" + base;
	}
}
