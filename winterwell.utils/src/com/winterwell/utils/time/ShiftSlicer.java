package com.winterwell.utils.time;

import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;

public class ShiftSlicer implements IShift {

	IShift base;
	Dt dt;	
	
	public ShiftSlicer(Dt dt, IShift base) {
		this.dt = dt;
		this.base = base;
		assert dt.getMillisecs() > 0;
		assert base != null;
	}
	
	@Override
	public boolean isOn(Time now) {
		return base.isOn(now);
	}
	
	@Override
	public Time shiftStart(Time now) {
		Time bs = base.shiftStart(now);
		assert base.isOn(bs) : base+" "+now+" "+bs;
		Time s = bs;
		while(s.diff(now) > dt.getMillisecs()) {
			Time s2 = s.plus(dt);
			if ( ! base.isOn(s2.minus(TUnit.MILLISECOND))) {
				return s;
			}
			s = s2;
		}
		assert base.isOn(s) : base+" "+now+" "+s;
		return s;
	}
	
	@Override
	public Time nextOn(Time now) {
		Time on = base.nextOn(now);
		return on;
	}

	@Override
	public Time nextOff(Time now) {
		if ( ! isOn(now)) {
			return now;
		}
		Time on = shiftStart(now);
		assert now.isAfter(on) || now.equals(on) : on+" v "+now;
		Time off = base.nextOff(now);
		Time maxOff = on.plus(dt);
		if (maxOff.isBefore(off)) return maxOff;
		return off;
	}

}
