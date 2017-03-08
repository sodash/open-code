package com.winterwell.datalog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
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
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Dep;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;

public class ESStorage implements IDataLogStorage {

	private ESConfig settings;
	private ESHttpClient client;
	
	@Override
	public void save(Period period, Map<String, Double> tag2count, Map<String, MeanVar1D> tag2mean) {
		Collection<DataLogEvent> events = new ArrayList();
		String ds = DataLog.getDataspace();
		for(Entry<String, Double> tc : tag2count.entrySet()) {
			DataLogEvent event = new DataLogEvent(ds, tc.getValue(), "simple", new ArrayMap("tag", tc.getKey()));
			events.add(event);
		}
		for(Entry<String, MeanVar1D> tm : tag2mean.entrySet()) {
			DataLogEvent event = new DataLogEvent(ds, tm.getValue().getMean(), "simple", new ArrayMap("tag", tm.getKey()));
			event.setExtraResults(new ArrayMap(
					tm.getValue().toJson2()
					));
			events.add(event);
		}
		saveEvents(events, period);
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
		DataLogEvent spec = eventspec4tag(tag);
		SearchResponse sr = getData2(spec, start, end, true);
		List<Map> hits = sr.getSearchResults();
		ListDataStream list = new ListDataStream(1);
		for (Map hit : hits) {
			Object t = hit.get("time");
			Time time = Time.of(t.toString());
			Number count = (Number) hit.get("count");
			Datum d = new Datum(time, count.doubleValue(), tag);
			list.add(d);
		}
		// TODO interpolate and buckets
		return new StatReqFixed<IDataStream>(list);
	}

	@Override
	public StatReq<Double> getTotal(String tag, Time start, Time end) {
		DataLogEvent spec = eventspec4tag(tag);
		double total = getEventTotal(start, end, spec);
		return new StatReqFixed<Double>(total);
	}

	private DataLogEvent eventspec4tag(String tag) {
		String ds = DataLog.getDataspace(); 
		DataLogEvent spec = new DataLogEvent(ds, 0, "simple", new ArrayMap("tag", tag));
		return spec;
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

	public IDataLogStorage init(StatConfig config) {
		if (settings == null) {
			settings = new ESConfig();			
		}
		client = new ESHttpClient(settings);
		String idx = indexFromDataspace(DataLog.getDataspace());
		initIndex(idx);
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
		// register some standard event types??
	}

	private String indexFromDataspace(String dataspace) {
		assert ! Utils.isBlank(dataspace);
		String idx = "datalog."+dataspace;
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
		Log.d("datalog.es", "saveEvent: "+event);
		String index = indexFromDataspace(dataspace);
		String type = typeFromEventType(event.eventType);
		// put a time marker on it -- the end in seconds is enough
		long secs = period.getEnd().getTime() % 1000;
		String id = event.getId()+"_"+secs;
		IndexRequestBuilder prepIndex = client.prepareIndex(index, type, id);
		if (event.time==null) event.time = period.getEnd();
		// set doc
		prepIndex.setBodyMap(event.toJson2());
		ListenableFuture<ESHttpResponse> f = prepIndex.execute();
		// log stuff
		f.addListener(() -> {			
			try {
				ESHttpResponse response = f.get();
				response.check();
				Log.d("datalog.es", "...saveEvent done :) event: "+event);
			} catch(Throwable ex) {
				Log.d("datalog.es", "...saveEvent FAIL :( "+ex+" from event: "+event);
			}
		}, MoreExecutors.directExecutor());
		return f;
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

	@Override
	public void registerEventType(String dataspace, String eventType) {
		String index = indexFromDataspace(dataspace);
		String type = typeFromEventType(eventType);
		PutMappingRequestBuilder putMapping = client.admin().indices().preparePutMapping(index, type);
		// Set the time property as time. The rest it can auto-figure
		Map msrc = new ESType()
						.property("time", new ESType().date());
		putMapping.setBodyMap(msrc);
		IESResponse res = putMapping.get();
		res.check();
		Map<String, Object> jout = res.getParsedJson();
	}
	
	public double getEventTotal(Time start, Time end, DataLogEvent spec) {
		SearchResponse sr = getData2(spec, start, end, false);
		Map<String, Object> jobj = sr.getParsedJson();
		List<Map> hits = sr.getHits();
		Map aggs = sr.getAggregations();
		Map stats = (Map) aggs.get("event_total");
		Object sum = stats.get("sum");
		return MathUtils.toNum(sum);
	}
	
	SearchResponse getData2(DataLogEvent spec, Time start, Time end, boolean sortByTime) {
		StatConfig config = Dep.get(StatConfig.class);		
		String index = indexFromDataspace(spec.dataspace);
		SearchRequestBuilder search = client.prepareSearch(index);
		search.setType(typeFromEventType(spec.eventType));
		search.setSize(config.maxDataPoints);
		RangeQueryBuilder timeFilter = QueryBuilders.rangeQuery("time").from(start.toISOString(), true).to(end.toISOString(), true);
		
		BoolQueryBuilder filter = QueryBuilders.boolQuery()		
				.must(timeFilter);
		
		// HACK tag match
		String tag = (String) spec.props.get("tag");
		if (tag!=null) {
			QueryBuilder tagFilter = QueryBuilders.termQuery("tag", tag);
			filter = filter.must(tagFilter);
		}		
		
		search.setFilter(filter);
		if (sortByTime) {
			search.addSort("time", SortOrder.DESC);
		}

		// stats or just sum??
		search.addAggregation("event_total", "stats", "count");
		search.setSize(0);
//		ListenableFuture<ESHttpResponse> sf = search.execute(); TODO return a future
		SearchResponse sr = search.get();
		return sr;
	}

	@Override
	public void saveEvents(Collection<DataLogEvent> events, Period period) {
		// TODO use a batch-save for speed
		for (DataLogEvent e : events) {
			saveEvent(e.dataspace, e, period);
		}
	}

	public void registerDataspace(String dataspace) {
		String index = indexFromDataspace(dataspace);
		initIndex(index);
	}
	
}
