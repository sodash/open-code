/**
 *
 */
package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.winterwell.maths.timeseries.ExtraDimensionsDataStream.KMatchPolicy;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * Create a multi-dimensional data-stream by combining other data-streams. A
 * Datum is taken from each of the input streams, and concatenated to give a
 * long vector. The stream ends as soon as any of the input streams end.
 * <p>
 * Important note: The time stamp is taken from the *first* stream. The
 * time-stamps of the other streams are <i>completely ignored</i>.
 * <p>
 * If a label is set, that is used, otherwise the label is set *if* there is a
 * unique non-null label amongst the streams.
 * 
 * @param streams
 * @return
 * 
 * @testedby  ExtraDimensionsDataStreamTest}
 */
public final class ExtraDimensionsDataStream extends ADataStream {

	// TODO use these & document status & test
	public enum KMatchPolicy {		
		DISCARD_ON_MISMATCH,
		/**
		 * Move through the streams in index lock-step,  
		 * i.e. take 1 from each stream & sod the timestamps.
		 * The returned timestamps are taken from the 1st stream.
		 */
		IGNORE_TIMESTAMPS, 
		TODO_SUM_PREVIOUS_VALUES_ON_MISMATCH,
		/**
		 * This returns every value from every stream.
		 * So if 1 stream is hourly & the other is daily, then you get
		 * hourly. 
		 * <p>
		 * It will fill in values by assuming the previous one holds good.
		 * WARNING: Leading blanks will be 0s!
		 */
		USE_PREVIOUS_VALUE_ON_MISMATCH, 
		USE_ZERO_ON_MISMATCH
//		USE_NAN_ON_MISMATCH
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static int sumDims(List<? extends IDataStream> streams) {
		int tdims = 0;
		for (IDataStream s : streams) {
			tdims += s.getDim();
		}
		return tdims;
	}

	final long[] keepCount;
	final long[] throwCount;
	
	String label;
	final KMatchPolicy policy;
	final IDataStream[] streams;


	/**
	 * If the timestamps are a little off, consider them to match. Default: a
	 * bit under a second.
	 */
	int toleranceMillisecs = 750;

	/**
	 * Convenience for {@link #ExtraDimensionsDataStream(KMatchPolicy, List)}
	 * 
	 * @param policy
	 * @param dataStreams
	 */
	public ExtraDimensionsDataStream(KMatchPolicy policy,
			IDataStream... dataStreams) {
		this(policy, Arrays.asList(dataStreams));
	}

	public ExtraDimensionsDataStream(KMatchPolicy policy,
			List<? extends IDataStream> streams) {
		super(sumDims(streams));
		this.policy = policy;
		this.streams = streams.toArray(new IDataStream[0]);
		keepCount = new long[streams.size()];
		throwCount = new long[keepCount.length];
	}

	/**
	 * For convenient testing really.
	 * 
	 * @param label
	 * @param streams
	 */
	public ExtraDimensionsDataStream(KMatchPolicy policy, String label,
			IDataStream... streams) {
		this(policy, Arrays.asList(streams));
		// assert ! (label instanceof ADataStream); // an easy mistake with
		// Object, ... method signature
		setLabel(label);
	}

	@Override
	public void close() {
		for (IDataStream stream : streams) {
			stream.close();
		}
	}

	@Override
	public IDataStream factory(Object sourceSpecifier) {
		List<IDataStream> streams2 = new ArrayList<IDataStream>();
		for (IDataStream stream : streams) {
			IDataStream s2 = stream.factory(sourceSpecifier);
			streams2.add(s2);
		}
		ExtraDimensionsDataStream edds = new ExtraDimensionsDataStream(policy,
				streams2);
		edds.setLabel(label);
		return edds;
	}

	/**
	 * @return for each stream, how much data is getting discarded due to timing
	 *         mis-matches? Values are 0 (nothing discarded) to 1 (everything
	 *         discarded).
	 *         <p>
	 *         If called before any data has gone through the stream, the ratios
	 *         will all be 1 (total discard).
	 */
	public double[] getDiscardRatios() {
		double[] ratios = new double[keepCount.length];
		for (int i = 0; i < ratios.length; i++) {
			double total = (keepCount[i] + throwCount[i]);
			if (total == 0) {
				ratios[i] = 1;
			} else {
				ratios[i] = throwCount[i] / total;
			}
		}
		return ratios;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		final AbstractIterator[] streamIts = new AbstractIterator[streams.length];
		for (int i = 0; i < streams.length; i++) {
			streamIts[i] = streams[i].iterator();
		}
		return new EDIterator(streamIts, this);
	}

	private void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Set the difference in timestamps which this stream will accept (ie.
	 * ignore).
	 * 
	 * @param dt
	 *            Must not be null. Default: a bit under a second.
	 */
	public void setTolerance(Dt dt) {
		assert policy != KMatchPolicy.IGNORE_TIMESTAMPS;
		this.toleranceMillisecs = (int) dt.getMillisecs();
	}

	boolean withinTolerance(Time a, Time b) {
		long dt = a.diff(b);
		return Math.abs(dt) <= toleranceMillisecs;
	}

}

class EDIterator extends AbstractIterator<Datum> {
		/**
		 * only used with some match policies
		 */
		Datum[] previous;
		private AbstractIterator[] streamIts;
		private ExtraDimensionsDataStream strm;

		public EDIterator(AbstractIterator[] streamIts, ExtraDimensionsDataStream strm) {
			this.streamIts = streamIts;
			this.strm = strm;
		}

		private Datum getNext2_discardOnMismatch() {
			assert strm.policy == KMatchPolicy.DISCARD_ON_MISMATCH : strm.policy;
			// Datum[] next = new Datum[streams.length];
			// skip until we get into lock step
			Time last = null;
			while (true) {
				// get last time in the next datums
				boolean inStep = true;
				for (int i = 0; i < streamIts.length; i++) {
					Datum d = (Datum) streamIts[i].peekNext();
					if (d == null)
						// a stream has run dry - automatic mismatch from
						// here on
						return null;
					if (last == null) {
						last = d.time;
						continue;
					}
					if (strm.withinTolerance(last, d.time)) {
						// all ok
						continue;
					}
					// oh dear - a mismatch
					inStep = false;
					if (d.time.isAfter(last)) {
						last = d.time;
					}
				}
				assert last != null;
				if (inStep) {
					// hurrah! in-sync
					break;
				}
				// discard items
				for (int i = 0; i < streamIts.length; i++) {
					while (true) {
						Datum d = (Datum) streamIts[i].peekNext();
						if (d == null)
							return null;
						if (strm.withinTolerance(d.time, last)) {
							break;
						}
						if (d.time.isAfter(last)) {
							break;
						}
						// discard
						streamIts[i].next();
						strm.throwCount[i]++;
					}
				}
				// try again...
			}
			// OK - now instep
			// send 'em all
			boolean[] mask = new boolean[streamIts.length];
			Arrays.fill(mask, true);
			return getNext3_advance(last, mask);
		}

		private Datum getNext2_ignoreTimeStamps() {
			Datum[] next = new Datum[streamIts.length];
			for (int i = 0; i < streamIts.length; i++) {
				Datum d = (Datum) streamIts[i].peekNext();
				if (d == null)
					return null;
				next[i] = d;
			}
			// send 'em all
			boolean[] mask = new boolean[next.length];
			Arrays.fill(mask, true);
			return getNext3_advance(next[0].time, mask);
		}

		private Datum getNext2_previousValueOnMismatch() {
			assert strm.policy == KMatchPolicy.USE_PREVIOUS_VALUE_ON_MISMATCH;
			// 1st call: sure we have a previous value for each dim.
			if (previous == null) {
				previous = new Datum[streamIts.length];
				// fill in with zeroes?? TODO use the 1st item of each stream??
				for(int i=0; i<previous.length; i++) {					
					int baseDim = strm.streams[i].getDim();
					previous[i] = new Datum(new double[baseDim]);
				}
			}
			
			Time first = getNext3_earliest();
			if (first==null) {
				return null; // no data left
			}
			// pick the items
			boolean[] mask = new boolean[streamIts.length];
			for (int i = 0; i < streamIts.length; i++) {
				Datum d = (Datum) streamIts[i].peekNext();
				if (d == null) {
					// a stream has run dry - just ignore it
					continue;
				}
				if (strm.withinTolerance(first, d.time)) {
					mask[i] = true;
				}
			}			
			Datum datum = getNext3_advance(first, mask);
			return datum;			
		}
		
		private Datum getNext2_zeroOnMismatch() {
			assert strm.policy == KMatchPolicy.USE_ZERO_ON_MISMATCH;
			Time first = getNext3_earliest();
			if (first==null) {
				return null; // no data left
			}
			// pick the items
			boolean[] mask = new boolean[streamIts.length];
			for (int i = 0; i < streamIts.length; i++) {
				Datum d = (Datum) streamIts[i].peekNext();
				if (d == null) {
					// a stream has run dry - just ignore it
					continue;
				}
				if (strm.withinTolerance(first, d.time)) {
					mask[i] = true;
				}
			}			
			Datum datum = getNext3_advance(first, mask);
			return datum;
		}

		/**
		 * @return the earliest time among the peek-next datums,
		 * or null if there is no more data.
		 */
		private Time getNext3_earliest() {
			Time first = null;
			// get first time in the next datums
			for (int i = 0; i < streamIts.length; i++) {
				Datum d = (Datum) streamIts[i].peekNext();
				if (d == null) {
					// a stream has run dry - just ignore it
					continue;
				}
				first = TimeUtils.first(first, d.getTime());
			}
			return first;
		}

		/**
		 * Assumes: the #previous array has been setup if needed.
		 * 
		 * @param t time-stamp to use in the returned Datum
		 * @param mask true for those streams which will be used (& advanced).
		 * for mask=false, we use 0 or #previous depending on the strm.policy.
		 * @return
		 */
		private Datum getNext3_advance(Time t, boolean[] mask) {
			double[] v = new double[strm.getDim()];
			// index into v
			int i = 0;
			Object lbl = null;
			boolean pure = strm.label == null; // label overrides, so no need to
											// look for pure labels
			for (int s = 0; s < streamIts.length; s++) {
				AbstractIterator<Datum> baseIt = streamIts[s];
				Datum d;
				if (mask[s]) {
					// advance the underlying data stream
					d = baseIt.next();
					strm.keepCount[s]++;
					assert DataUtils.isFinite(d) : d + "\n" + baseIt;
					// ...store for next time?
					if (strm.policy == KMatchPolicy.USE_PREVIOUS_VALUE_ON_MISMATCH) {
						previous[s] = d;
					}
					
				} else if (strm.policy == KMatchPolicy.USE_PREVIOUS_VALUE_ON_MISMATCH) {
					d = previous[s];
				} else if (strm.policy == KMatchPolicy.USE_ZERO_ON_MISMATCH) {
					// array of 0s
					int baseDim = strm.streams[s].getDim();
					d = new Datum(new double[baseDim]);
//				} else if (strm.policy == KMatchPolicy.USE_NAN_ON_MISMATCH) {
//						// array of NaNs
//						int baseDim = strm.streams[s].getDim();
//						double[] nans = new double[baseDim];
//						Arrays.fill(nans, Double.NaN);						
//						d = new Datum(nans); 
				} else {
					throw new TodoException(strm.policy);
				}
				assert d != null;
				
				// Pure label?
				if (pure && d.getLabel() != null) {
					if (lbl == null) {
						lbl = d.getLabel();
					} else if (!lbl.equals(d.getLabel())) {
						pure = false;
						lbl = null;
					}
				}
				double[] arr = d.getData();
				for (int j = 0; j < arr.length; j++) {
					v[i] = arr[j];
					i++;
				}
			}
			
			// Which label?
			if (strm.label != null) {
				lbl = strm.label;
			} else if (!pure) {
				lbl = null;
			}
			// Done
			return new Datum(t, v, lbl);
		}

		@Override
		protected Datum next2() {
			switch (strm.policy) {
			case DISCARD_ON_MISMATCH:
				return getNext2_discardOnMismatch();
			case IGNORE_TIMESTAMPS:
				return getNext2_ignoreTimeStamps();
			case USE_PREVIOUS_VALUE_ON_MISMATCH:
				return getNext2_previousValueOnMismatch();
			case USE_ZERO_ON_MISMATCH:
				return getNext2_zeroOnMismatch();
			}
			throw new TodoException();
		}

	}
