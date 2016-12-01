package com.winterwell.datalog;

import org.junit.Test;

import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.TimeSlicer;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

import com.winterwell.utils.threads.IFuture;

public class MiniReportTest extends DatalogTestCase {

	@Test
	public void testMiniReport() {
		// setup some data
		Time a = new Time(2014, 1, 1);
		Time b = new Time(2014, 1, 5); // 2 days later
		TimeSlicer ts = new TimeSlicer(a, b, TUnit.HOUR.dt);
		for (Period period : ts) {
			Stat.set(period.getStart(), period.getStart().getDayOfMonth(), "test","repdata");
		}		
		Stat.flush();
		
		// read it
		getMiniReport(b, TUnit.DAY, "test","repdata");
	}

	private void getMiniReport(Time now, TUnit day, String... tagBits) {
		IFuture<Iterable> _data = Stat.getData(now.minus(2, day), now, null, day.dt, tagBits);
		IDataStream data = (IDataStream) _data.get();
		for (Datum datum : data) {
			System.out.println(datum.getTime()+"\t"+datum.x());
		}
		// looks kind of kosher :)
	}
}
