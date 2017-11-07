package com.winterwell.datalog;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

public class DataLogRemoteStorageTest {

//	@Test
	public void testGetDataStringTimeTimeKInterpolateDt() {
		fail("Not yet implemented");
	}

	@Test
	public void testSaveEvent() {				
		DataLogConfig dc = new DataLogConfig();
		dc.storageClass = DataLogRemoteStorage.class;
		dc.logEndpoint = "http://locallg.good-loop.com/lg";
		dc.getDataEndpoint = "http://locallg.good-loop.com/data";
		DataLog.setConfig(dc);
		
		DataLog.count(1, "testSaveEvent");
		DataLog.flush();
		
		DataLogRemoteStorage storage = (DataLogRemoteStorage) DataLog.getImplementation().getStorage();
		StatReq<IDataStream> data = storage.getData("testSaveEvent", new Time().minus(TUnit.MINUTE), new Time(), null, null);
		System.out.println(data);
	}

}
