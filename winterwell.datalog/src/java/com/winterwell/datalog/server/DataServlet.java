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
import com.winterwell.datalog.ESDataLogSearchBuilder;
import com.winterwell.datalog.ESStorage;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.SearchRequest;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ICallable;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.Json2Csv;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.DtField;
import com.winterwell.web.fields.IntField;
import com.winterwell.web.fields.ListField;
import com.winterwell.web.fields.SField;
import com.winterwell.youagain.client.AuthToken;
import com.winterwell.youagain.client.YouAgainClient;

/**
 * Serves up aggregations data.
 * 
 * size: number of examples
 * numRows: max terms in the breakdown
 * breakdown: e.g. evt/time 
 * 	See {@link ESDataLogSearchBuilder} for more info on the breakdown syntax

 * 
 * @author daniel
 * @testedby  DataServletTest}
 */
public class DataServlet implements IServlet {

	/**
	 * Number of results in aggregations
	 */
	private static final IntField numRows = new IntField("numRows");
	private static final IntField SIZE = new IntField("size");
	public static final SField DATASPACE = new SField("dataspace");
	private static final String LOGTAG = "DataServlet";

	@Override
	public void process(WebRequest state) throws IOException {						
				
		Dataspace dataspace = new Dataspace(state.get(DATASPACE, "default"));				
		// Uses "paths" of breakdown1/breakdown2/... {field1:operation, field2}
		List<String> breakdown = state.get(DataLogFields.breakdown);
		if (breakdown==null) {
//			Log.w(LOGTAG, "You want data but no breakdown?! Default to time. "+state);
			breakdown = new ArrayList();
			breakdown.add("time");
		}
		// remove `none` if present (which is to block the default)
		breakdown.remove("none");

		// security: on the dataspace, and optionally on the breakdown
		DataLogSecurity.check(state, dataspace, breakdown);

		// num results
		int numTerms = state.get(numRows, 1000);		
		
		// num examples
		int size = state.get(SIZE, 10);
		// ONLY give examples for logged in users
		if ( ! isLoggedIn(state) && size > 0) {			
			size = 0;
			state.addMessage("Not logged in => no examples");
		}
		
		// time window
		ICallable<Time> cstart = state.get(DataLogFields.START);
		Time start = cstart==null? new Time().minus(TUnit.MONTH) : cstart.call();
		ICallable<Time> cend = state.get(DataLogFields.END);
		Time end = cend==null? new Time() : cend.call();
		
		// TODO distribution data??
//		ess.getMean(start, end, tag);
		
		// query e.g. host:thetimes.com
		String q = state.get("q");
		if (q==null) q = "";
		SearchQuery filter = new SearchQuery(q);				

		DataLogImpl dl = (DataLogImpl) DataLog.getImplementation();
		ESStorage ess = (ESStorage) dl.getStorage();
//		ESStorage ess = Dep.get(ESStorage.class);
		
		ESHttpClient esc = ess.client(dataspace);

		// collect all the info together
		ESDataLogSearchBuilder essb = new ESDataLogSearchBuilder(esc, dataspace);		
		essb.setBreakdown(breakdown)
			.setQuery(filter)
			.setNumResults(numTerms)
			.setStart(start)
			.setEnd(end);
		Dt interval = state.get(new DtField("interval"), TUnit.DAY.dt);
		essb.setInterval(interval);
		
		SearchRequest search = essb.prepareSearch();		
		search.setDebug(true);
//		search.setType(typeFromEventType(spec.eventType)); all types unless fixed
		search.setSize(size);
		
		// Search!
		SearchResponse sr = search.get();		
		sr.check();
		
		Map aggregations = sr.getAggregations();
		if (aggregations==null) {
			Log.d(LOGTAG, "No aggregations?! "+state+" "+sr);
			aggregations = new ArrayMap();
		}
		// strip out no0 filter wrappers
		aggregations = essb.cleanJson(aggregations);
		// also send eg data
		aggregations.put("examples", sr.getHits());		
		// debug?
		if (state.debug && isLoggedIn(state)) {
			aggregations.put("debug", search.getCurl());
		}
		// done
		JsonResponse jr = new JsonResponse(state, aggregations);		
		WebUtils2.sendJson(jr, state);
	}

	/**
	 * @param state
	 * @return true if state has an AuthToken. Does NOT check validity of the token!
	 */
	boolean isLoggedIn(WebRequest state) {		
		YouAgainClient yac = Dep.get(YouAgainClient.class);
		List<AuthToken> authd = yac.getAuthTokens(state);
		for(AuthToken at : authd) {
			if (at.isTemp()) continue;
			return true;
		}
		return false;
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


}
