package com.winterwell.datalog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.ajax.JSON;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.es.ESPath;
import com.winterwell.es.ESType;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.ESHttpResponse;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.IndexRequestBuilder;
import com.winterwell.es.client.PainlessScriptBuilder;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.UpdateRequestBuilder;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.CreateIndexRequest.Analyzer;
import com.winterwell.es.client.admin.PutMappingRequestBuilder;
import com.winterwell.es.client.agg.Aggregation;
import com.winterwell.es.client.agg.Aggregations;
import com.winterwell.es.client.query.ESQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.gson.Gson;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.Dep;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Null;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.WeirdException;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.app.AppUtils;

/**
 * ElasticSearch backed storage for DataLog
 * 
 * @testedby {@link ESStorageTest}
 * @author daniel
 *
 */
public class ESStorage implements IDataLogStorage {

	private static final String LOGTAG = "DataLog.ES";
	private ESConfig esConfig;
	private DataLogConfig config;
	
	@Override
	public void save(Period period, Map<String, Double> tag2count, Map<String, IDistribution1D> tag2mean) {
		Collection<DataLogEvent> events = new ArrayList();
		for(Entry<String, Double> tc : tag2count.entrySet()) {
			DataLogEvent event = event4tag(tc.getKey(), tc.getValue());
			events.add(event);
		}
		for(Entry<String, IDistribution1D> tm : tag2mean.entrySet()) {
			DataLogEvent event = event4distro(tm.getKey(), tm.getValue());
			events.add(event);
		}
		saveEvents(events, period);
	}

	/**
	 * make an event to store a distribution stat
	 * @param tm
	 * @return
	 */
	DataLogEvent event4distro(String stag, IDistribution1D distro) {
		DataLogEvent event = event4tag(stag, distro.getMean());
		// stash the whole thing, pref using json (but encoded as a string, so that ES won't go wild on index fields)
		if (Dep.has(Gson.class)) {
			Gson gson = Dep.get(Gson.class);
			Object json = gson.toJson(distro);
			ArrayMap xtra = new ArrayMap("gson", json);				
			event.setExtraResults(xtra);
		} else {
			ArrayMap xtra = new ArrayMap("xml", XStreamUtils.serialiseToXml(distro));				
			event.setExtraResults(xtra);
		}
		return event;
	}

	@Override
	public void saveHistory(Map<Pair2<String, Time>, Double> tag2time2count) {		
		for(Entry<Pair2<String, Time>, Double> tc : tag2time2count.entrySet()) {
			DataLogEvent event = event4tag(tc.getKey().first, tc.getValue());
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
		DataLogEvent spec = eventspec4tag(tag);
		SearchResponse sr = getData2(spec, start, end, true);
		List<Map<String, Object>> hits = sr.getSearchResults();
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
		return event4tag(tag, 0);
	}
	
	@Deprecated // use new DataLogEVent directly
	static DataLogEvent event4tag(String tag, double count) {
		return new DataLogEvent(tag, count);
	}

	@Override
	public Iterator getReader(String server, Time start, Time end, Pattern tagMatcher, String tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFuture<MeanRate> getMean(Time start, Time end, String tag) {
		// TODO aggregate down in ES?
		StatReq<IDataStream> mdata = getMeanData(tag, start, end, KInterpolate.SKIP_ZEROS, null);
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public StatReq<IDataStream> getMeanData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		DataLogEvent spec = eventspec4tag(tag);
		SearchResponse sr = getData2(spec, start, end, true);
		List<Map<String, Object>> hits = sr.getSearchResults();
		ListDataStream list = new ListDataStream(1);
		for (Map hit : hits) {
			Object t = hit.get("time");
			Object xtra = hit.get("xtra");
			Time time = Time.of(t.toString());
			Number count = (Number) hit.get("count");
			Datum d = new Datum(time, count.doubleValue(), tag);
			list.add(d);
		}
		// TODO interpolate and buckets
		return new StatReqFixed<IDataStream>(list);
	}

	@Override
	public void setHistory(Map<Pair2<String, Time>, Double> tagTime2set) {
		// TODO Auto-generated method stub
	}

	public IDataLogStorage init(DataLogConfig config) {
		this.config = config;
		// ES config
		if (esConfig == null) {
			ConfigFactory cf = ConfigFactory.get();
			ConfigBuilder cb = cf.getConfigBuilder(ESConfig.class);
			// also look in config/datalog.properties (as well as es.properties)
			esConfig = cb.set(new File("config/datalog.properties")).get();
			// check the connection
			ESHttpClient es = new ESHttpClient(esConfig);
			es.checkConnection();
		}
		// Support per-namespace ESConfigs
		if (config.namespaceConfigs!=null) {
			synchronized (config4dataspace) {
				for (String n : config.namespaceConfigs) {
					// also look in config/datalog.namespace.properties
					File f = new File("config/datalog."+n.toLowerCase()+".properties");
					Log.d(LOGTAG, "Looking for special namespace "+n+" config in "+f+" file-exists: "+f.exists());
					if ( ! f.exists()) {
						Log.w(LOGTAG, "No special config file "+f.getAbsoluteFile());
						continue;
					}
					ConfigFactory cf = ConfigFactory.get();
					ConfigBuilder cb = cf.getConfigBuilder(ESConfig.class);
					ESConfig esConfig4n = cb
							.set(new File("config/datalog.properties"))
							.set(f)
							.get();
					config4dataspace.put(n, esConfig4n);					
				}
			}
		}
		// init
		ArraySet<String> dataspaces = new ArraySet();
		if (config.namespace!=null) dataspaces.add(config.namespace);
		if (config.namespaceConfigs!=null) {
			dataspaces.addAll(config.namespaceConfigs);
		}
		for (String d : dataspaces) {
			String idx = indexFromDataspace(d);
			initIndex(d, idx);			
		}
		// share via Dep
		Dep.setIfAbsent(ESStorage.class, this);
		return this;
	}

	
	final Set<String> knownIndexes = new HashSet();
	
	private void initIndex(String dataspace, String index) {
		if (knownIndexes.contains(index)) return;
		ESHttpClient _client = client(dataspace);
		if (_client.admin().indices().indexExists(index)) {
			knownIndexes.add(index);
			assert knownIndexes.size() < 100000;
			return;
		}
		// make it, with a base and an alias
		initIndex2(index, _client);
	}

	private synchronized void initIndex2(String index, ESHttpClient _client) {
		// rcae condition - check it hasn't been made
		if (_client.admin().indices().indexExists(index)) {
			knownIndexes.add(index);
			return;
		}
		try {			
			// HACK
			String v = _client.getConfig().getIndexAliasVersion();
			String baseIndex = index+"_"+v;
			CreateIndexRequest pc = _client.admin().indices().prepareCreate(baseIndex);
			pc.setFailIfAliasExists(true); // this is synchronized, but what about other servers?
			pc.setDefaultAnalyzer(Analyzer.keyword);
			pc.setAlias(index);
			IESResponse cres = pc.get();
			cres.check();
		
			// register some standard event types??
			initIndex3_registerEventType(_client, index, DataLogEvent.simple);
		} catch(Throwable ex) {
			Log.e(LOGTAG, ex);
			// swallow and carry on -- an out of date schema may not be a serious issue
		}
	}

	private void initIndex3_registerEventType(ESHttpClient _client, String index, String type) 
	{
		String esType = typeFromEventType(type);
//		String v = _client.getConfig().getIndexAliasVersion();
		PutMappingRequestBuilder pm = _client.admin().indices().preparePutMapping(index, esType);
		// See DataLogEvent.COMMON_PROPS and toJson()
		ESType keywordy = new ESType().keyword().norms(false).lock();
		// Huh? Why were we using type text with keyword analyzer??
//				.text().analyzer("keyword")					
//				.fielddata(true);
		ESType props = new ESType()
				.property("k", keywordy)
				.property("v", new ESType().text().norms(false))
				.property("n", new ESType().DOUBLE());
		ESType simpleEvent = new ESType()
				.property(DataLogEvent.EVENTTYPE, keywordy.copy()) // ?? should we set fielddata=true??
				.property("time", new ESType().date())
				.property("count", new ESType().DOUBLE())
				.property("props", props);		
		// common probs...
		for(Entry<String, Class> cp : DataLogEvent.COMMON_PROPS.entrySet()) {
			// HACK to turn Class into ESType
			ESType est = keywordy.copy();
			if (cp.getValue()==StringBuilder.class) {
				est = new ESType().text().norms(false);
			} else if (cp.getValue()==Time.class) {
				est = new ESType().date();
			} else if (cp.getValue()==Double.class) {
				est = new ESType().DOUBLE();
			} else if (cp.getValue()==Integer.class) {
				est = new ESType().INTEGER();
			} else if (cp.getValue()==Long.class) {
				est = new ESType().LONG();					 
			} else if (cp.getValue()==Object.class) {
				if ("geo".equals(cp.getKey())) {
					est = new ESType().geo_point();
				}
			} else if (cp.getValue()==Null.class) {
				est = new ESType().object().noIndex();
			}
			simpleEvent.property(cp.getKey(), est);
		}
				
		pm.setMapping(simpleEvent);
		IESResponse res = pm.get();
		res.check();
	}

	public static String indexFromDataspace(String dataspace) {
		assert ! Utils.isBlank(dataspace);
		assert ! dataspace.startsWith("datalog.") : dataspace;
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
	 * @param bucketPeriod 
	 * @return 
	 */
	@Override
	public Future<ESHttpResponse> saveEvent(String dataspace, DataLogEvent event, Period bucketPeriod) {
		if (event.dataspace!=null && ! event.dataspace.equals(dataspace)) {
			Log.e(LOGTAG, new WeirdException("(swallowing) Dataspace mismatch: "+dataspace+" vs "+event.dataspace+" in "+event));
		}
//		Log.d("datalog.es", "saveEvent: "+event);		
		String index = indexFromDataspace(dataspace);
		// init?
		initIndex(dataspace, index);
		String type = typeFromEventType(event.getEventType()[0]);
		
		// ID
		String id;
		boolean grpById = event.groupById!=null; 
		if (grpById) {
			// HACK group by means no time bucketing
			id = event.getId();
		} else {
			// put a time marker on it -- the end in seconds is enough
			long secs = bucketPeriod.getEnd().getTime() % 1000;
			id = event.getId()+"_"+secs;
		}
		
		// always have a time
		if (event.time==null) {
			event.time = bucketPeriod.getEnd();
		}
		
		ESHttpClient client = client(dataspace);
		
		client.debug = true;
		
		// save -- update for grouped events, index otherwise
		ESPath path = new ESPath(index, type, id);
		Future<ESHttpResponse> f;
		if (grpById) {
			UpdateRequestBuilder saveReq = client.prepareUpdate(path);			
			// set doc
			Map<String, Object> doc = event.toJson2();
			PainlessScriptBuilder psb = PainlessScriptBuilder.fromJsonObject(doc);
			saveReq.setScript(psb);
			// upsert		
			saveReq.setUpsert(doc);
			f = saveReq.execute();
		} else {
			IndexRequestBuilder saveReq = client.prepareIndex(path);
			if (event.time==null) event.time = bucketPeriod.getEnd();
			// set doc
			Map<String, Object> doc = event.toJson2();			
			saveReq.setBodyMap(doc);
			f = saveReq.execute();
		}		
		client.close();
		
		// log stuff ??does this create a resource leak??
		if (f instanceof ListenableFuture) {
			((ListenableFuture<ESHttpResponse>) f).addListener(() -> {			
				try {
					ESHttpResponse response = f.get();
					response.check();
	//				Log.d("datalog.es", "...saveEvent done :) event: "+event);
				} catch(Throwable ex) {
					Log.e(DataLog.LOGTAG, "...saveEvent FAIL :( "+ex+" from event: "+event);
				}
			}, MoreExecutors.directExecutor());
		}
		
		return f;
	}

	/**
	 * 
	 * @param eventType Could come from the wild, so lets not use it directly.
	 * Lets insist on latin chars and protect the base namespace.
	 * @return
	 */
	public String typeFromEventType(String eventType) {
		return "evt"; // all under one type! 'cos (a) we can collate several events into one session event, and (b) ES is deprecating types anyway.
//		assert ! eventType.startsWith("evt.");
//		return "evt."+StrUtils.toCanonical(eventType);
	}

	@Override
	public void registerEventType(String dataspace, String eventType) {
		String index = indexFromDataspace(dataspace);
		initIndex(dataspace, index);
		
		ESHttpClient _client = client(dataspace);
		initIndex3_registerEventType(_client, index, eventType);
	}
	
	public double getEventTotal(Time start, Time end, DataLogEvent spec) {
		SearchResponse sr = getData2(spec, start, end, false);
		Map<String, Object> jobj = sr.getParsedJson();
		List<Map> hits = sr.getHits();
		Map aggs = sr.getAggregations();
		Map stats = (Map) aggs.get("event_total");
		Object sum = stats.get("sum");
		double total = MathUtils.toNum(sum);
		// Add in the last bucket
		// If you request data up to the present moment, then the last data-point is not in the database -- so we add it from memory.
		DataLogImpl impl = (DataLogImpl) DataLog.getImplementation();
		String tag = DataLogImpl.event2tag(spec.dataspace, spec.toJson2());
		Datum latest = impl.currentBucket(tag, end);
		// TODO
//		List<Datum> curHistData = stat.currentHistoric(tag, start, end);
		if (latest!=null) total += latest.x();
		return total;
	}
	
	SearchResponse getData2(DataLogEvent spec, Time start, Time end, boolean sortByTime) {
		
		if end
		DataLogImpl dl = (DataLogImpl) DataLog.getImplementation();
		Period bucketPeriod = dl.getBucket(event.time);
		end in 
		end at end of bucket
		
		DataLogConfig config = Dep.get(DataLogConfig.class);		
		String index = indexFromDataspace(spec.dataspace);
		SearchRequestBuilder search = client(spec.dataspace).prepareSearch(index);
		search.setType(typeFromEventType(spec.getEventType()[0]));
		search.setSize(config.maxDataPoints);
		
		com.winterwell.es.client.query.BoolQueryBuilder filter = ESQueryBuilders.boolQuery();
//		BoolQueryBuilder filter = QueryBuilders.boolQuery();
				
		// time box?
		if (start !=null || end != null) {
			ESQueryBuilder timeFilter = ESQueryBuilders.dateRangeQuery("time", start, end);			
			filter = filter.must(timeFilter);
		}
		
		// HACK tag match
		String tag = (String) spec.props.get("tag");
		if (tag!=null) {
			ESQueryBuilder tagFilter = ESQueryBuilders.termQuery("tag", tag);
			filter = filter.must(tagFilter);
		}		
		
		search.setQuery(filter);
		if (sortByTime) {
			search.addSort("time", SortOrder.ASC);
		}

		// stats or just sum??
		if (sortByTime) {
			
		} else {
			search.addAggregation(Aggregations.stats("event_total", "count"));
			search.setSize(0);
		}
//		ListenableFuture<ESHttpResponse> sf = search.execute(); TODO return a future
//		client.debug = true;
		SearchResponse sr = search.get();
//		client.debug = false;
		return sr;
	}

	static Map<String, ESConfig> config4dataspace = new HashMap();
	
	public ESHttpClient client(String dataspace) {
		assert ! dataspace.startsWith("datalog.") : dataspace;
		ESConfig _config = Utils.or(config4dataspace.get(dataspace), esConfig);
		assert _config != null : dataspace+" "+esConfig;
		return new ESHttpClient(_config);
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
		initIndex(dataspace, index);
	}

	public SearchResponse doSearchEvents(String dataspace, 
			int numResults, int numExamples, 
			Time start, Time end, 
			SearchQuery query, List<String> breakdown) 
	{
		BoolQueryBuilder filter = AppUtils.makeESFilterFromSearchQuery(query, start, end);
		
		String index = "datalog."+dataspace;
		ESHttpClient esc = client(dataspace);
		
		SearchRequestBuilder search = esc.prepareSearch(index);
//		search.setType(typeFromEventType(spec.eventType)); all types unless fixed
		// size controls
		search.setSize(numExamples);
		
		
		// search parameters				
		Set<String> allOutputs = new ArraySet<>();
		for(final String bd : breakdown) {
			if (bd==null) {
				Log.w("DataLog.ES", "null breakdown?! in "+breakdown);
			}
			// tag & time
			// e.g. tag/time {count:avg}
			// TODO proper recursive handling
			String[] breakdown_output = bd.split("\\{");
			String[] b = breakdown_output[0].trim().split("/");
			com.winterwell.es.client.agg.Aggregation byTag = Aggregations.terms(
					"by_"+StrUtils.join(b,'_'), b[0]);
			byTag.setSize(numResults);
			byTag.setMissing(ESQueryBuilders.UNSET);
			Aggregation leaf = byTag;
			if (b.length > 1) {
				if (b[1].equals("time")) {
					com.winterwell.es.client.agg.Aggregation byTime = Aggregations.dateHistogram("by_time", "time");
					byTime.put("interval", "hour");			
					byTag.subAggregation(byTime);
					leaf = byTime;
				} else {
					com.winterwell.es.client.agg.Aggregation byHost = Aggregations.terms("by_"+b[1], b[1]);
					byHost.setSize(numResults);
					byHost.setMissing(ESQueryBuilders.UNSET);
					byTag.subAggregation(byHost);
					leaf = byHost;
				}
			}				
			// add a count handler?
			if (breakdown_output.length <= 1) { // no - done
				search.addAggregation(byTag);
				continue;
			}
			String json = bd.substring(bd.indexOf("{"), bd.length());
			Map<String,String> output = (Map) JSON.parse(json);
			for(String k : output.keySet()) {
				allOutputs.add(k);
				com.winterwell.es.client.agg.Aggregation myCount = Aggregations.stats(k, k);
				leaf.subAggregation(myCount);
				// filter 0s ??does this work??
				filter.must(QueryBuilders.rangeQuery(k).gt(0));
			}						
			search.addAggregation(byTag);						
		} // ./breakdown
		
		// TODO add a total count as well
		for(String k : allOutputs) {
			com.winterwell.es.client.agg.Aggregation myCount = Aggregations.stats(k, k);
			search.addAggregation(myCount);	
		}
		
		// Set filter
		search.setFilter(filter);
		
		// TODO unify the ES search above with the DataLog interface call below
		// i.e. define a Java interface to match the above 
//		String[] tagBits = null;
//		DataLog.getData(start, end, null, TUnit.HOUR.dt, tagBits);
//		DataLog.getData(start, end, sum/as-is, bucket, DataLogEvent filter, breakdown);
		
//		esc.debug = true;
		SearchResponse sr = search.get();
		return sr;
	}
	
}
