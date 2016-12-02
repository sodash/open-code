package com.winterwell.utils.time;

import com.winterwell.utils.containers.AbstractIterator;

/**
 * Iterable over "on" periods (e.g. working days).
 * @author daniel
 * @testedby {@link OfficeHoursPeriodTest}
 */
public class OfficeHoursPeriod implements Iterable<Period> {

	IShift oh;
	Time start;
	Time end;

	public OfficeHoursPeriod(IShift hours, Time start, Time end) {
		oh = hours;
		this.start = start;
		this.end = end;
	}
	
	/**
	 * @return
	 */
	public Dt getTotalOfficeTime() {
		Dt total = new Dt(0);
		for (Period p : this) {
			Dt len = p.length();
			total = total.plus(len);
		}
		return total;
	}

	@Override
	public AbstractIterator<Period> iterator() {
		return new OHIt(start);
	}
	
	
	class OHIt extends AbstractIterator<Period>
	{
		private Time now;
		
		OHIt(Time start) {
			now = start; //.minus(TUnit.MILLISECOND);	
		}	

		@Override
		protected Period next2() throws Exception {
			if (now.isAfter(end)) {
				return null;
			}
			Time s = oh.nextOn(now);
			// we might now have advanced past end
			if (s.isAfter(end)) {
				return null;
			}
			Time e = oh.nextOff(s);
			// cap e?
			if (e.isAfter(end)) {
				e = end;
			}
			// step over the end (to avoid fencepost errors)
			now = e.plus(TUnit.MINUTE);
			return new Period(s, e);
		}
	}
}


