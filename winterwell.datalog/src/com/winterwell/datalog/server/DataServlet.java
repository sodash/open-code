package com.winterwell.datalog.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.util.ajax.JSON;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.DataLogImpl;
import com.winterwell.datalog.DataLogSecurity;
import com.winterwell.datalog.Dataspace;
import com.winterwell.datalog.ESStorage;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.agg.Aggregation;
import com.winterwell.es.client.agg.Aggregations;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.nlp.query.SearchQuery.SearchFormatException;
import com.winterwell.utils.Dep;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ICallable;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.AppUtils;
import com.winterwell.web.app.CommonFields;
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

	/**
	 * Number of results in aggregations
	 */
	private static final IntField numRows = new IntField("numRows");
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
				
		Dataspace dataspace = new Dataspace(state.get(DATASPACE, "default"));				
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

		// num results
		int numTerms = state.get(numRows, 1000);		
		// num examples
		int size = state.get(new IntField("size"), 10);
		// time window
		ICallable<Time> cstart = state.get(CommonFields.START);
		Time start = cstart==null? new Time().minus(TUnit.MONTH) : cstart.call();
		ICallable<Time> cend = state.get(CommonFields.END);
		Time end = cend==null? new Time() : cend.call();
		// query e.g. host:thetimes.com
		String q = state.get("q");
		SearchQuery filter = makeQueryFilter(q, start, end);

		DataLogImpl dl = (DataLogImpl) DataLog.getImplementation();
		ESStorage ess = (ESStorage) dl.getStorage();
//		ESStorage ess = Dep.get(ESStorage.class);
		
		SearchResponse sr = ess.doSearchEvents(dataspace, numTerms, size, start, end, filter, breakdown);
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

	/**
	 * @param state
	 * @param start
	 * @param end
	 * @return
	 */
	private SearchQuery makeQueryFilter(String q, Time start, Time end) {
		try {
			if (q==null) q = "";
			SearchQuery sq = new SearchQuery(q);		
			return sq;
		} catch(SearchFormatException ex) {
			throw new WebEx.BadParameterException("q", q, ex);
		}
	}

}
