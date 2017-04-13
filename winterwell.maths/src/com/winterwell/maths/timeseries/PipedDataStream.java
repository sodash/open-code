package com.winterwell.maths.timeseries;

import java.util.concurrent.LinkedBlockingQueue;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.IOneShot;

/**
 * A data stream which pipes out its data. A call to {@link #next()} will block
 * until either more data is available or the end of the stream is confirmed.
 * 
 * @testedby {@link PipedDataStreamTest}
 * 
 * @author daniel
 */
public final class PipedDataStream extends ADataStream implements IOneShot {
	private static final long serialVersionUID = 1L;
	/**
	 * true if the pipe has been closed to new input.
	 */
	private boolean eof;
	private final LinkedBlockingQueue<Datum> q;

	public PipedDataStream(int dim) {
		super(dim);
		q = new LinkedBlockingQueue<Datum>();
	}

	public void add(Datum datum) {
		assert !eof;
		assert datum.size() == getDim() : "Expected:" + getDim() + "d	got:"
				+ datum.size();
		q.add(datum);
	}

	/**
	 * No more data may be added after this.
	 */
	public void addEndOfStream() {
		eof = true;
	}

	@Override
	public boolean isFactory() {
		// This relies on "manual" pumping of data into it
		return false;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			/**
			 * Get the next element, waiting if need be.
			 * 
			 * @throws wrapped
			 *             InterruptedException if interrupted
			 */
			@Override
			protected Datum next2() {
				// done?
				if (eof && q.size() == 0)
					return null;
				try {
					return q.take();
				} catch (InterruptedException e) {
					throw Utils.runtime(e);
				}
			}
		};
	}

}
