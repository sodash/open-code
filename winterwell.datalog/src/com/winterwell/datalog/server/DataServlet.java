package com.winterwell.datalog.server;

import java.io.IOException;
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
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;

public class DataServlet implements IServlet {

	@Override
	public void process(WebRequest state) throws IOException {		
		ESStorage ess = Dep.get(ESStorage.class);
		String dataspace = "gl";
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
		
		// events over time
		com.winterwell.es.client.agg.Aggregation byEvent = new Aggregation("byEvent", "terms", DataLogEvent.EVENTTYPE);
		com.winterwell.es.client.agg.Aggregation dh = Aggregations.dateHistogram("events_over_time", "time");
		dh.put("interval", "hour");
		byEvent.subAggregation(dh);						
		search.addAggregation(byEvent);
		
		// sorta bug: the breakdowns below sum
		// TODO use a 2nd search, so we can filter by evt.type = visible
		// events by publisher
		com.winterwell.es.client.agg.Aggregation byDomain = Aggregations.terms("byDomain", "domain");
		search.addAggregation(byDomain);
		com.winterwell.es.client.agg.Aggregation byHost = Aggregations.terms("byHost", "host");
		search.addAggregation(byHost);
		// TODO
//		com.winterwell.es.client.agg.Aggregation byAdvert = Aggregations.terms("byCampaign", "campaign");
//		search.addAggregation(byAdvert);
		// TODO by variant
//		com.winterwell.es.client.agg.Aggregation byVariant = Aggregations.terms("byVariant", "host");
//		search.addAggregation(byVariant);
		// TODO MPU vs leaderboard
//		com.winterwell.es.client.agg.Aggregation byFormat = Aggregations.terms("byVariant", "host");
//		search.addAggregation(byFormat);
		
		search.setSize(0); // is this wanted??
//		search.setSearchType("count"); // aggregations c.f. https://www.elastic.co/blog/intro-to-aggregations
		
//		ListenableFuture<ESHttpResponse> sf = search.execute(); TODO return a future
//		client.debug = true;
		SearchResponse sr = search.get();
		AggregationResults aggr = sr.getAggregationResults(byEvent.name);
		System.out.println(aggr);
		
		JsonResponse jr = new JsonResponse(state, sr.getAggregations());
		WebUtils2.sendJson(jr, state);
	}

}
