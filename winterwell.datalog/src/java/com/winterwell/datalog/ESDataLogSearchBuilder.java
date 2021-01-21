package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.SearchRequest;
import com.winterwell.es.client.agg.Aggregation;
import com.winterwell.es.client.agg.Aggregations;
import com.winterwell.es.client.query.ESQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.web.WebEx;
import com.winterwell.web.app.AppUtils;

/**
 * @testedby  ESDataLogSearchBuilderTest}
 * @author daniel
 *
 */
public class ESDataLogSearchBuilder {
	
	private static final String no0_ = "no0_";
	/**
	 * maximum number of ops you can request in one breakdown.
	 * Normally just 1!
	 */
	private static final int MAX_OPS = 10;
	private static final String LOGTAG = "ESDataLogSearchBuilder";
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
	
	public SearchRequest prepareSearch() {
		doneFlag = true;
		com.winterwell.es.client.query.BoolQueryBuilder filter 
			= AppUtils.makeESFilterFromSearchQuery(query, start, end);
		
		String index = ESStorage.readIndexFromDataspace(dataspace);
		
		SearchRequest search = esc.prepareSearch(index);
	
		// breakdown(s)
		List<Aggregation> aggs = prepareSearch2_aggregations();
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
	List<Aggregation> prepareSearch2_aggregations() 
	{
		List<Aggregation> aggs = new ArrayList();
		for(final String bd : breakdown) {
			if (Utils.isBlank(bd)) {				
				continue;
			}
			Aggregation agg = prepareSearch3_agg4breakdown(bd);
			aggs.add(agg);
		} // ./breakdown
		
		// add a total count as well for each top-level terms breakdown
		Aggregation fCountStats = Aggregations.sum("all", ESStorage.count);
		aggs.add(fCountStats);			
		
		return aggs;
	}

	/**
	 * 
	 * @param bd Format: bucket-by-fields/ {"report-fields": "operation"} 
	 * 	e.g. "evt" or "evt/time" or "tag/time {"mycount":"avg"}"
	 * NB: the latter part is optional, but if present must be valid json.
	 *   
	 * @return
	 */
	private Aggregation prepareSearch3_agg4breakdown(String bd) {

		// ??Is there a use-case for recursive handling??
		String[] breakdown_output = bd.split("\\{");
		String[] bucketBy = breakdown_output[0].trim().split("/");
		Map<String,String> reportSpec = new ArrayMap("count","sum"); // default to sum of `count`
		if (breakdown_output.length > 1) {
			String json = bd.substring(bd.indexOf("{"), bd.length());
			try {
				reportSpec = (Map) JSON.parse(json);
			} catch(Exception pex) {
				throw new WebEx.BadParameterException("breakdown", "invalid json: "+json, pex);
			}
		}
		// loop over the f1/f2 part, building a chain of nested aggregations
		Aggregation root = null;
		Aggregation leaf = null;
		Aggregation previousLeaf = null;
		String s_bucketBy = StrUtils.join(bucketBy, '_');
		for(String field : bucketBy) {
			if (Utils.isBlank(field)) {
				// "" -- use-case: you get this with top-level "sum all"
				continue;
			}
			if (field.equals("time")) {
				leaf = Aggregations.dateHistogram("by_"+s_bucketBy, "time", interval);
			} else if (field.equals("dateRange")) {
				// A slightly hacky option. Use-case: return stats for the 
				// 	last week, the week before (to allow "+25%" comparisons), and older
				Time now = end;
				Time prev = now.minus(interval);
				Time prev2 = prev.minus(interval);
				List<Time> times = Arrays.asList(start, prev2, prev, now);
				leaf = Aggregations.dateRange("by_"+s_bucketBy, "time", times);
			} else {
				leaf = Aggregations.terms("by_"+s_bucketBy, field);
				if (numResults>0) leaf.setSize(numResults);
				// HACK avoid "unset" -> parse exception
				leaf.setMissing(ESQueryBuilders.UNSET);
			}
			if (root==null) {
				root = leaf;
			} else {
				previousLeaf.subAggregation(leaf);
			}
			previousLeaf = leaf;
			// chop down name for the next loop, if there is one.
			if (field.length() < s_bucketBy.length()) {
				s_bucketBy = s_bucketBy.substring(field.length()+1);
			}
		}
		
		// add a count handler? old - with compression, doc_count alone is meaningless
//		if (reportSpec==null) { // no - we're done - return terms
//			return root;
//		}
		
		// e.g. {"count": "avg"}
		String[] rkeys = reportSpec.keySet().toArray(StrUtils.ARRAY);
		for(int i=0; i<rkeys.length; i++) {
			String k = rkeys[i];
			// safety check: k is a field, not an op
			if (k.equals("sum") || k.equals("avg")) {
				throw new IllegalArgumentException("Bad breakdown {field:op} parameter: "+bd);
			}
			// Note k should be a numeric field, e.g. count -- not a keyword field!
			if ( ! ESStorage.count.equals(k)) {
				Class klass = DataLogEvent.COMMON_PROPS.get(k);
				if ( ! ReflectionUtils.isa(klass, Number.class)) {
					Log.w(LOGTAG, "Possible bug! numeric op on non-numeric field "+k+" in "+bd);
				}
			}
			Aggregation myCount = Aggregations.sum(k, k);
			// filter 0s??
			ESQueryBuilder no0 = ESQueryBuilders.rangeQuery(k, 0, null, false);
			// NB: we could have multiple ops, so number the keys
			Aggregation noZeroMyCount = Aggregations.filtered(no0_+i, no0, myCount);
			if (leaf != null) {
				leaf.subAggregation(noZeroMyCount);
				assert root != null;
				continue;
			}
			// this is a top-level sum
			assert root == null;
			leaf = noZeroMyCount;
			root = leaf;			
		}		
		return root;
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

	/**
	 * Remove the no0_ filtering wrappers 'cos they're an annoyance at the client level.
	 * Also remove doc_count for safety.
	 * Simplify count:value:5 to count:5
	 * @param aggregations 
	 * @return cleaned aggregations
	 */
	public Map cleanJson(Map<String,Object> aggregations) {

		Map aggs2 = Containers.applyToJsonObject(aggregations, ESDataLogSearchBuilder::cleanJson2);
		// also top-level
		Map aggs3 = (Map) cleanJson2(aggs2, null);
		return aggs3;
	}	
	
	static Object cleanJson2(Object old, List<String> __path) {
		if ( ! (old instanceof Map)) {
			return old;
		}
		Map mold = (Map) old;
		// no doc_count (its misleading with compression)
		mold.remove("doc_count");
		
		// simplify {value:5} to 5
		if (mold.size() == 1) {
			Object k = Containers.only(mold.keySet());
			if ("value".equals(k)) {
				Number v = (Number) mold.get(k);
				return v;
			}			
		}
		
		Map newMap = null;
		for(int i=0; i<MAX_OPS; i++) {
			Map<String,?> wrapped = (Map) mold.get(no0_+i);
			// no no0s to remove?
			if (wrapped==null) break;
			// copy and edit
			if (newMap==null) newMap = new ArrayMap(mold);
			newMap.remove(no0_+i);
			for(String k : wrapped.keySet()) {
				Object v = wrapped.get(k);
				if (v instanceof Map) {
					// its some aggregation results :)
					Object oldk = newMap.put(k, v);
					if (oldk!=null) {
						Log.e(LOGTAG, "duplicate aggregation results for "+k+"?! "+newMap);
					}
				}
			}
		}
		Map v = newMap==null? mold : newMap;
		return v;		
	}
	
}
