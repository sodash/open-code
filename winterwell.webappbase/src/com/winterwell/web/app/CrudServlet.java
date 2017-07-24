package com.winterwell.web.app;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.data.KStatus;
import com.winterwell.depot.IInit;
import com.winterwell.es.ESPath;
import com.winterwell.es.IESRouter;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.UpdateRequestBuilder;
import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;
/**
 * TODO refactor so adserver and sogive use this
 * @author daniel
 *
 * @param <T>
 */
public class CrudServlet<T> implements IServlet {

	@Override
	public void process(WebRequest state) throws Exception {
		
	}
	
	Class<T> type;
	T thing;

	IESRouter esRouter;

	private T doPublish(WebRequest state) {
		String id = getId(state);
		Utils.check4null(id); 
		ESPath draftPath = esRouter.getPath(type, id, KStatus.DRAFT);
		ESPath publishPath = esRouter.getPath(type, id, KStatus.PUBLISHED);		
		Map<String, Object> obj = AppUtils.doPublish(draftPath, publishPath);
		return thing;
	}

	private String getId(WebRequest state) {
		return state.getSlugBits(1);
	}

	private void doList(WebRequest state) throws IOException {
		// copied from SoGive SearchServlet
		SearchRequestBuilder s = new SearchRequestBuilder(es).setIndices(
				config.publisherIndex(KStatus.PUBLISHED),
				config.publisherIndex(KStatus.PENDING),
				config.publisherIndex(KStatus.DRAFT), 
				config.publisherIndex(KStatus.REQUEST_PUBLISH));
		String q = state.get("q");
		if ( q != null) {
			QueryBuilder qb = QueryBuilders.multiMatchQuery(q, 
					"id", "name", "keywords")
							.operator(Operator.AND);
			s.setQuery(qb);
		}
		// TODO paging!
		s.setSize(10000);
		SearchResponse sr = s.get();
		Map<String, Object> jobj = sr.getParsedJson();
		List<Map> hits = sr.getHits();
		List hits2 = Containers.apply(hits, h -> h.get("_source"));
		long total = sr.getTotal();
		String json = Dep.get(Gson.class).toJson(
				new ArrayMap(
					"hits", hits2, 
					"total", total
				));
		JsonResponse output = new JsonResponse(state).setCargoJson(json);
		WebUtils2.sendJson(output, state);
	}

	private void doSave(WebRequest state) {
		String json = state.get("publisher");
		String id = state.getSlugBits(1);
//		if (id.endsWith(".json")) id = id.substring(0, id.length()-5); not needed - done in slug??
		Gson gson = Dep.get(Gson.class);
		thing = gson.fromJson(json, type);
		JSON.parse(json);
		if (thing instanceof IInit) {
			((IInit) thing).init();
		}
		assert true;
		// new? No -- created by installing the code on your site
		assert id.equals(publisher.id);
		{	// update
			UpdateRequestBuilder pu = es.prepareUpdate(config.publisherIndex(publisher.getStatus()), config.publisherType, id);
			pu.setDoc(json);		
			IESResponse r = pu.get().check();
		}
	}
}
