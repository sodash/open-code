package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.agg.Aggregation;
import com.winterwell.es.client.agg.Aggregations;
import com.winterwell.es.client.query.BoolQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.web.app.AppUtils;

public class ESDataLogSearchBuilder {
	
	final Dataspace dataspace;
	int numResults;
	int numExamples; 
	Time start;
	Time end; 
	SearchQuery query;
	List<String> breakdown;
	private boolean doneFlag;
	private ESHttpClient esc;
	
	public ESDataLogSearchBuilder(ESHttpClient esc, Dataspace dataspace) {
		this.dataspace = dataspace;
		this.esc = esc;
	}
	
	public ESDataLogSearchBuilder setBreakdown(List<String> breakdown) {
		assert ! doneFlag;
		this.breakdown = breakdown;
		return this;
	}
	
	public ESDataLogSearchBuilder setQuery(SearchQuery query) {
		assert ! doneFlag;
		this.query = query;
		return this;
	}
	
	public SearchRequestBuilder prepareSearch() {
		doneFlag = true;
		com.winterwell.es.client.query.BoolQueryBuilder filter 
			= AppUtils.makeESFilterFromSearchQuery(query, start, end);
		
		String index = ESStorage.readIndexFromDataspace(dataspace);
		
		SearchRequestBuilder search = esc.prepareSearch(index);
	
		// breakdown(s)
		List<Aggregation> aggs = prepareSearch2_aggregations(filter);
		for (Aggregation aggregation : aggs) {
			search.addAggregation(aggregation);
		}
		
		// Set filter
		search.setQuery(filter);

		
		return search;
	}

	public ESDataLogSearchBuilder setNumResults(int numResults) {
		assert ! doneFlag;
		this.numResults = numResults;
		return this;
	}
	
	
	

	/**
	 * Add aggregations 
	 * @param numResults
	 * @param breakdown
	 * @param filter This may be modified to filter out 0s
	 * @param search
	 */
	List<Aggregation> prepareSearch2_aggregations(BoolQueryBuilder filter) 
	{
		List<Aggregation> aggs = new ArrayList();
		for(final String bd : breakdown) {
			if (bd==null) {
				Log.w("DataLog.ES", "null breakdown?! in "+breakdown);
				continue;
			}
			// TODO new Breakdown
			// tag & time
			// e.g. tag/time {count:avg}
			// TODO proper recursive handling
			String[] breakdown_output = bd.split("\\{");
			String[] b = breakdown_output[0].trim().split("/");			
			String field = b[0];
			com.winterwell.es.client.agg.Aggregation byTag = Aggregations.terms("by_"+field, field);
			if (numResults > 0) byTag.setSize(numResults);
			if ( ! "time".equals(b[0])) { // HACK avoid "unset" -> parse exception
				byTag.setMissing(ESQueryBuilders.UNSET);
			}
			// add a count handler?
			if (breakdown_output.length <= 1) { // no - done
				aggs.add(byTag);
				continue;
			}
			
			Aggregation leaf = byTag;
			if (b.length > 1) {
				if (b[1].equals("time")) {
					com.winterwell.es.client.agg.Aggregation byTime = Aggregations.dateHistogram("by_time", "time", interval);
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
			
			// e.g. {"count": "avg"}
			String json = bd.substring(bd.indexOf("{"), bd.length());
			Map<String,String> output = (Map) JSON.parse(json);
			for(String k : output.keySet()) {
				com.winterwell.es.client.agg.Aggregation myCount = Aggregations.stats(k, k);
				// filter 0s
				ESQueryBuilder no0 = ESQueryBuilders.rangeQuery(k, 0, null, false);
				Aggregation noZeroMyCount = Aggregations.filtered("no0_"+k, no0, myCount);
				leaf.subAggregation(noZeroMyCount);
			}		
			aggs.add(byTag);
		} // ./breakdown
		
		// add a total count as well for each top-level breakdown
		for(Aggregation agg : aggs.toArray(new Aggregation[0])) {
			String field = agg.getField();
			if (field != null) {
				Aggregation myCount = Aggregations.stats(field, field);
				aggs.add(myCount);
			}
		}
		
		return aggs;
	}

	public ESDataLogSearchBuilder setStart(Time start) {
		assert ! doneFlag;
		this.start = start;
		return this;
	}

	public ESDataLogSearchBuilder setEnd(Time end) {
		assert ! doneFlag;
		this.end = end;
		return this;
	}

	Dt interval = TUnit.DAY.dt;
	
	public void setInterval(Dt interval) {
		this.interval = interval;
	}
	
}
