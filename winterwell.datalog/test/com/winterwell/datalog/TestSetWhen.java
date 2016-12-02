package com.winterwell.datalog;

import org.junit.Test;

import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Utils;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class TestSetWhen extends DatalogTestCase {
	
	@Test
	public void testWhenInsert() {
		StatImpl si = (StatImpl) Stat.dflt;
		String salt = Utils.getRandomString(6);
		si.set(new Time(2015,1,1), 10, "test", salt);
		si.set(new Time(2015,1,1,6,0,0), 15, "test", salt);
		Stat.set(new Time(2015,1,2), 20, "test", salt);
		
		si.flush();
		
		Time start = new Time(2015,1,1).minus(TUnit.SECOND);
		Time end = new Time(2015,1,3);
		IFuture<IDataStream> data = si.getData(start, end, null, null, "test", salt);
		ListDataStream list = (ListDataStream) data.get();		
		System.out.println(list);	
		assert ! list.isEmpty();
		assert isDataStreamValid(list, new double[]{10,15,20});
		
		IStatReq<Double> total = si.getTotal(start, end, "test", salt);
		Double t = total.get();
		assert t == 45 : t;
	}
	
	@Test
	public void testWhenUpdate() {
		StatImpl si = (StatImpl) Stat.dflt;
		String salt = Utils.getRandomString(6);
		si.set(new Time(2015,1,1), 10, "test", salt);
		Stat.set(new Time(2015,1,2), 20, "test", salt);		
		si.flush();
		
		// overwrite and insert
		si.set(new Time(2015,1,1), 15, "test", salt);
		Stat.set(new Time(2015,1,3), 30, "test", salt);		
		si.flush();
				
		Time start = new Time(2015,1,1).minus(TUnit.SECOND);
		Time end = new Time(2015,1,4);
		IFuture<IDataStream> data = si.getData(start, end, null, null, "test", salt);
		ListDataStream list = (ListDataStream) data.get();		
		System.out.println(list);	
		assert ! list.isEmpty();
		assert isDataStreamValid(list, new double[]{15,20,30});
		

		IStatReq<Double> total = si.getTotal(start, end, "test", salt);
		Double t = total.get();
		assert t == 65 : t;
	}

}
