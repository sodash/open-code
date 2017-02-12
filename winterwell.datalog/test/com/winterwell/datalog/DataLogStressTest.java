package com.winterwell.datalog;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Utils;
import com.winterwell.utils.threads.SafeExecutor;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.RateCounter;
import com.winterwell.utils.time.StopWatch;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;


public class DataLogStressTest extends DatalogTestCase {

	@Test
	public void testStressCSV() throws IOException, InterruptedException {
		StatConfig config = new StatConfig();
		config.interval = new Dt(5, TUnit.SECOND);
		config.storageClass = CSVStorage.class;
		DataLog.dflt = new StatImpl(config);
		DataLog.setConfig(config);
		stress();
	}
	
	@Test
	public void testStressSQL() throws IOException, InterruptedException {
		StatConfig config = new StatConfig();
		new SQLStorage(config).initStatDB();		
		config.interval = new Dt(5, TUnit.SECOND);
		config.storageClass = SQLStorage.class;
		DataLog.dflt = new StatImpl(config);
		DataLog.setConfig(config);
		stress();
	}
	
	public void stress() throws IOException, InterruptedException {
//		assert Stat.saveThread != null;
		// use a big bucket, otherwise we can miss out
		Time start = new Time();
		
		Utils.sleep(5000); // wait for it to start saving
		
		System.out.println("Go!");		
		StopWatch sw = new StopWatch();
		SafeExecutor se = new SafeExecutor(Executors.newFixedThreadPool(100));
		final Gaussian1D g = new Gaussian1D(0, 1);
		final RateCounter rc = new RateCounter(TUnit.SECOND.dt);
		for(int i=0; i<1000000; i++) { 
			final int j = i;
			se.submit(new Runnable() {				
				@Override
				public void run() {
					rc.plus(1);
					DataLog.count(1, "stress", "1");
					DataLog.set(g.sample(), "stress", "2");	
				}
			});
		}
		se.shutdown();
		se.awaitTermination(10, TimeUnit.SECONDS);
		
		DataLog.flush();
		
		Time end = new Time();		
		sw.print();
		
		String[] tagBits = {"stress", "1"};
		ListDataStream data = (ListDataStream) DataLog.getData(start, start.plus(TUnit.MINUTE), null, null, tagBits).get();
		System.out.println(data);
		assert ! data.isEmpty();
		for (Datum datum : data) {
			assert datum != null;
			System.out.println("stress/1\t"+datum);
		}
		
		IDataStream data2 = (IDataStream) DataLog.getData(start, end, null, null, "stress.2").get();
		System.out.println(data2);
		for (Datum datum : data2) {
			assert datum != null;
			System.out.println("stress.2\t"+datum);
		}
		System.out.println(rc);
	}

}
