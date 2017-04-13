package com.winterwell.maths.timeseries;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;

/**
 * A data stream which just counts up. For use in testing
 * 
 * @author daniel
 * 
 */
public class CounterDataStream extends ADataStream {

	private static final long serialVersionUID = 1L;
	private Dt dt;
	private double dx = 1;
	private Object label;
	private Time start;
	private double startX;

	/**
	 * 
	 * @param dt
	 * @param x
	 *            First value
	 */
	public CounterDataStream(Dt dt, double x) {
		super(1);
		this.dt = dt;
		this.startX = x;
	}

	@Override
	public Dt getSampleFrequency() {
		return dt;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			Time now = start == null ? new Time() : start;
			private double x = startX;

			@Override
			protected Datum next2() {
				Datum d = new Datum(now, x, label);
				now = now.plus(dt);
				x += dx;
				return d;
			}
		};
	}

	public void setDx(double dx) {
		this.dx = dx;

	}

	public void setLabel(Object label) {
		this.label = label;
	}

	public void setTime(Time time) {
		start = time;
	}

}
