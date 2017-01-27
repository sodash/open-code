package com.winterwell.datalog;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import com.winterwell.datalog.Stat.KInterpolate;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;

public class ESStorage implements IStatStorage {

	@Override
	public void save(Period period, Map<String, Double> tag2count, Map<String, MeanVar1D> tag2mean) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveHistory(Map<Pair2<String, Time>, Double> tag2time2count) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IFuture<IDataStream> getData(Pattern id, Time start, Time end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StatReq<IDataStream> getData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StatReq<Double> getTotal(String tag, Time start, Time end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator getReader(String server, Time start, Time end, Pattern tagMatcher, String tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFuture<MeanRate> getMean(Time start, Time end, String tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StatReq<IDataStream> getMeanData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setHistory(Map<Pair2<String, Time>, Double> tagTime2set) {
		// TODO Auto-generated method stub
		
	}

}
