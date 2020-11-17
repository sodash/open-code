/**
 * 
 */
package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.Arrays;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * Sum data over a time period (i.e. collect into buckets).
 * <p>
 * Buckets include their start time, but not their end time. I.e. they are of
 * the form [start,end). Unless start==end, in which case they include the end.
 * 
 * <p>
 * A bucket is labelled if all the data for that bucket had the same label. So a
 * labelled input stream may lead to a partially labelled bucketed stream.
 * 
 * TODO worry about overflow. use rolling averages?
 * 
 * @testedby  BucketedDataStreamTest}
 * 
 * @author Daniel
 */
public final class BucketedDataStream extends ADataStream {
	class BucketIterator extends AbstractIterator<Datum> {

		private final AbstractIterator<Datum> baseIt;

		/**
		 * The sum of squared values for this bucket (sued for variance). Not
		 * recycled between buckets
		 */
		private final double[] bucket2Vector;

		/**
		 * This *is* recycled between buckets
		 */
		private final ArrayList<Datum> bucketContents = new ArrayList<Datum>();

		private Time bucketEnd;

		private Time bucketStart;
		/**
		 * The sum of values for this bucker. The array is not recycled between
		 * buckets
		 */
		private final double[] bucketVector;

		private Object label;
		private boolean pureBucket;

		public BucketIterator() {
			baseIt = base.iterator();
			bucketVector = new double[base.getDim()];
			bucket2Vector = new double[base.getDim()];
			// init first bucket
			if (baseIt.hasNext()) {
				Datum d = baseIt.peekNext();
				bucketEnd = d.getTime(); // this will become bucketStart
				nextBucket();
			}
		}

		/**
		 * Allows caller to manually set the start of the first bucket.
		 * 
		 * @param t
		 */
		public BucketIterator(Time t) {
			baseIt = base.iterator();
			bucketVector = new double[base.getDim()];
			bucket2Vector = new double[base.getDim()];
			// init first bucket
			if (baseIt.hasNext()) {
				bucketEnd = t; // this will become bucketStart
				nextBucket();
			}
		}

		/**
	     * If requested, align buckets by rounding down to the next half hour mark
		 * Otherwise equivalent to default constructor.
	     * TODO: generalise to Dt, unify constructors
		 * 
		 * @param align
		 */
		public BucketIterator(Boolean align) {
			baseIt = base.iterator();
			bucketVector = new double[base.getDim()];
			bucket2Vector = new double[base.getDim()];
			// init first bucket
			if (baseIt.hasNext()) {
				if (align) {
					bucketEnd = this.roundDownToHalfHour(baseIt.peekNext().time);
				} else {
					bucketEnd = baseIt.peekNext().time;
				}
				nextBucket();
			}
		}
		
		private Time roundDownToHalfHour(Time t) {
			Integer minutes_out = 0;
			if (t.getMinutes() >= 30) {
				minutes_out = 30;
			}
			return new Time(t.getYear(),t.getMonth(),t.getDayOfMonth(),t.getHour(),minutes_out,0);
		}
		
		private void addToBucket(Datum d) {
			if (d.getTime().isBefore(bucketStart))
				// Log.report("Datum out of time: "+d); ??optional
				// reporting/exceptions?
				return;
			assert TimeUtils.between(d.getTime(), bucketStart, bucketEnd) : d
					.getTime() + " " + bucketStart + "-" + bucketEnd;
			// Add to bucket
			double[] dv = d.getData();
			assert dv.length == bucketVector.length;
			for (int i = 0; i < dv.length; i++) {
				addToBucket2(dv, i);
			}
			// store?
			if (storeBucketContents) {
				bucketContents.add(d);
			}
			// Count
			count++;
			// Pass on "pure" labels
			if (!pureBucket || d.getLabel() == null)
				return;
			if (label == null) {
				label = d.getLabel();
			} else if (!label.equals(d.getLabel())) {
				// impure bucket - no label
				label = null;
				pureBucket = false;
			}
		}

		/**
		 * The actual adding
		 * 
		 * @param dv
		 * @param i
		 *            dimension index
		 */
		private void addToBucket2(double[] dv, int i) {
			assert MathUtils.isFinite(dv[i]);
			if (useStreamingMean) {
				// average as we go
				int cn = count + 1; // count is increased in addToBucket() after
									// this method
				double old = (count * bucketVector[i]) / cn;
				bucketVector[i] = old + (dv[i] / cn);
			} else {
				// sum as we go
				bucketVector[i] += dv[i];
			}
			if (!returnVariance)
				return;
			// sum x2 as well
			if (useStreamingMean) {
				int cn = count + 1;
				double old = (count * bucket2Vector[i]) / cn;
				bucket2Vector[i] = old + dv[i] * dv[i] / cn;
				return;
			}
			bucket2Vector[i] += dv[i] * dv[i];
		}

		private void fillBucket() {
			long bs = bucketStart.getTime();
			long be = bucketEnd.getTime();
			while (baseIt.hasNext()) {
				Datum d = baseIt.peekNext();
				long dt = d.getTime().getTime();
				if (dt > be)
					return;
				if (dt == be && be != bs)
					return;
				// take the datum
				d = baseIt.next();
				// Add to bucket
				addToBucket(d);
			}
		}

		/**
		 * 
		 * @return the array to return (this is always a fresh object)
		 */
		private double[] getNext2() {
			double[] out;
			if (returnAverage) {
				if (useStreamingMean) {
					out = Arrays.copyOf(bucketVector, bucketVector.length);
					return out;
				}
				// Take average now
				if (count == 0) {
					count = 1; // avoid divide by zero
				}
				out = new double[bucketVector.length];
				for (int i = 0; i < out.length; i++) {
					out[i] = bucketVector[i] / count;
				}
				return out;
			}
			if (returnVariance) {
				// Take E(X2) - E(X)2
				out = new double[bucketVector.length];
				if (useStreamingMean) {
					for (int i = 0; i < out.length; i++) {
						out[i] = bucket2Vector[i] - bucketVector[i]
								* bucketVector[i];
					}
				} else {
					if (count == 0) {
						count = 1; // avoid divide by zero
					}
					for (int i = 0; i < out.length; i++) {
						out[i] = bucket2Vector[i] / count
								- MathUtils.sq(bucketVector[i] / count);
					}
				}
				return out;
			}
			if (addCount) {
				out = Arrays.copyOf(bucketVector, bucketVector.length + 1);
				out[bucketVector.length] = count;
				return out;
			}
			// just a copy of the bucket
			out = Arrays.copyOf(bucketVector, bucketVector.length);
			return out;
		}

		@Override
		protected Datum next2() {
			// Fill the bucket
			if (filterEmptyBuckets) {
				// Skip over gaps
				while (baseIt.hasNext() && count == 0) {
					fillBucket();
					if (count != 0) {
						break;
					}
					// empty!
					nextBucket();
				}
				if (count == 0)
					return null;
			} else {
				if (!baseIt.hasNext())
					return null;
				fillBucket();
			}
			assert pureBucket || label == null;
			// output
			double[] out = getNext2();
			assert out != bucketVector;
			Datum datum = new Datum(bucketStart, out, label);
			// init next bucket
			nextBucket();
			return datum;
		}

		/**
		 * create an empty bucket
		 */
		private void nextBucket() {
			// reset vars
			Arrays.fill(bucketVector, 0);
			Arrays.fill(bucket2Vector, 0);
			count = 0;
			pureBucket = true;
			label = null;
			bucketContents.clear();
			// set new window
			if (bucketSize.getMillisecs() != 0 && !stepByBaseStream) {
				// normal case: step forward
				bucketStart = bucketEnd;
				bucketEnd = bucketEnd.plus(bucketSize);
				return;
			}
			// zero-tolerance and data-driven case:
			// ...hop forward according to the base stream
			Datum pn = baseIt.peekNext();
			if (pn == null) {
				// end of stream - doesn't matter what bucket we create, it
				// won't get used
				bucketStart = bucketStart.plus(TUnit.MILLISECOND);
			} else {
				bucketStart = pn.getTime();
			}
			bucketEnd = bucketStart;
		}
	}

	private static final long serialVersionUID = 1L;

	private boolean addCount;

	private final IDataStream base;

	private final Dt bucketSize;
	private int count;
	private boolean filterEmptyBuckets;

	private boolean returnAverage;
	private boolean returnVariance;

	private Time startTime;

	private boolean align; // align to nearest half-hour
	
	private boolean stepByBaseStream;

	boolean storeBucketContents;

	/**
	 * Try to avoid overflow
	 */
	boolean useStreamingMean;

	/**
	 * A datastream that sums data over a time period (i.e. collect into
	 * buckets).
	 * 
	 * @param base
	 * @param bucketSize
	 */
	public BucketedDataStream(IDataStream base, Dt bucketSize) {
		this(base, bucketSize, false);
		this.startTime = null;
	}

	/**
	 * A datastream that sums data over a time period (i.e. collect into
	 * buckets).
	 * 
	 * @param base
	 * @param bucketSize
	 * @param addCount
	 *            If true, the count of objects that went into a bucket will be
	 *            added as an extra dimension.
	 */
	public BucketedDataStream(IDataStream base, Dt bucketSize, boolean addCount) {
		super(addCount ? base.getDim() + 1 : base.getDim());
		Utils.check4null(base, bucketSize);
		this.base = base;
		this.bucketSize = bucketSize;
		this.addCount = addCount;
		this.startTime = null;
	}

	@Override
	public void close() {
		base.close();
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
		// recurse
		if (base.isFactory()) {
			sourceSpecifier = base.factory(sourceSpecifier);
		}
		// clone
		IDataStream s = (IDataStream) sourceSpecifier;
		BucketedDataStream clone = new BucketedDataStream(s, bucketSize,
				addCount);
		return clone;
	}

	@Override
	public Dt getSampleFrequency() {
		return bucketSize;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		if (this.align == true) {
			return new BucketIterator(true);
		} else if (this.startTime == null) {
			return new BucketIterator(); 
		} else {
			return new BucketIterator(this.startTime);
		}
	}

	/**
	 * If true, empty buckets will not produce datums.
	 * 
	 * @param filterEmptyBuckets
	 *            false by default
	 */
	public void setFilterEmptyBuckets(boolean filterEmpties) {
		this.filterEmptyBuckets = filterEmpties;
	}

	/**
	 * If true, values are averages. Otherwise returns sums. false by default.
	 */
	public void setReturnAverage(boolean average) {
		this.returnAverage = average;
		assert !(returnVariance && returnAverage);
	}

	/**
	 * False by default. If true, return the variance of a bucket in each
	 * dimension.
	 * 
	 * @param b
	 */
	public void setReturnVariance(boolean b) {
		returnVariance = b;
		assert !(returnVariance && returnAverage);
	}

	/**
	 * Sets the start time of first bucket. Note that the value used will be the
	 * last one set before iterator() is called.
	 * 
	 * @param t
	 *            start time of first bucket
	 */
	public void setStartTime(Time _startTime) {
		this.startTime = _startTime;
	}

	/**
	 * Align first bucket to the nearest half hour mark.
	 * If this is true, start time is ignored.
	 * @param _align
	 */
	public void setAlign(Boolean _align) {
		this.align = _align;
	}
	
	/**
	 * If true, time-steps will be made by picking the next bucket start-time
	 * from the base stream. If false, the time-step (bucket-width) is applied
	 * methodically.
	 * <p>
	 * On the one hand, this can lead to irregular spacing between buckets. On
	 * the other hand, it keeps the stream in line with the base stream and
	 * avoids strange bucketing which could occur due to over-regular buckets.
	 * (Note: if true, this makes {@link #setFilterEmptyBuckets(boolean)}
	 * irrelevant)
	 * 
	 * @param stepByBaseStream
	 *            false by default
	 */
	public void setStepByBaseStream(boolean stepByBaseStream) {
		this.stepByBaseStream = stepByBaseStream;
	}

	/**
	 * False by default. If true, the data for each bucket will be stored and
	 * can be accessed via {@link #getBucketContents()}.
	 * 
	 * @param storeBucketContents
	 */
	public void setStoreBucketContents(boolean storeBucketContents) {
		this.storeBucketContents = storeBucketContents;
	}

	/**
	 * If true, mean and variance calculations will be done as we go. This can
	 * help avoid overflow problems, but introduces some rounding errors. false
	 * by default
	 */
	public void setUseStreamingMean(boolean b) {
		useStreamingMean = b;
	}
	
	@Override
	public String toString() {
		return "Bucketed["+base+"]";
	}

}
