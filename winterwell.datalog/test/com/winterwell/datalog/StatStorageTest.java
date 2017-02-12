package com.winterwell.datalog;

import org.junit.Test;

import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class StatStorageTest extends DatalogTestCase {

	@Test
	public void testGetData() {
		// force an init
		DataLog.count(1, "dummy");

		StatImpl ss = (StatImpl) DataLog.dflt;
		StatReq<IDataStream> dr = ss.getData(new Time().minus(TUnit.WEEK), new Time(), null, null, "Cache_hit");
		IDataStream data = dr.get();
		
		assert !data.isEmpty();
		for (Datum datum : data) {
			assert datum != null;
			System.out.println("\t"+datum);
		}
	}
	
	@Test
	public void testGetData2() {
		// force an init
		DataLog.count(1, "dummy");

		StatImpl ss = (StatImpl) DataLog.dflt;
		String[] tagBits = {"test", "foo"};
		StatReq<IDataStream> bg = ss.getData(null, new Time().plus(TUnit.DAY), null, null, tagBits);
		IDataStream data = bg.get();
		System.out.println(data);
	}


//	@Test
//	public void testRemote() {
//		StatConfig config = new StatConfig();
//		StatImpl ss = new StatImpl(config);
//		= StatDataReq.getRemoteData("egan.winterwell.com", "Cache_hit", new Time().minus(TUnit.WEEK), new Time());
//		IDataStream data ;
//		System.out.println(data);
//		assert ! data.isEmpty();
//		for (Datum datum : data) {
//			assert datum != null;
//			System.out.println("\t"+datum);
//			break;
//		}
//	}
	
}
