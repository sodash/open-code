package com.winterwell.datalog;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class DataLogRemoteStorageTest {

//	@Test
	public void testGetDataStringTimeTimeKInterpolateDt() {
		fail("Not yet implemented");
	}
	
	
	@Test
	public void testHackUse() {
		{
			String dataspace = "test";
			double count = 2.1;
			DataLogEvent event = new DataLogEvent(dataspace, count, "woot", 
					new ArrayMap("n", 7, "w", 100));
			boolean ok = DataLogRemoteStorage.saveToRemoteServer("http://locallg.good-loop.com", event);
			assert ok;
		}
	}


	@Test
	public void testDirectUse() {				
		DataLogRemoteStorage dlrs = new DataLogRemoteStorage();
		DataLogConfig remote = new DataLogConfig();
		remote.logEndpoint = "https://lg.good-loop.com/lg";
		dlrs.init(remote);
		Dataspace dataspace = new Dataspace("test");
		DataLogEvent event = new DataLogEvent(dataspace, 2, "woot", 
				new ArrayMap("n", 7, "w", 100));
		Object ok = dlrs.saveEvent(dataspace, event, null);
		System.out.println(ok);
	}

	/**
	 * Warning: Tests against testlg -- which is often not running the latest code.
	 */
	@Test
	public void testSaveEvent() {				
		DataLogConfig dc = new DataLogConfig();
		dc.storageClass = DataLogRemoteStorage.class;
		dc.logEndpoint = "https://testlg.good-loop.com/lg";
		dc.getDataEndpoint = "https://testlg.good-loop.com/data";
		DataLog.init(dc);
		
		DataLog.count(1, "testSaveEvent");
		DataLog.flush();
		
		DataLogRemoteStorage storage = (DataLogRemoteStorage) DataLog.getImplementation().getStorage();
		StatReq<IDataStream> data = storage.getData("testSaveEvent", new Time().minus(TUnit.MINUTE), new Time(), null, null);
		System.out.println(data);
	}

}
