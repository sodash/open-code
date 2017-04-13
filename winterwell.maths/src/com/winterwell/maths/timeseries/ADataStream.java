package com.winterwell.maths.timeseries;

import java.io.Serializable;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.Dt;

/**
 * A time series data stream. Possibly infinite. {@link ADataStream}s are not
 * thread safe.
 * 
 * @author Daniel
 * 
 */
public abstract class ADataStream
		implements IDataStream, Serializable {
	private static final long serialVersionUID = 1L;

	private int dim;

	/**
	 * For de-serialisation ONLY
	 */
	@Deprecated
	protected ADataStream() {
	}

	public ADataStream(int dim) {
		this.dim = dim;
	}
	
	protected void setDim(int dim) {
		assert dim >= 0; // 0 is allowed if empty
		this.dim = dim;
	}

	/**
	 * Checks that all streams have the same dimension. Utility for streams that
	 * work with multiple base streams.
	 * 
	 * @param streams
	 * @return
	 */
	protected boolean checkDims(IDataStream[] streams) {
		int d = streams[0].getDim();
		for (IDataStream s : streams) {
			assert s.getDim() == d : s.getDim() + " vs " + d + ": "
					+ streams[0] + " vs " + s;
		}
		return true;
	}

	@Override
	public void close() {
		// do nothing by default
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * winterwell.maths.timeseries.IDataStream2#instantiate(java.lang.Object)
	 */
	@Override
	public IDataStream factory(Object sourceSpecifier)
			throws ClassCastException {
		throw new UnsupportedOperationException(String.valueOf(sourceSpecifier));
	}

	@Override
	protected void finalize() throws Throwable {
		FileUtils.close(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.timeseries.IDataStream#getDim()
	 */
	@Override
	public final int getDim() {
		return dim;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.timeseries.IDataStream#getSampleFrequency()
	 */
	@Override
	public Dt getSampleFrequency() {
		return null;
	}

	@Override
	public Object getSourceSpecifier() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.timeseries.IDataStream#is1D()
	 */
	@Override
	public final boolean is1D() {
		return getDim() == 1;
	}

	@Override
	public boolean isEmpty() {
		return !iterator().hasNext();
	}

	@Override
	public boolean isFactory() {
		// the default should probably be false, but this will flush out bugs
		// better
		return true;
	}

	/**
	 * Read the rest of this datastream into a {@link ListDataStream}.
	 * <p>
	 * Warning: will hang if this is an infinite stream!
	 */
	@Override
	public ListDataStream list() {
		return new ListDataStream(this);
	}

	@Override
	public int size() {
		return -1;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[size=" + size() + "]";
	}

}
