package com.winterwell.maths.timeseries;

import java.io.Serializable;
import java.util.BitSet;

import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * For marking which time-buckets have been done. 
 * Buckets are [start, end) -- ie start-inclusive, end-exclusive.
 * 
 * TODO maybe refactor to use IGridInfo, so not Time specific??
 * 
 * TODO support for in-progress?? cursors??
 * 
 * TODO support for adding gaps?? so we can use this to fill in streaming-probe outages??
 * 
 * @author daniel
 *
 */
public class TimeCoverage implements Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() {
		return "TimeCoverage["+slicer+", past-done:"+past.cardinality()+", future-done:"+future.cardinality()+"]";
	}
	
	public Time getOrigin() {
		return slicer.getBucketStart(0);
	}
	
	final ITimeGrid slicer;
	final BitSet future = new BitSet();
	final BitSet past= new BitSet();
	
	public TimeCoverage(ITimeGrid slicer) {
		this.slicer = slicer;
	}
	
	public void set(Time time, boolean onOff) {
		int i = slicer.getBucket(time);
		if (i<0) {
			past.set(-i, onOff);
		} else {
			future.set(i, onOff);
		}
	}
	
	public boolean isDone(Time time) {
		int i = slicer.getBucket(time);
		return isDone(i);
	}

	/**
	 * Set for [start, end)
	 * @param period
	 * @param onOff
	 */
	public void set(Period period, boolean onOff) {
		int a = slicer.getBucket(period.getStart());
		// buckets are [,) so step inside the bucket
		int b = slicer.getBucket(period.getEnd().minus(TUnit.MILLISECOND));
		// TODO we could use BitSet.flip for extra speed
		for(int i=a; i<=b; i++) {
			if (i<0) {
				past.set(-i, onOff);
			} else {
				future.set(i, onOff);
			}			
		}
	}

	public boolean isDone(Period period) {
		int a = slicer.getBucket(period.getStart());
		int b = slicer.getBucket(period.getEnd());
		for(int i=a; i<=b; i++) {
			boolean v = isDone(i);			
			if ( ! v) return false;
		}
		return true;
	}

	private boolean isDone(int i) {
		boolean v;
		if (i<0) {
			v = past.get(-i);
		} else {
			v = future.get(i);
		}
		return v;
	}
	
	/**
	 * 
	 * @param from
	 * @return a period (snapped to fit the grid) which is undone, or null if all done
	 */
	public Period getNextUndonePast(Time from) {
		return getNextUndone2(from, false);
	}
	
	/**
	 * 
	 * @param _from
	 * @param fwd
	 * @return Note: Can have from in the middle of it, if from falls within a bucket. ie. it can mix past & future a little.
	 */
	private Period getNextUndone2(final Time _from, boolean fwd) {
		Time from = _from;
		Dt dt = slicer.getDt();
		while(slicer.contains(from)) {
			int bi = slicer.getBucket(from);
			if (isDone(bi)) {
				from = fwd? from.plus(dt) : from.minus(dt);
				continue;
			}
			Period p = new Period(slicer.getBucketStart(bi), slicer.getBucketEnd(bi));
//			if ((fwd && from.equals(slicer.getBucketEnd(bi))) || from.equals(slicer.getBucketStart(bi))) {
//				// TODO? actually let's take the next bucket
//			} 
			return p;
		}
		return null;
	}

	/**
	 * 
	 * @param from
	 * 	 * @return a period (snapped to fit the grid) which is undone, or null

	 */
	public Period getNextUndoneFuture(Time from) {
		return getNextUndone2(from, true);
	}

	public Period getPeriod() {
		return new Period(slicer.getStart(), slicer.getEnd());
	}

	public double[] getProgress() {
		int p = past.cardinality();
		int f = future.cardinality();
		double n = getPeriod().length(slicer.getDt().getUnit());
		return new double[]{
				p+f,
				n
		};
	}

	public Period getNextUndone() {
		return getNextUndoneFuture(getOrigin());
	}
}
