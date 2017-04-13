package com.winterwell.maths.timeseries;

import java.lang.reflect.Constructor;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;

/**
 * Filter and process data points from a time-stamped data stream.
 * 
 * @author daniel
 */
public abstract class FilteredDataStream extends ADataStream {
	/**
	 * Please stop!
	 */
	public static class EnoughAlreadyException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	private static final long serialVersionUID = 1L;

	/**
	 * Filter all null-labelled datums.
	 * 
	 * @param data
	 */
	public static IDataStream hasLabels(IDataStream baseData) {
		return new HasLabelsStream(baseData);
	}

	protected final IDataStream base;

	/**
	 * Convenience for {@link #FilteredDataStream(IDataStream, int)} with
	 * dimensions out = dimensions in.
	 * 
	 * @param base
	 */
	public FilteredDataStream(IDataStream base) {
		this(base, base.getDim());
	}
	
	/**
	 * Filters/modifies data from a base stream.
	 * 
	 * @param base
	 * @param dim
	 *            The number of output dimensions.
	 */
	public FilteredDataStream(IDataStream base, int dim) {
		super(dim);
		this.base = base;
		assert base != null;
	}

	@Override
	public void close() {
		base.close();
	}

	/**
	 * This implementation relies on there being a 1-arg constructor for the
	 * class taking in an IDataStream
	 */
	@Override
	public IDataStream factory(Object sourceSpecifier) {
		IDataStream source = base.isFactory() ? base.factory(sourceSpecifier)
				: (IDataStream) sourceSpecifier;
		try {
			Constructor<? extends FilteredDataStream> cons = getClass()
					.getConstructor(IDataStream.class);
			FilteredDataStream clone = cons.newInstance(source);
			return clone;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Process a data point.
	 * 
	 * @param datum
	 * @return a possibly modified version of datum, null to filter out this
	 *         datum.
	 * @throws EnoughAlreadyException
	 *             to quit early
	 */
	protected abstract Datum filter(Datum datum) throws EnoughAlreadyException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.timeseries.ADataStream#getSampleFrequency()
	 */
	@Override
	public Dt getSampleFrequency() {
		return base.getSampleFrequency();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.timeseries.IDataStream2#canInstantiate()
	 */
	@Override
	public boolean isFactory() {
		return true;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			AbstractIterator<Datum> baseIt = base.iterator();

			@Override
			public double[] getProgress() {
				return baseIt.getProgress();
			}

			@Override
			protected Datum next2() {
				try {
					Datum n = null;
					while (n == null && baseIt.hasNext()) {
						n = baseIt.next();
						n = filter(n);
					}
					return n;
				} catch (EnoughAlreadyException e) {
					return null;
				}
			}
		};
	}
}

/**
 * Filter all null-labelled datums.
 * 
 * @param data
 */
class HasLabelsStream extends FilteredDataStream {
	
	private static final long serialVersionUID = 1L;

	public HasLabelsStream(IDataStream baseData) {
		super(baseData);
	}

	@Override
	protected Datum filter(Datum datum) {
		return datum.getLabel() == null ? null : datum;
	}
};