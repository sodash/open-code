package com.winterwell.utils.time;

import java.util.List;

import org.junit.Test;

public class ShiftSlicerTest {

	@Test
	public void testHourSlices() {
		OfficeHours base = new OfficeHours(TimeUtils._GMT_TIMEZONE);
		ShiftSlicer ss = new ShiftSlicer(TUnit.HOUR.dt, base);
		List<Period> shifts = TimeUtils.getShifts(ss, new Time(), new Time().plus(TUnit.DAY));
		System.out.println(shifts);
		for (Period period : shifts) {
			assert period.getMillisecs() <= TUnit.HOUR.millisecs : period;
		}
		assert shifts.size() >= 7;
	}

	
	@Test
	public void testShiftStart247() {
		IShift base = IShift.ROUND_THE_CLOCK;
		ShiftSlicer ss = new ShiftSlicer(TUnit.HOUR.dt, base);
		List<Period> shifts = TimeUtils.getShifts(ss, new Time(2015, 6, 10), new Time(2015, 6, 11));
		System.out.println(shifts);
		for (Period period : shifts) {
			assert period.getMillisecs() <= TUnit.HOUR.millisecs : period;
		}
		assert shifts.size() >= 7;
		Period s0 = shifts.get(0);
		Period sl = shifts.get(shifts.size() - 1);
		assert s0.getMillisecs() == TUnit.HOUR.getMillisecs() : s0.getMillisecs();
		assert sl.getMillisecs() == TUnit.HOUR.getMillisecs() : sl.getMillisecs();
		assert s0.getStart().equals(new Time(2015, 6, 10));
		assert s0.getEnd().equals(new Time(2015, 6, 10, 1, 0, 0));
		assert sl.getStart().equals(new Time(2015, 6, 10, 23, 0, 0)) : sl;
		assert sl.getEnd().equals(new Time(2015, 6, 11)) : sl;
	}

}
