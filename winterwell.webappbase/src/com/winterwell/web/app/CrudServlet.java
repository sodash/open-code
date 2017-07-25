package com.winterwell.web.app;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.data.JThing;
import com.winterwell.data.KStatus;
import com.winterwell.depot.IInit;
import com.winterwell.es.ESPath;
import com.winterwell.es.IESRouter;
import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESHttpClient;
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
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.SField;
/**
 * TODO refactor so adserver and sogive use this
 * @author daniel
 *
 * @param <T>
 */
public abstract class CrudServlet<T> implements IServlet {


	public CrudServlet(Class<T> type) {
		this(type, Dep.get(IESRouter.class));
	}
	
	public CrudServlet(Class<T> type, IESRouter esRouter) {
		this.type = type;
		this.esRouter = esRouter;
		Utils.check4null(type, esRouter);
	}

	protected JThing<T> doDiscardEdits(WebRequest state) {
		ESPath path = esRouter.getPath(type, getId(state), KStatus.DRAFT);
		DeleteRequestBuilder del = es.prepareDelete(path.index(), path.type, path.id);
		IESResponse ok = del.get().check();		
		getThing(state);
		return jthing;
	}

	public void process(WebRequest state) throws IOException {
		// list?
		if (state.getSlug().contains("/list")) {
			doList(state);
			return;
		}
		// make a new thing?
		if (state.actionIs("new") || "new".equals(getId(state))) {
			// add is "special" as the only request that doesn't need an id
			jthing = doNew(state);
		}
		// save?
		if (state.actionIs("save")) {
			doSave(state);
		}
		if (state.actionIs("discard-edits")) {
			jthing = doDiscardEdits(state);
		}
		// publish?
		if (state.actionIs("publish")) {
			doPublish(state);
		}
		// return json?
		getThing(state);
		if (jthing==null) jthing = getThingFromDB(state); 
		if (jthing != null) {			
			String json = jthing.string();
			JsonResponse output = new JsonResponse(state).setCargoJson(json);
			WebUtils2.sendJson(output, state);
			return;
		}
		if (state.getAction()==null) {
			throw new WebEx.E404(state.getRequestUrl());
		}
		JsonResponse output = new JsonResponse(state);
		WebUtils2.sendJson(output, state);
	}
	
	protected JThing<T> getThingFromDB(WebRequest state) {		
		ESPath path = getPath(state);
		Map<String, Object> obj = AppUtils.get(path);
		if (obj==null) {
			// was version=draft?
			if (state.get(AppUtils.STATUS)==KStatus.DRAFT) {
				// Try for the published version
				WebRequest state2 = new WebRequest(state.request, state.response);
				state2.put(AppUtils.STATUS, KStatus.PUBLISHED);
				return getThingFromDB(state2);
			}
			return null;
		}
		return new JThing().setMap(obj).setType(type);
	}

	protected ESPath getPath(WebRequest state) {
		assert state != null;
		ESPath path = esRouter.getPath(type, getId(state), state.get(AppUtils.STATUS, KStatus.PUBLISHED));
		return path;
	}

	/**
	 * Make a new thing. The state will often contain json info for this.
	 * @param state
	 * @return
	 */
	protected abstract JThing<T> doNew(WebRequest state);

	protected ESHttpClient es = Dep.get(ESHttpClient.class);
	protected final Class<T> type;
	protected JThing<T> jthing;

	final IESRouter esRouter;

	protected T doPublish(WebRequest state) {
		String id = getId(state);
		Utils.check4null(id); 
		ESPath draftPath = esRouter.getPath(type, id, KStatus.DRAFT);
		ESPath publishPath = esRouter.getPath(type, id, KStatus.PUBLISHED);		
		Map<String, Object> obj = AppUtils.doPublish(draftPath, publishPath);
		return (T) new JThing().setMap(obj).setType(type).java();
	}

	protected String getId(WebRequest state) {
		return state.getSlugBits(1);
	}

	protected void doList(WebRequest state) throws IOException {
//		// copied from SoGive SearchServlet
//		SearchRequestBuilder s = new SearchRequestBuilder(es).setIndices(
//				config.publisherIndex(KStatus.PUBLISHED),
//				config.publisherIndex(KStatus.PENDING),
//				config.publisherIndex(KStatus.DRAFT), 
//				config.publisherIndex(KStatus.REQUEST_PUBLISH));
//		String q = state.get("q");
//		if ( q != null) {
//			QueryBuilder qb = QueryBuilders.multiMatchQuery(q, 
//					"id", "name", "keywords")
//							.operator(Operator.AND);
//			s.setQuery(qb);
//		}
//		// TODO paging!
//		s.setSize(10000);
//		SearchResponse sr = s.get();
//		Map<String, Object> jobj = sr.getParsedJson();
//		List<Map> hits = sr.getHits();
//		List hits2 = Containers.apply(hits, h -> h.get("_source"));
//		long total = sr.getTotal();
//		String json = Dep.get(Gson.class).toJson(
//				new ArrayMap(
//					"hits", hits2, 
//					"total", total
//				));
//		JsonResponse output = new JsonResponse(state).setCargoJson(json);
//		WebUtils2.sendJson(output, state);
	}

	protected void doSave(WebRequest state) {
		XId user = state.getUserId(); // TODO save who did the edit + audit trail
		T thing = getThing(state);
		assert thing != null : state;
		// set modified = true
		jthing.put("modified", true);
		{	// update
			String id = getId(state);
			ESPath path = esRouter.getPath(type, id, KStatus.DRAFT);
			AppUtils.doSaveEdit(path, jthing, state);
		}
	}
	

	/**
	 * Get from field or state. Does NOT call the database.
	 * @param state
	 * @return
	 */
	protected T getThing(WebRequest state) {
		if (jthing!=null) return jthing.java();
		String json = getJson(state);
		if (json==null) {
			return null;
		}
		jthing = new JThing(json).setType(type);
		return jthing.java();
	}

	protected String getJson(WebRequest state) {
		return state.get(new SField(AppUtils.ITEM.getName()));
	}
	
}
