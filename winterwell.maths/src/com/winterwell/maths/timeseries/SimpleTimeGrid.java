package com.winterwell.maths.timeseries;

import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;


/**
 * Buckets are [start, end) -- ie start-inclusive, end-exclusive.
 * 
 * WARNING: uses millisecond arithmetic -- not calendar arithmetic! 
 * So it cannot handle eg a 1 month grid
 * @author daniel
 * @testedby  SimpleTimeGridTest}
 */
public class SimpleTimeGrid implements ITimeGrid {

	public String toString() {
		return "SimpleTimeGrid["+limit+", origin: "+origin+"]";
	}
	
	final Time origin;
	private final TUnit unit;
	
	Period limit;
	
	@Override
	public Time getStart() {
		return limit.getStart();
	}
	@Override
	public Time getEnd() {
		return limit.getEnd();
	}
	
	/**
	 * 
	 * @param origin The grid extends into the past and future from this point. You are strongly advised to use 
	 * {@link TimeUtils#getStartOfDay(Time)} or similar.
	 * @param unit
	 */
	public SimpleTimeGrid(Time origin, TUnit unit) {
		assert unit != TUnit.MONTH; // needs calendar arithmetic
		this.origin = origin;
		this.unit = unit;
		// default to a wide limit
		setLimit(new Period(origin.minus(3, TUnit.YEAR), origin.plus(3, TUnit.YEAR)));
	}
	
	public SimpleTimeGrid setLimit(Period limit) {
		this.limit = limit;
		assert limit.within(origin) : limit+" v "+origin;
		return this;
	}
	
	@Override
	public int getBucket(Time time) {
		// limit
		if (time.isBefore(limit.first)) {
			time = limit.first;
		} else if (time.isAfter(limit.second)) {
			time = limit.second;
		}
		
		double steps = (time.getTime() - origin.getTime()) / (1.0*unit.getMillisecs());
		int n = (int) Math.floor(steps);
		return n;
	}
	
	public Time getOrigin() {
		return origin;
	}

	@Override
	public Dt getDt() {
		return unit.dt;
	}

	@Override
	public Time getBucketStart(int bucket) {
		return new Time(origin.getTime() + bucket*unit.getMillisecs());
	}

	@Override
	public Time getBucketEnd(int bucket) {
		return getBucketStart(bucket).plus(getDt());
	}

	@Override
	public boolean contains(Time time) {
		return limit.within(time);
	}	

}
