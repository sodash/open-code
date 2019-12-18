package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.datastorage.ChainedIterable;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Chain together several data streams in sequence.
 * 
 * @author daniel
 * @see ChainedIterable
 * 
 */
public final class ChainedDataStream extends ADataStream {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final List<IDataStream> streams;

	/**
	 * Chain together several data streams in sequence.
	 */
	public ChainedDataStream(List<? extends IDataStream> streams) {
		super(streams.get(0).getDim());
		for (IDataStream dataStream : streams) {
			assert dataStream.getDim() == getDim();
		}
		this.streams = new ArrayList<IDataStream>(streams);
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			private int i;
			private AbstractIterator<Datum> stream = streams.get(0).iterator();

			@Override
			protected Datum next2() {
				if (stream.hasNext())
					return stream.next();
				i++;
				if (i >= streams.size())
					return null;
				stream = streams.get(i).iterator();
				return next2();
			}
		};
	}

}
