package com.winterwell.datalog.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.winterwell.datalog.DataLogEvent;
import com.winterwell.datalog.ESStorage;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.agg.Aggregation;
import com.winterwell.es.client.agg.AggregationResults;
import com.winterwell.es.client.agg.Aggregations;
import com.winterwell.utils.Dep;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.ListField;
import com.winterwell.web.fields.SField;

public class DataServlet implements IServlet {

	private static final SField DATASPACE = new SField("dataspace");

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
		String dataspace = state.get(DATASPACE, "gl");
		List<String> breakdown = state.get(new ListField<String>("breakdown"));
		String index = "datalog."+dataspace;
		ESHttpClient esc = ess.client(dataspace);
		
		SearchRequestBuilder search = esc.prepareSearch(index);
//		search.setType(typeFromEventType(spec.eventType)); all types unless fixed
		search.setSize(0); // just the stats
		
		Time start = new Time().minus(TUnit.MONTH);
		Time end = new Time();
		RangeQueryBuilder timeFilter = QueryBuilders.rangeQuery("time")
				.from(start.toISOString(), true)
				.to(end.toISOString(), true);
		
		BoolQueryBuilder filter = QueryBuilders.boolQuery()		
				.must(timeFilter);
		
//		// HACK tag match
//		String tag = (String) spec.props.get("tag");
//		if (tag!=null) {
//			QueryBuilder tagFilter = QueryBuilders.termQuery("tag", tag);
//			filter = filter.must(tagFilter);
//		}		
		
		search.setFilter(filter);
		
		// tag & time
		com.winterwell.es.client.agg.Aggregation byTag = Aggregations.terms("byTag", "tag");
		com.winterwell.es.client.agg.Aggregation myCount = Aggregations.stats("myCount", "count");
		com.winterwell.es.client.agg.Aggregation byTime = Aggregations.dateHistogram("byTime", "time");
		byTime.put("interval", "hour");
		byTime.subAggregation(myCount);
		byTag.subAggregation(byTime);
		search.addAggregation(byTag);
		
//		// events over time
//		com.winterwell.es.client.agg.Aggregation byEvent = new Aggregation("byEvent", "terms", DataLogEvent.EVENTTYPE);
//		com.winterwell.es.client.agg.Aggregation dh = Aggregations.dateHistogram("byTime", "time");
//		dh.put("interval", "hour");
//		byEvent.subAggregation(dh);						
//		search.addAggregation(byEvent);
//		
//		// sorta bug: the breakdowns below sum
//		// TODO use a 2nd search, so we can filter by evt.type = visible
//		// events by publisher
//		com.winterwell.es.client.agg.Aggregation byDomain = Aggregations.terms("byDomain", "domain");
//		search.addAggregation(byDomain);
//		com.winterwell.es.client.agg.Aggregation byHost = Aggregations.terms("byHost", "host");
//		search.addAggregation(byHost);
//		// TODO
////		com.winterwell.es.client.agg.Aggregation byAdvert = Aggregations.terms("byCampaign", "campaign");
////		search.addAggregation(byAdvert);
//		// TODO by variant
////		com.winterwell.es.client.agg.Aggregation byVariant = Aggregations.terms("byVariant", "host");
////		search.addAggregation(byVariant);
//		// TODO MPU vs leaderboard
////		com.winterwell.es.client.agg.Aggregation byFormat = Aggregations.terms("byVariant", "host");
////		search.addAggregation(byFormat);
//				
////		search.setSearchType("count"); // aggregations c.f. https://www.elastic.co/blog/intro-to-aggregations
		
//		ListenableFuture<ESHttpResponse> sf = search.execute(); TODO return a future
//		client.debug = true;
		SearchResponse sr = search.get();
		
		Map aggregations = sr.getAggregations();
		JsonResponse jr = new JsonResponse(state, aggregations);
		WebUtils2.sendJson(jr, state);
	}

}
