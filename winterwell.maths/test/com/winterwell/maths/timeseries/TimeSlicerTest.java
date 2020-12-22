package com.winterwell.maths.timeseries;

import java.util.List;

import org.junit.Test;

import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeIterator;

public class TimeSlicerTest {

	@Test
	public void testAprilOddity() {
		TimeSlicer ts = new TimeSlicer(new Time(2020,1,1), new Time(2022,9,30), TUnit.MONTH.dt);		
		int mi = ts.getBucket(new Time(2020,3,1));
		int ai = ts.getBucket(new Time(2020,4,1));
		assert mi == 2;
		assert ai == 3;
	}
	
	@Test
	public void testGetBucket() {
		TimeSlicer bucketer = new TimeSlicer(new TimeIterator(new Time(2011, 1,
				1, 0, 0, 0), new Time(2011, 1, 1, 12, 0, 0), new Dt(3,
				TUnit.HOUR)));
		int b = bucketer.size();
		assert b == 4 : b;
		int n = bucketer.getBucket(new Time(2011, 1, 1, 1, 0, 0));
		assert n == 0 : n;
		n = bucketer.getBucket(new Time(2011, 1, 1, 4, 0, 0));
		assert n == 1 : n;

		n = bucketer.getBucket(new Time(2011, 1, 1, 12, 30, 0));
		assert n == 3 : n;
	}
	
	@Test
	public void testIterate() {
		TimeSlicer bucketer = new TimeSlicer(
				new Time(2011, 1, 1, 0, 0, 0), 
				new Time(2011, 1, 2, 12, 0, 0), 
				new Dt(3, TUnit.HOUR));
		List<Period> periods = Containers.getList(bucketer);
		Period p0 = periods.get(0);
		assert p0.getStart().equals(new Time(2011, 1, 1, 0, 0, 0));
		assert p0.getEnd().equals(new Time(2011, 1, 1, 3, 0, 0));
		
		Period pe = periods.get(periods.size() - 1);
		assert pe.getEnd().equals(new Time(2011, 1, 2, 12, 0, 0));
	}

	
	@Test
	public void testBucketTimeLabels() {
		TimeSlicer bucketer = new TimeSlicer(new Time(2013, 1, 1), new Time(2013, 1, 4), TUnit.DAY.dt);
		for(double t : bucketer.times) {
			System.out.println(new Time(t));
		}
		System.out.println("Datums");
		ListDataStream list = new ListDataStream(1, bucketer);
		for (Datum datum : list) {
			System.out.println(datum.time);
		}
	}
	
	@Test
	public void test1YearEndIncluded() {
		{
			TimeSlicer bucketer = new TimeSlicer(new Time(2013, 1, 1), new Time(2013, 1, 1).plus(TUnit.YEAR), TUnit.MONTH.dt);
			for(double t : bucketer.times) {
				System.out.println(new Time(t));
			}
			int jan = bucketer.getBucket(new Time(2013,1,1));
			int dec = bucketer.getBucket(new Time(2013,12,1));
			assert jan == 0;
			assert dec == 11;
		}
		System.out.println("");
		{
			TimeSlicer bucketer = new TimeSlicer(new Time(2013, 1, 1), new Time(2013, 12, 31), TUnit.MONTH.dt);
			for(double t : bucketer.times) {
				System.out.println(new Time(t));
			}
			int jan = bucketer.getBucket(new Time(2013,1,1));
			int dec = bucketer.getBucket(new Time(2013,12,1));
			assert jan == 0;
			assert dec == 11;
		}
	}
	

	@Test
	public void testOneSmallBucket() {
//		try {
			TimeIterator tit = new TimeIterator(new Time(2011, 1, 1, 0, 0, 0), new Time(2011, 1, 1, 0, 0, 10), TUnit.HOUR.dt);
			TimeSlicer bucketer = new TimeSlicer(tit);
			int b = bucketer.size();
			assert b == 1;
			Time in = new Time(2011, 1, 1, 0, 0, 5);
			Time out1 = new Time(2011, 1, 1, 0, 0, 15);
			Time out2 = new Time(2011, 1, 2, 0, 0, 0);
			assert bucketer.contains(in);
			assert ! bucketer.contains(out2);
			assert ! bucketer.contains(out1);
//		} catch(Exception ex) {
//			// oh well - I suppose that's OK behaviour
//			System.out.println(ex);
//		}
	}

}
