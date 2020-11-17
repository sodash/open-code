package com.winterwell.datalog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.io.DBOptions;
import com.winterwell.utils.io.SqlUtils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * Super class that initialises the db connection for SQL tests
 * or the CSV implementation for CSV tests.
 *  
 * @author Agis Chartsias <agis@winterwell.com>
 *
 */
public class DatalogTestCase {

	public DatalogTestCase() {
//		initSQL(); not by default
	}
	
	/**
	 * Init a SQL-backed stat
	 */
	public void initSQL() {
		setupDB();
		
		DataLogConfig config = new DataLogConfig();
		config.storageClass = SQLStorage.class;
		config.interval = new Dt(5, TUnit.SECOND);
		
		config.dbUrl = "jdbc:postgresql://localhost/sodash";
		config.dbUser = "worker";
		config.dbPassword = "winterwell";
		
		DataLog.dflt = new DataLogImpl(config);
		DataLog.init(config);
	}
	
	private void setupDB() {
		DBOptions dbo = new DBOptions();
		dbo.dbUrl = "jdbc:postgresql://localhost/sodash";
		dbo.dbUser = "worker";
		dbo.dbPassword = "winterwell";

		SqlUtils.setDBOptions(dbo);
	}
	
	/**
	 * Init a CSV-backed stat
	 */
	public void initCSV() {
		DataLogConfig config = new DataLogConfig();
		config.interval = new Dt(5, TUnit.SECOND);
		config.storageClass = CSVStorage.class;
		DataLog.dflt = new DataLogImpl(config);
		DataLog.init(config);
	}
	
	/**
	 * This is similar to {@link DataLogImpl#doSave()}, without the time filtering.
	 */
	public static Period saveData(DataLogImpl si, double count, Object... tags) throws InterruptedException {
		long s1 = System.currentTimeMillis();
		Thread.sleep(10);
		si.count(count, tags);
		Thread.sleep(10);
		long e1 = System.currentTimeMillis();
		
		Time ts1 = new Time(s1);
		Time te1 = new Time(e1);
		Period period = new Period(ts1, te1);
		
		Map<String, Double> old = si.tag2count;		
		Map<String, IDistribution1D> oldMean = si.tag2dist;	
		
		si.save(period, old, oldMean);
		si.tag2count = new ConcurrentHashMap();
		si.tag2dist = new ConcurrentHashMap();
		return period;
	}
	

	/**
	 * Checks that the data stream is in order, and that its results match the resultArray given
	 * @param l
	 * @param lastTime 
	 * @param arrayOfExpectedResults an array of expected results.
	 * @return
	 */
	public boolean isDataStreamValid(ListDataStream l,
			double[] arrayOfExpectedResults) {
		Time lastTime = TimeUtils.ANCIENT;
		for (int x = 0; x < l.size() ; x++){
			Datum d = (Datum) l.get(x);
			Time t = d.getTime();
			assert t.isAfter(lastTime) : "Time:" +t+"\n"+d+" "+l;
			lastTime = t;
			double dx = d.x();
			assert dx == arrayOfExpectedResults[x] : x+") "+d.getData()[0]+" != "+arrayOfExpectedResults[x];
		}
		return true;
	}
}
