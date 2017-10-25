package com.winterwell.datalog.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import com.winterwell.datalog.DataLogSecurity;
import com.winterwell.datalog.ESStorage;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.agg.Aggregation;
import com.winterwell.es.client.agg.Aggregations;
import com.winterwell.utils.Dep;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ICallable;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.IntField;
import com.winterwell.web.fields.ListField;
import com.winterwell.web.fields.SField;
import com.winterwell.web.fields.TimeField;

/**
 * Serves up aggregations data
 * @author daniel
 *
 */
public class DataServlet implements IServlet {

	static final SField DATASPACE = new SField("dataspace");
	private static final String LOGTAG = "DataServlet";

	@Override
	public void process(WebRequest state) throws IOException {						
		// TODO request memory use as a good graph to test
//		"_index": "datalog.default",
//        "_type": "evt.simple",
//        "_id": "default/simple_6814221e746bf98518810a931070ba6c_76",
//        "_score": 1.0,
//        "_source": {
//          "evt": "simple",
//          "time": "2017-06-30T14:00:00Z",
//          "count": 7571440.0,
//          "tag": "mem_used",
		
		ESStorage ess = Dep.get(ESStorage.class);
		String dataspace = state.get(DATASPACE, "default");				
		// Uses "paths" of breakdown1/breakdown2/... {field1:operation, field2}
		List<String> breakdown = state.get(
				new ListField<String>("breakdown").setSplitPattern(",")
				);
		if (breakdown==null) {
			Log.w(LOGTAG, "You want data but no breakdown?! "+state);
			breakdown = new ArrayList();
		}

		// security: on the dataspace, and optionally on the breakdown
		DataLogSecurity.check(state, dataspace, breakdown);

		String index = "datalog."+dataspace;
		ESHttpClient esc = ess.client(dataspace);
		
		SearchRequestBuilder search = esc.prepareSearch(index);
//		search.setType(typeFromEventType(spec.eventType)); all types unless fixed
		// size controls
		// num results
		int numTerms = state.get(new IntField("termsSize"), 1000);		
		// num examples
		Integer size = state.get(new IntField("size"), 10);
		search.setSize(size);
		
		
		// search parameters
		// time box
		ICallable<Time> cstart = state.get(new TimeField("start"));
		Time start = cstart==null? new Time().minus(TUnit.MONTH) : cstart.call();
		ICallable<Time> cend = state.get(new TimeField("end").setPreferEnd(true));
		Time end = cend==null? new Time() : cend.call();
		// query
		String q = state.get("q");
		if (q==null) q = "";
		SearchQuery sq = new SearchQuery(q);
		
		RangeQueryBuilder timeFilter = QueryBuilders.rangeQuery("time")
				.from(start.toISOString()) //, true) ES versioning pain
				.to(end.toISOString()); //, true);
		
		BoolQueryBuilder filter = QueryBuilders.boolQuery()		
				.must(timeFilter);		
		
		// filters
		for(String prop : "evt host campaign".split(" ")) {
			String host = sq.getProp(prop);
			if (host!=null) {
				QueryBuilder kvFilter = QueryBuilders.termQuery(prop, host);
				filter = filter.must(kvFilter);
			}
		}
				
		for(final String bd : breakdown) {
			if (bd==null) {
				Log.w(LOGTAG, "null breakdown?! in "+breakdown+" from "+state);
			}
			// tag & time
			// e.g. tag/time {count:avg}
			// TODO proper recursive handling
			String[] breakdown_output = bd.split("\\{");
			String[] b = breakdown_output[0].trim().split("/");
			com.winterwell.es.client.agg.Aggregation byTag = Aggregations.terms(
					"by_"+StrUtils.join(b,'_'), b[0]);
			byTag.setSize(numTerms);
			Aggregation leaf = byTag;
			if (b.length > 1) {
				if (b[1].equals("time")) {
					com.winterwell.es.client.agg.Aggregation byTime = Aggregations.dateHistogram("by_time", "time");
					byTime.put("interval", "hour");			
					byTag.subAggregation(byTime);
					leaf = byTime;
				} else {
					com.winterwell.es.client.agg.Aggregation byHost = Aggregations.terms("by_"+b[1], b[1]);
					byHost.setSize(numTerms);
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
				com.winterwell.es.client.agg.Aggregation myCount = Aggregations.stats(k, k);			
				leaf.subAggregation(myCount);
				// filter 0s ??does this work??
				filter.must(QueryBuilders.rangeQuery(k).gt(0));
			}						
			search.addAggregation(byTag);
		} // ./breakdown
		
		// Set filter
		search.setFilter(filter);
		
		// TODO unify the ES search above with the DataLog interface call below
		// i.e. define a Java interface to match the above 
//		String[] tagBits = null;
//		DataLog.getData(start, end, null, TUnit.HOUR.dt, tagBits);
//		DataLog.getData(start, end, sum/as-is, bucket, DataLogEvent filter, breakdown);
		
		esc.debug = true;
		SearchResponse sr = search.get();
		sr.check();
		
		Map aggregations = sr.getAggregations();
		if (aggregations==null) {
			Log.d(LOGTAG, "No aggregations?! "+state+" "+sr);
			aggregations = new ArrayMap();
		}
		// also send eg data
		aggregations.put("examples", sr.getHits());
		JsonResponse jr = new JsonResponse(state, aggregations);
		WebUtils2.sendJson(jr, state);
	}

}
