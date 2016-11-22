package com.winterwell.utils.time;

import java.util.Calendar;
import java.util.List;

import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.TimeIteratorTest;

import com.winterwell.utils.containers.AbstractIterator;

/**
 * Iterate through a range of dates.
 * 
 * @testedby {@link TimeIteratorTest}
 * @author Daniel
 */
public final class TimeIterator implements Iterable<Time> {

	final Calendar cal;

	private final Time end;
	private final Dt step;

	/**
	 * Create a new TimeIterator with the given stepping. Stepping uses the
	 * local Calendar, so stepping in months works as you'd want (e.g. you can
	 * construct an iterator for the 1st of every month).
	 * 
	 * @param range
	 *            start time (inclusive) and end time (inclusive if in-step; end
	 *            time is not included if adding step*dt to start time oversteps
	 *            it).
	 */
	public TimeIterator(Time start, Time end, Dt step) {
		assert !start.isAfter(end) : start + " is after " + end;
		cal = start.getCalendar();
		this.end = end;
		this.step = step;
	}

	/**
	 * Convenience for {@link #TimeIterator(Time, Time, Dt)}
	 */
	public TimeIterator(Time start, Time end, TUnit step) {
		this(start, end, step.dt);
	}
	
	
	public Time getEnd() {
		return end;
	}

	public Dt getStep() {
		return step;
	}

	@Override
	public AbstractIterator<Time> iterator() {
		final Calendar cal2 = (Calendar) cal.clone();
		return new TIIt(cal2);
	}

	@Override
	public String toString() {
		return "TimeIterator [now=" + new Time(cal) + ", end=" + end + ", step=" + step
				+ "]";
	}

	/**
	 * Convert into a list.
	 * @return
	 */
	public List<Time> toList() {
		return Containers.getList(this);
	}
	
	private class TIIt extends AbstractIterator<Time> {
		private Calendar cal2;
		public TIIt(Calendar cal2) {
			this.cal2 = cal2;
		}

		@Override
		public Time next2() {
			Time now = new Time(cal2);
			if (now.isAfter(end))
				return null;
			step.addTo(cal2);
			return now;
		}
	}

}
