package com.winterwell.datalog;

import java.io.IOException;

import org.junit.Test;

import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class StatSpeedTest extends DatalogTestCase {
	
	
	@Test
	public void testSmoke() throws IOException {
		StatConfig config = new StatConfig();
		config.interval = TUnit.SECOND.dt;
		DataLog.setConfig(config);
//		assert Stat.saveThread != null;
		Time start = new Time();
		Utils.sleep(30000); // wait for it to start saving
		for(int i=0; i<10; i++) {
			DataLog.count(1, "test","1");
			DataLog.count(1, "test","2");
			Utils.sleep(300);
		}
		Time end = new Time();
		// flush the data
		DataLog.flush();
//		Stat.close();
		
		String tag = DataLog.tag("test","1");
		ListDataStream data = (ListDataStream) DataLog.getData(start, end, null, null, tag).get();
		System.out.println(data);
		assert ! data.isEmpty();
		for (Datum datum : data) {
			assert datum != null;
			System.out.println("\t"+datum);
		}
		
		IDataStream data2 = (IDataStream) DataLog.getData(start, end, null, null, "test").get();
		System.out.println(data2);
		for (Datum datum : data2) {
			assert datum != null;
			System.out.println("\t"+datum);
		}
	}

}
