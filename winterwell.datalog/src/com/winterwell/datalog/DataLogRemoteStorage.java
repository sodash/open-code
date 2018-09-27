package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.datalog.server.DataLogFields;
import com.winterwell.datalog.server.DataServlet;
import com.winterwell.datalog.server.LgServlet;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.timeseries.IDataStream;
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
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.KServerType;

/**
 * This is a kind of DatalogClient API class
 * TODO Remote server storage for DataLog
 * So the adserver can log stuff into lg.
 * @author daniel
 * @testedby {@link DataLogRemoteStorageTest}
 */
public class DataLogRemoteStorage implements IDataLogStorage
{

	/**
	 * @deprecated This is inefficient
	 * HACK a direct call to the remote server
	 * @param server e.g. lg.good-loop.com NB: https or /lg are optional but can be provided
	 * @param event
	 * @return
	 */
	public static boolean saveToRemoteServer(String server, DataLogEvent event) {
		Utils.check4null(server, event);
		// TODO via Dep
		DataLogRemoteStorage dlrs = new DataLogRemoteStorage();
		DataLogConfig remote = new DataLogConfig();
		// add https and endpoint
		if ( ! server.startsWith("http")) {
			server = "https://"+server;
		}
		if ( ! server.endsWith("/lg")) server += "/lg";
		
		remote.logEndpoint = server;
		dlrs.init(remote);
		
		// DEBUG July 2018
		String eds = event.dataspace; 
		if (event.dataspace==null) {
			Log.e("datalog", "null dataspace?! "+event);
			eds = "gl"; // paranoia HACK
		}		
		
		Dataspace ds = new Dataspace(eds);
		// save
		Object ok = dlrs.saveEvent(ds, event, new Period(event.time));
		Log.d("datalog.remote", "Save to "+server+" "+event+" response: "+ok);
		return true;
	}
	
	
	/**
	 * // HACK log to lg (this should really be done by altering the DataLog settings)
	 * @param event
	 * @param state
	 */
	public static void hackRemoteDataLog(DataLogEvent event) {
		try {
			// TODO replace with use of DatalogConfig
			KServerType st = AppUtils.getServerType(null);
			StringBuilder su = AppUtils.getServerUrl(st, "lg.good-loop.com");			
			String LG_SERVER = su.toString();
			
			DataLogRemoteStorage.saveToRemoteServer(LG_SERVER, event);
		} catch(Throwable ex) {
			Log.e("datalog.hack (swallowed)", ex);
		}
	}

	
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


	@Deprecated // TODO! parse the output. Unify with DataLogHttpClient
	@Override
	public StatReq<IDataStream> getData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		FakeBrowser fb = fb();		
//		fb.setAuthentication("daniel@local.com", "1234");		// FIXME remove this into options!
		Map<String, String> vars = new ArrayMap(
			"q", "evt:"+tag,
			"breakdown", "time"
				);
		vars.put("d", DataLog.getDataspace());
		vars.put("t", DataLogEvent.simple); // type
		String res = fb.getPage(getDataEndpoint, vars);
		Object jobj = JSON.parse(res);
		throw new TodoException(jobj);
	}

	private FakeBrowser fb() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		fb.setUserAgent(FakeBrowser.HONEST_USER_AGENT);
		return fb;
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
	public Object saveEvent(Dataspace dataspace, DataLogEvent event, Period periodIsNotUsedHere) {
		// See LgServlet which reads these		
		FakeBrowser fb = fb();
		fb.setRetryOnError(5); // try a few times to get through. Can block for 2 seconds.
		Map<String, Object> vars = new ArrayMap();
		// core fields
		vars.put(DataLogFields.d.name, 	dataspace.name);		
		vars.put(LgServlet.GBY.name, 	event.groupById); // group it?
		vars.put(DataLogFields.t.name, 	event.getEventType0()); // type
		vars.put("count", event.count);
		vars.put("time", event.getTime());
		// props
		String p = JSON.toString(event.getProps());
		vars.put("p", p);		
		// TODO String r = referer		
		String res = fb.getPage(logEndpoint, (Map) vars);
		Log.d("datalog.remote", "called "+fb.getLocation()+" return: "+res);
		return res;
	}

	@Override
	public void saveEvents(Collection<DataLogEvent> events, Period period) {
		// TODO use a batch-save for speed
		for (DataLogEvent e : events) {
			saveEvent(new Dataspace(e.dataspace), e, period);
		}
	}

	@Override
	public void registerEventType(Dataspace dataspace, String eventType) {
		// no-op??
	}
}
