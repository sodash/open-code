package com.winterwell.utils.time;

import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;

import com.winterwell.utils.time.TimeUtils;

import junit.framework.TestCase;

public class TimeUnitTest extends TestCase {

	public void testConvertDoubleTimeUnit() {
		{
			double m = new Dt(15, TUnit.SECOND).convertTo(TUnit.MINUTE)
					.getValue();
			assert m == 0.25 : m;
		}
		{
			double m = TUnit.SECOND.convert(0.25, TUnit.MINUTE);
			assert m == 15 : m;
		}
	}

	public void testConvertDt() {
		{
			Dt dt = new Dt(5, TUnit.MINUTE);
			Dt dt2 = dt.convertTo(TUnit.SECOND);
			assert dt2.getValue() == 300;
			assert dt2.getMillisecs() == 300 * 1000;
			assert dt2.getUnit() == TUnit.SECOND;
		}
		{
			Dt dt = new Dt(2, TUnit.YEAR);
			Dt dt2 = dt.convertTo(TUnit.MONTH);
			assert dt2.getValue() == 24;
			assert dt2.getUnit() == TUnit.MONTH;
		}
	}

	public void testParseDt() {
		{
			Dt dt = TimeUtils.parseDt("2 hour");
			assert dt.getUnit() == TUnit.HOUR : dt;
			assert dt.getValue() == 2 : dt;
		}
		{
			Dt dt = TimeUtils.parseDt("3.5 minutes");
			assert dt.getUnit() == TUnit.MINUTE : dt;
			assert dt.getValue() == 3.5 : dt;
		}
		{
			Dt dt = TimeUtils.parseDt("a minute");
			assert dt.getUnit() == TUnit.MINUTE : dt;
			assert dt.getValue() == 1 : dt;
		}
		{
			Dt dt = TimeUtils.parseDt("hour");
			assert dt.getUnit() == TUnit.HOUR : dt;
			assert dt.getValue() == 1 : dt;
		}
	}

}
