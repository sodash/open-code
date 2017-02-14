package com.winterwell.datalog;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.util.concurrent.ListenableFuture;
import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.datalog.server.DataLogSettings;
import com.winterwell.depot.Desc;
import com.winterwell.es.ESType;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.ESHttpResponse;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.IndexRequestBuilder;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.CreateIndexRequest.Analyzer;
import com.winterwell.es.client.admin.PutMappingRequestBuilder;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;

public class ESStorage implements IStatStorage {

	private ESConfig settings;
	private ESHttpClient client;
	
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

	public IStatStorage init(StatConfig config) {
		if (settings == null) {
			settings = new ESConfig();			
		}
		client = new ESHttpClient(settings);
		initIndex(indexFromDataspace("gen"));
		return this;
	}

	
	private void initIndex(String index) {
		if (client.admin().indices().indexExists(index)) {
			return;
		}
		// make it
		CreateIndexRequest pc = client.admin().indices().prepareCreate(index);
		pc.setDefaultAnalyzer(Analyzer.keyword);
		IESResponse res = pc.get();
		res.check();
//		// TODO map it
//		PutMappingRequestBuilder pm = client.admin().indices().preparePutMapping(index);
//		Map mapping = new ArrayMap();
//		pm.setSource(mapping);
	}

	private String indexFromDataspace(String dataspace) {
		assert ! Utils.isBlank(dataspace);
		String idx = "datalog."+dataspace;
		initIndex(idx);
		return idx;
	}

	public ESStorage() {
	}
	
	/**
	 * 
	 * 
	 * @param cnt
	 * @param dataspace
	 * @param event
	 * @param period 
	 * @return 
	 */
	@Override
	public ListenableFuture<ESHttpResponse> saveEvent(String dataspace, DataLogEvent event, Period period) {
		String index = indexFromDataspace(dataspace);
		String type = typeFromEventType(event.eventType);
		// put a time marker on it -- the end in seconds is enough
		long secs = period.getEnd().getTime() % 1000;
		String id = event.getId()+"_"+secs;
		IndexRequestBuilder prepIndex = client.prepareIndex(index, type, id);
		if (event.time==null) event.time = period.getEnd();
		// set doc
		prepIndex.setSource(event.toJson2());
		return prepIndex.execute();
	}

	/**
	 * 
	 * @param eventType Could come from the wild, so lets not use it directly.
	 * Lets insist on latin chars and protect the base namespace.
	 * @return
	 */
	private String typeFromEventType(String eventType) {
		return "evt."+StrUtils.toCanonical(eventType);
	}

	public double getEventTotal(String dataspace, Time start, Time end, DataLogEvent spec) {
		String index = indexFromDataspace(dataspace);
		SearchRequestBuilder search = client.prepareSearch(index);
		search.setType(typeFromEventType(spec.eventType));
		// stats or just sum??
		search.addAggregation("event_total", "stats", "count");
		search.setSize(0);
		SearchResponse sr = search.get();
		Map<String, Object> jobj = sr.getParsedJson();
		List<Map> hits = sr.getHits();
		Map aggs = sr.getAggregations();
		Map stats = (Map) aggs.get("event_total");
		Object sum = stats.get("sum");
		return MathUtils.toNum(sum);
	}

	@Override
	public void saveEvents(Collection<DataLogEvent> events, Period period) {
		// TODO use a batch-save for speed
		for (DataLogEvent e : events) {
			saveEvent(e.dataspace, e, period);
		}
	}
	
}
