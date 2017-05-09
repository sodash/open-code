package com.winterwell.datalog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.winterwell.datalog.DataLog.KInterpolate;

import com.winterwell.depot.Depot;
import com.winterwell.depot.Desc;
import com.winterwell.depot.MetaData;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;


/**
 * 
 * @testedby {@link CSVStorageTest}
 * @author Agis Chartsias <agis@winterwell.com>
 *
 */
public class CSVStorage implements IDataLogStorage {

	@Override @Deprecated
	public void setHistory(Map<Pair2<String, Time>, Double> tagTime2set) {
		// unsupported
	}
	
	public IFuture<MeanRate> getMean(Time start, Time end, String tag) {
		throw new TodoException(); // TODO		
	}
	
	// FIXME any reason to use Depot?
	Depot depot = Depot.getDefault();
	DataLogConfig config;
	
	public CSVStorage() {
		this(new DataLogConfig());
	}
	
	public CSVStorage(DataLogConfig config) {
		init(config);
	}
	
	@Override
	public IDataLogStorage init(DataLogConfig config) {
		this.config = config;
		return this;
	}
		
	/**
	 * Use IDataStream csv format: timestamp, tag, value.
	 * 
	 * @param period
	 * @param tag2count
	 * @param tag2mean
	 */	
	@Override
	public void save(Period period, Map<String, Double> tag2count, Map<String, MeanVar1D> tag2mean) {
		File csv = getFile(period.getStart());
		Log.d(DataLog.LOGTAG, "saving "+tag2count.size()+" to "+csv+": "+tag2count);
		csv.getParentFile().mkdirs();
		FileLock lock = null;
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(csv, true);
			// Suppose 2 JVMs are running... Protect our file edits!
			lock = fout.getChannel().lock();
			BufferedWriter bw = FileUtils.getWriter(fout);
			CSVWriter w = new CSVWriter(bw, ',', '"'); // append to existing
			
			// Save as the middle of the period?!
			Time mid = DataLogImpl.doSave3_time(period);
	//		Set<String> written = new HashSet(); guard against count & mean??
			for(Map.Entry<String,Double> e : tag2count.entrySet()) {			
				csv.getParentFile().mkdirs();
//				Log.v(Stat.LOGTAG, "saving "+e.getKey()+"="+e.getValue()+" to "+csv);
	//			String lbl = tag2event.get(e.getKey()); ??
				w.write(mid.getTime(), e.getKey(), e.getValue());
			}
			
			for(Map.Entry<String,MeanVar1D> e : tag2mean.entrySet()) {
//				Log.v(Stat.LOGTAG, "saving "+e.getKey()+"="+e.getValue()+" to "+csv);	
				MeanVar1D mv = e.getValue();
	//			String lbl = tag2event.get(e.getKey()); ??
				// mean first, so if you're just grabbing the 1st value you get the "right" one
				w.write(mid.getTime(), e.getKey(), mv.getMean(), mv.getVariance(), mv.getMin(), mv.getMax());
			}
			w.close();
			
	//		if ( ! tag2event.isEmpty()) {
	//			CSVWriter w2 = new CSVWriter(csv, ',', true); // append to existing
	//			for(Map.Entry<String,String> e : tag2event.entrySet()) {
	//				
	//			}
				// TODO
	//		}
		} catch (IOException ex) {
			throw Utils.runtime(ex);
		} finally {
			FileUtils.close(fout);
			try {
				if (lock!=null) lock.release();
			} catch (ClosedChannelException ex) {
				// ignore
			} catch (IOException e) {
				Log.e(DataLog.LOGTAG, e);
			}
		}
	}
	
	// Unsupported for CSV.
	@Override @Deprecated
	public void saveHistory(Map<Pair2<String, Time>, Double> tag2time2count) {
	}
		
	/**
	 * 
	 * @param key
	 * @param period Will be rounded to the surrounding day by {@link #getDesc(String, Period)}
	 * @return
	 */
	protected File getFile(Time t) {		
		Desc<File> desc = getDesc(t);
		MetaData md = depot.getMetaData(desc);
		return md.getFile();
	}

	Desc<File> getDesc(Time start) {
		// one file per day?
		Dt fp = config.filePeriod;			
		// drop the remainder
		long _start = (start.getTime() / fp.getMillisecs()) * fp.getMillisecs(); 		
		Time s = new Time(_start);
		Time e = s.plus(fp);
		return getDesc2(null, s, e);
	}
	
	Desc<File> getDesc2(String host, Time s, Time e) {
		Desc<File> desc = new Desc("all.csv", File.class);
		desc.setRange(s, e);		
		desc.setTag("datalog");
		// namespace?
		if ( ! Utils.isBlank(config.namespace)) {
			desc.put("n", config.namespace);
		}
		desc.setServer(host==null? Desc.MY_SERVER() : host);
		return desc;
	}


	@Override
	public IFuture<IDataStream> getData(Pattern id, Time _start, Time end) {
		return new StatReqCSV<IDataStream>(KStatReq.DATA, id, _start, end);
	}

	/**
	 * TODO How can we return spread information? i.e. mean/var etc.??
	 * 
	 * @WARNING bucket filtering is done by mid-point value, and you only get buckets
	 * whose mid-points fall within start & end.
	 * 
	 * @param id
	 * @param start Can be null (includes all)
	 * @param end Can be null (includes all)
	 * @return never null! May be empty.
	 */
	@Override
	public StatReq<IDataStream> getData(String id, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		return new StatReqCSV<IDataStream>(KStatReq.DATA, id, start, end, fn, bucketSize);
	}
	
	@Override
	public StatReq<Double> getTotal(String tag, Time start, Time end) {
		return new StatReqCSV<Double>(KStatReq.TOTAL, tag, start, end, null, null);
	}
	
	/**
	 * Pattern and tags are ignored, since CSV cannot filter.
	 * @return Can be null
	 */
	@Override
	public CSVReader getReader(String server, Time s, Time e, Pattern tagMatcher, String tag) {
		Desc<File> desc = getDesc2(server, s, e);
		File csv = depot.get(desc);
		if (csv==null) {
			// no data
			return null;
		}
		CSVReader r = new CSVReader(csv, ',');
		r.setNumFields(-1);
		return r;
	}

	@Override
	public StatReq<IDataStream> getMeanData(String tag, Time start, Time end,
			KInterpolate fn, Dt bucketSize) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public Object saveEvent(String dataspace, DataLogEvent event, Period period) {
		throw new TodoException();
	}

	@Override
	public void saveEvents(Collection<DataLogEvent> values, Period period) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerEventType(String dataspace, String eventType) {
	}
}
