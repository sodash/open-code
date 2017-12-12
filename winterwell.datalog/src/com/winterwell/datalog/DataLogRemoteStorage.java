package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.datalog.DataLogConfig;
import com.winterwell.datalog.DataLogEvent;
import com.winterwell.datalog.MeanRate;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.Dep;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.web.FakeBrowser;

/**
 * TODO Remote server storage for DataLog
 * So the adserver can log stuff into lg.
 * @author daniel
 *
 */
public class DataLogRemoteStorage implements IDataLogStorage
{

	private String logEndpoint;
	private String getDataEndpoint;


	@Override
	public IDataLogStorage init(DataLogConfig settings) {
		logEndpoint = settings.logEndpoint;
		getDataEndpoint = settings.getDataEndpoint;
		return this;
	}

	@Override
	public void save(Period period, Map<String, Double> tag2count, Map<String, IDistribution1D> tag2mean) {
		Collection<DataLogEvent> events = new ArrayList();
		for(Entry<String, Double> tc : tag2count.entrySet()) {
			DataLogEvent event = new DataLogEvent(tc.getKey(), tc.getValue());
			events.add(event);
		}
		for(Entry<String, IDistribution1D> tm : tag2mean.entrySet()) {
			IDistribution1D distro = tm.getValue();
			DataLogEvent event = new DataLogEvent(tm.getKey(), distro.getMean());
			if (distro instanceof IHasJson) {
				// paranoid defensive copy
				ArrayMap json = new ArrayMap(((IHasJson) distro).toJson2());
				event.setExtraResults(json);
			}
			events.add(event);
		}
		saveEvents(events, period);
	}

	@Override
	public void saveHistory(Map<Pair2<String, Time>, Double> tag2time2count) {		
		for(Entry<Pair2<String, Time>, Double> tc : tag2time2count.entrySet()) {
			DataLogEvent event = new DataLogEvent(tc.getKey().first, tc.getValue());
			event.time = tc.getKey().second;
			// Minor TODO batch for efficiency
			Collection<DataLogEvent> events = new ArrayList();
			events.add(event);		
			DataLogImpl dl = (DataLogImpl) DataLog.getImplementation();
			Period bucketPeriod = dl.getBucket(event.time); 
			saveEvents(events, bucketPeriod);
		}		
	}

	@Override
	public IFuture<IDataStream> getData(Pattern id, Time start, Time end) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public StatReq<IDataStream> getData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		FakeBrowser fb = new FakeBrowser();
		
		fb.setAuthentication("daniel@local.com@email", "1234");
		fb.setDebug(true);
		Map<String, String> vars = new ArrayMap(
			"q", "tag:"+tag,
			"breakdown", "time"
				);
		vars.put("d", DataLog.getDataspace());
		vars.put("t", DataLogEvent.simple); // type
		String res = fb.getPage(getDataEndpoint, vars);
		Object jobj = JSON.parse(res);
		throw new TodoException();
	}

	@Override
	public StatReq<Double> getTotal(String tag, Time start, Time end) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public Iterator getReader(String server, Time start, Time end, Pattern tagMatcher, String tag) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public IFuture<MeanRate> getMean(Time start, Time end, String tag) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public StatReq<IDataStream> getMeanData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public void setHistory(Map<Pair2<String, Time>, Double> tagTime2set) {
		// TODO Auto-generated method stub
		if (Utils.isEmpty(tagTime2set)) return;
		Log.w(new TodoException(tagTime2set));	
	}

	@Override
	public Object saveEvent(String dataspace, DataLogEvent event, Period period) {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		Map<String, String> vars = new ArrayMap(
			event.toJson2()
				);
		vars.put("d", dataspace);
		vars.put("t", event.eventType); // type
		String res = fb.getPage(logEndpoint, vars);
		return res;
	}

	@Override
	public void saveEvents(Collection<DataLogEvent> events, Period period) {
		// TODO use a batch-save for speed
		for (DataLogEvent e : events) {
			saveEvent(e.dataspace, e, period);
		}
	}

	@Override
	public void registerEventType(String dataspace, String eventType) {
		// no-op??
	}
}
