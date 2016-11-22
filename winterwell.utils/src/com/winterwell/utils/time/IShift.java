package com.winterwell.utils.time;

public interface IShift {
	
	static final IShift ROUND_THE_CLOCK = new RoundTheClock();

	boolean isOn(Time now);
	
	/**
	 * @param t Cannot be null
	 * @return next start-on-time after-or-equal t (ie. = t if t is "on")
	 */
	Time nextOn(Time now);
	
	/**
	 * 
	 * @param t Cannot be null
	 * @return next end-off-time after-or-equal t (ie. = t if t is "off").
	 */
	Time nextOff(Time now);
	

	/**
	 * @param t Cannot be null
	 * @return previous start-on-time before-or-equal t
	 */
	Time shiftStart(Time now);
	
}

class RoundTheClock implements IShift {
	
	@Override
	public boolean isOn(Time now) {
		return true;
	}

	@Override
	public Time nextOn(Time now) {
		return now;
	}

	@Override
	public Time nextOff(Time now) {
		Time longShift = now.plus(TUnit.MONTH);
		return longShift;
	}
	
	@Override
	public Time shiftStart(Time now) {
		Time m1 = now.minus(TUnit.MONTH);
		// zero the time-of-day? HACK this ignores timezone (which we dont know)
		Time m2 = new TimeOfDay(0, 0).set(m1);
		return m2;
	}
}