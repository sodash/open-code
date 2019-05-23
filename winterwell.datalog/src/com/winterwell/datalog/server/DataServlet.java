package com.winterwell.datalog.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.DataLogImpl;
import com.winterwell.datalog.DataLogSecurity;
import com.winterwell.datalog.Dataspace;
import com.winterwell.datalog.ESStorage;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.nlp.query.SearchQuery.SearchFormatException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ICallable;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.Json2Csv;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.app.WebRequest.KResponseType;
import com.winterwell.web.fields.IntField;
import com.winterwell.web.fields.ListField;
import com.winterwell.web.fields.SField;

/**
 * Serves up aggregations data.
 * 
 * size: number of examples
 * numRows: max terms in the breakdown
 * 
 * @author daniel
 *
 */
public class DataServlet implements IServlet {

	/**
	 * Number of results in aggregations
	 */
	private static final IntField numRows = new IntField("numRows");
	public static final SField DATASPACE = new SField("dataspace");
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
		List<String> breakdown = state.get(DataLogFields.breakdown);
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
		ICallable<Time> cstart = state.get(DataLogFields.START);
		Time start = cstart==null? new Time().minus(TUnit.MONTH) : cstart.call();
		ICallable<Time> cend = state.get(DataLogFields.END);
		Time end = cend==null? new Time() : cend.call();
		
		// TODO distribution data??
//		ess.getMean(start, end, tag);
		
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
		List<Map> egs = sr.getHits();
		aggregations.put("examples", egs);
		
		// csv? Send a table of the examples=results
		if (state.getResponseType() == KResponseType.csv) {
			doSendCSV(state, egs);
			return;
		}
		
		JsonResponse jr = new JsonResponse(state, aggregations);
		WebUtils2.sendJson(jr, state);
	}

	/**
	 * Convert json into a csv (fairly crudely)
	 * @param state
	 * @param egs
	 */
	private void doSendCSV(WebRequest state, List<Map> egs) {
		HttpServletResponse response = state.getResponse();
		BufferedWriter out = null;
		try {
			response.setContentType(WebUtils.MIME_TYPE_CSV);
			out = FileUtils.getWriter(response.getOutputStream());
			CSVWriter w = new CSVWriter(out, new CSVSpec());
			Json2Csv j2c = new Json2Csv(w);

			// optionally have headers set
			List<String> headers = state.get(new ListField<>("headers"));
			if (headers != null) {
				j2c.setHeaders(headers);
			}
			// convert!
			j2c.run(egs);
			
			FileUtils.close(w);
		} catch (IOException e) {
			throw Utils.runtime(e);
		} finally {
			FileUtils.close(out);
		}
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
