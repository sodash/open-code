package com.winterwell.maths.timeseries;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * A 1D data stream for, e.g. a sin curve with 1=one hour, steps every 1/100 of
 * an hour
 * 
 * @author daniel
 * @see FilteredDataStream to apply a function to a base data stream.
 */
public abstract class FunctionDataStream extends ADataStream {

	private static final long serialVersionUID = 1L;
	private final Dt dt;
	Time end;
	private Object label;
	Time start;
	private final TUnit unit;

	/**
	 * x will be measured in dt's units. E.g. if dt = 5 minutes, then x will go
	 * 0, 5, 10, 15... The start time is set as *now* (can be changed using
	 * {@link #setStart(Time)}).
	 * 
	 * @param unit
	 *            the x values will be measured in this unit
	 * @param dt
	 *            Steps between values of x
	 */
	public FunctionDataStream(Dt dt) {
		super(1);
		this.dt = dt;
		this.unit = dt.getUnit();
		setStart(new Time());
	}

	/**
	 * x is measured in dt's units. E.g. if dt = 5 minutes, then x will go 0, 5,
	 * 10, 15...
	 * 
	 * @param x
	 *            time from start
	 * @return
	 */
	protected abstract double f(double x);

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			Time now = start == null ? new Time() : start;

			@Override
			protected Datum next2() throws Exception {
				Time time = now;
				now = now.plus(dt);
				if (end != null && now.isAfter(end))
					return null;
				double x = (now.longValue() - start.longValue())
						/ unit.getDt().getMillisecs();
				double fx = f(x);
				return new Datum(time, fx, label);
			}
		};
	}

	/**
	 * An optional end to the sequence. Otherwise it's infinite.
	 * 
	 * @param end
	 */
	public void setEnd(Time end) {
		this.end = end;
	}

	public void setLabel(Object label) {
		this.label = label;
	}

	public void setStart(Time time) {
		start = time;
	}

}
