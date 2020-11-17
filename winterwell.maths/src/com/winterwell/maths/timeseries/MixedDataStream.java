/**
 *
 */
package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.winterwell.maths.datastorage.ADataSet;
import com.winterwell.maths.datastorage.ColumnInfo;
import com.winterwell.utils.Printer;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;

/**
 * Mix together two data streams to give a stream which draws from both, picking
 * the next by time.
 * 
 * @see ExtraDimensionsDataStream which adds streams together so they run side
 *      by side.
 * 
 * @testedby  MixedDataStreamTest}
 * @author daniel
 */
public class MixedDataStream extends ADataStream {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private IDataStream[] streams;

	/**
	 * @param dataset
	 * @param cols
	 *            <p>
	 *            Note: This will copy everything into underlying in-memory
	 *            ListDataStreams. This is done to avoid "too many files open"
	 *            errors for hogging raw resources if a *lot* of cols are
	 *            specified. This is not ideal if you have a few large cols.
	 */
	public MixedDataStream(ADataSet dataset, List<ColumnInfo> cols) {
		super(1);
		streams = new IDataStream[cols.size()];
		for (int i = 0; i < streams.length; i++) {
			ColumnInfo<?> columnInfo = cols.get(i);
			IDataStream s = dataset.getDataStream1D(columnInfo);
			streams[i] = new ListDataStream(s);
		}
	}

	/**
	 */
	public MixedDataStream(IDataStream... streams) {
		super(streams[0].getDim());
		assert checkDims(streams);
		this.streams = streams;
	}

	/**
	 * Create an empty mix (for use with {@link #add(IDataStream)}).
	 * 
	 * @param dim
	 */
	public MixedDataStream(int dim) {
		super(dim);
		streams = new IDataStream[0];
	}

	public MixedDataStream(List<IDataStream> baseStreams) {
		this(baseStreams.toArray(new IDataStream[baseStreams.size()]));
	}

	/**
	 * Add a new stream into the mix
	 * 
	 * @param stream
	 */
	public void add(IDataStream stream) {
		IDataStream[] streams2 = Arrays.copyOf(streams, streams.length + 1);
		streams2[streams.length] = stream;
		streams = streams2;
	}

	@Override
	public void close() {
		for (IDataStream stream : streams) {
			stream.close();
		}
	}

	@Override
	public IDataStream factory(Object sourceSpecifier) {
		Pair<Object> pair = (Pair<Object>) sourceSpecifier;
		throw new TodoException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.timeseries.IDataStream#getSampleFrequency()
	 */
	@Override
	public Dt getSampleFrequency() {
		Dt fastest = null;
		for (IDataStream stream : streams) {
			Dt dta = stream.getSampleFrequency();
			if (dta == null) {
				continue;
			}
			if (fastest == null || dta.isShorterThan(fastest)) {
				fastest = dta;
			}
		}
		return fastest;
	}

	@Override
	public List<Object> getSourceSpecifier() {
		ArrayList<Object> sss = new ArrayList<Object>(streams.length);
		for (int i = 0; i < streams.length; i++) {
			sss.add(streams[i].getSourceSpecifier());
		}
		return sss;
	}

	@Override
	public boolean isFactory() {
		return true;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		final AbstractIterator[] streamIts = new AbstractIterator[streams.length];
		for (int i = 0; i < streams.length; i++) {
			streamIts[i] = streams[i].iterator();
		}
		return new AbstractIterator<Datum>() {
			@Override
			public final double[] getProgress() {
				// report average % progress
				Vector p = DataUtils.newVector(getDim());
				int cnt = 0;
				for (AbstractIterator<Datum> stream : streamIts) {
					double[] pa = stream.getProgress();
					if (pa == null) {
						continue;
					}
					p.add(new DenseVector(pa, false));
					cnt++;
				}
				if (cnt == 0)
					return null;
				p.scale(1.0 / cnt);
				return DataUtils.toArray(p);
			}

			@Override
			protected Datum next2() {
				// need to know who we draw from, so we can't avoid duplicating
				// code from peekNext
				int fi = -1;
				Time first = null;
				for (int i = 0; i < streamIts.length; i++) {
					Datum a = (Datum) streamIts[i].peekNext();
					if (a == null) {
						continue;
					}
					if (fi == -1 || a.time.isBefore(first)) {
						first = a.time;
						fi = i;
					}
				}
				if (fi == -1)
					return null;
				return (Datum) streamIts[fi].next();
			}
		};
	}

	@Override
	public int size() {
		int sum = 0;
		for (IDataStream s : streams) {
			int n = s.size();
			if (n == -1)
				return -1;
			sum += n;
		}
		return sum;
	}

	@Override
	public String toString() {
		return "mix:" + Printer.toString(streams);
	}

}
