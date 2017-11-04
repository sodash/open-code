package com.winterwell.web.app;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.QueryStringQueryBuilder;

import com.winterwell.data.AThing;
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
import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.SField;
import com.winterwell.youagain.client.AuthToken;
import com.winterwell.youagain.client.NoAuthException;
import com.winterwell.youagain.client.YouAgainClient;
/**
 * TODO security checks
 *  
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
		ESPath path = esRouter.getPath(dataspace,type, getId(state), KStatus.DRAFT);
		DeleteRequestBuilder del = es.prepareDelete(path.index(), path.type, path.id);
		IESResponse ok = del.get().check();		
		getThing(state);
		return jthing;
	}

	public void process(WebRequest state) throws Exception {
		// CORS??
		WebUtils2.CORS(state, false);
		
		doSecurityCheck(state);
		
		// list?
		if (state.getSlug().contains("/list")) {
			doList(state);
			return;
		}
		
		// crud?
		if (state.getAction() != null) {
			// do it
			doAction(state);
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
			// no thing?
			throw new WebEx.E404(state.getRequestUrl());
		}
		JsonResponse output = new JsonResponse(state);
		WebUtils2.sendJson(output, state);
	}
	
	protected void doSecurityCheck(WebRequest state) throws SecurityException {
		if (state.getAction() == null) {
			return;
		}
		// logged in?			
		YouAgainClient ya = Dep.get(YouAgainClient.class);
		List<AuthToken> tokens = ya.getAuthTokens(state);
		if (Utils.isEmpty(tokens)) throw new NoAuthException(state);
	}



	protected void doAction(WebRequest state) {
		// make a new thing?
		if (state.actionIs("new")) {
			// add is "special" as the only request that doesn't need an id
			String id = getId(state);
			jthing = doNew(state, id);
			jthing.setType(type);
		}
		// save?
		if (state.actionIs("save") || state.actionIs("new")) {
			doSave(state);
		}
		if (state.actionIs("discard-edits")) {
			jthing = doDiscardEdits(state);
		}
		if (state.actionIs("delete")) {
			jthing = doDelete(state);
		}
		// publish?
		if (state.actionIs("publish")) {
			jthing = doPublish(state);
			assert jthing.string().contains(KStatus.PUBLISHED.toString()) : jthing;
		}		
	}



	/**
	 * Delete from draft and published!!
	 * @param state
	 * @return
	 */
	protected JThing<T> doDelete(WebRequest state) {
		String id = getId(state);
		for(KStatus s : KStatus.main()) {
			try {
				ESPath path = esRouter.getPath(dataspace,type, id, s);
				DeleteRequestBuilder del = es.prepareDelete(path.index(), path.type, path.id);
				IESResponse ok = del.get().check();
			} catch(WebEx.E404 e404) {
				// gone already				
			}
		}
		return null;
	}

	/**
	 * 
	 * @param state
	 * @return thing or null
	 */
	protected JThing<T> getThingFromDB(WebRequest state) {		
		ESPath path = getPath(state);
		T obj = AppUtils.get(path, type);
		if (obj==null) {
			// Not found :(
			// was version=draft?
			if (state.get(AppUtils.STATUS)==KStatus.DRAFT) {
				// Try for the published version
				// NB: all published should be in draft, so this should be redundant
				WebRequest state2 = new WebRequest(state.request, state.response);
				state2.put(AppUtils.STATUS, KStatus.PUBLISHED);
				return getThingFromDB(state2);
			}
			return null;
		}
		return new JThing().setType(type).setJava(obj);
	}

	protected ESPath getPath(WebRequest state) {
		assert state != null;
		ESPath path = esRouter.getPath(dataspace,type, getId(state), state.get(AppUtils.STATUS, KStatus.PUBLISHED));
		return path;
	}

	/**
	 * Make a new thing. The state will often contain json info for this.
	 * @param state
	 * @param id Can be null ??It may be best for the front-end to normally provide IDs. 
	 * @return
	 */
	protected JThing<T> doNew(WebRequest state, String id) {
		String json = getJson(state);
		T item;
		if (json == null) {
			try {
				item = type.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw Utils.runtime(e);
			}
		} else {
			// from front end json
			item = Dep.get(Gson.class).fromJson(json, type);
			// TODO safety check ID! Otherwise someone could hack your object with a new object
//			idFromJson = AppUtils.getItemId(item);
		}
		// ID
		if (id != null) {
			if (item instanceof AThing) {
				((AThing) item).setId(id);
			}
			// else ??
		}
		if (item instanceof IInit) {
			((IInit) item).init();
		}
		return new JThing().setJava(item);
	}

	protected ESHttpClient es = Dep.get(ESHttpClient.class);
	protected final Class<T> type;
	protected JThing<T> jthing;

	final IESRouter esRouter;
	
	/**
	 * The focal thing's ID.
	 * This might be newly minted for a new thing
	 */
	private String _id;
	private String dataspace = null;

	protected JThing<T> doPublish(WebRequest state) {
		String id = getId(state);
		Utils.check4null(id); 
		ESPath draftPath = esRouter.getPath(dataspace,type, id, KStatus.DRAFT);
		ESPath publishPath = esRouter.getPath(dataspace,type, id, KStatus.PUBLISHED);
		// load (if not loaded)
		getThing(state);
		if (jthing==null) {
			jthing = getThingFromDB(state);
		}
		JThing obj = AppUtils.doPublish(jthing, draftPath, publishPath);
		return obj.setType(type);
	}

	protected String getId(WebRequest state) {
		if (_id!=null) return _id;
		_id = state.getSlugBits(1);
		if ("new".equals(_id)) {
			String nicestart = StrUtils.toCanonical(
					Utils.or(state.getUserId(), state.get("name"), type.getSimpleName()).toString()
					).replace(' ', '_');
			_id = nicestart+"_"+Utils.getRandomString(8);
			// avoid ad, 'cos adblockers dont like it!
			if (_id.startsWith("ad")) {
				_id = _id.substring(2, _id.length());
			}
		}
		return _id;
	}

	protected void doList(WebRequest state) throws IOException {
		// copied from SoGive SearchServlet
		SearchRequestBuilder s = new SearchRequestBuilder(es);
		/// which index? draft (which should include copies of published) by default
		KStatus status = state.get(AppUtils.STATUS, KStatus.DRAFT);
		if (status!=null) {
			s.setIndex(
					esRouter.getPath(dataspace, type, null, status).index()
					);
		} 
//		else {
//			s.setIndices(
//					esRouter.getPath(type, null, KStatus.PUBLISHED).index(),
//					esRouter.getPath(type, null, KStatus.DRAFT).index()
//				);
//		}
		
		// query
		String q = state.get("q");
		if ( q != null) {
			// TODO match on all?
			QueryStringQueryBuilder qsq = new QueryStringQueryBuilder(q); // QueryBuilders.queryStringQuery(q); // version incompatabilities in ES code :(			
//			multimatchquery, 
//					"id", "name", "keywords")
//							.operator(Operator.AND);
			s.setQuery(qsq);
		}
		// TODO paging!
		s.setSize(10000);
		es.debug = true;
		SearchResponse sr = s.get();
		Map<String, Object> jobj = sr.getParsedJson();
		List<Map> hits = sr.getHits();

		// prefer draft? No - in the ad portal, draft holds a copy of all ads, pubs
//		List hitSourcePreferDraft = new ArrayList(); 
//		for (Map hit : hits) {
//			Object index = hit.get("_index");
//			Object src = hit.get("_source");
//			System.out.println(hit);
//			hitsPreferDraft.add(src);
//		}
		
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

	protected void doSave(WebRequest state) {
		XId user = state.getUserId(); // TODO save who did the edit + audit trail
		T thing = getThing(state);
		assert thing != null : state;
		// set modified = true
		jthing.put("modified", true);
		{	// update
			String id = getId(state);
			ESPath path = esRouter.getPath(dataspace,type, id, KStatus.DRAFT);
			AppUtils.doSaveEdit(path, jthing, state);
		}
	}
	

	/**
	 * Get from field or state. Does NOT call the database.
	 * @param state
	 * @return
	 */
	protected T getThing(WebRequest state) {
		if (jthing!=null) {
			return jthing.java();
		}
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
