package com.winterwell.web.app;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.WebRequest.KResponseType;
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


	public static final String ACTION_PUBLISH = "publish";
	private static final String ACTION_NEW = "new";

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
		YouAgainClient ya = Dep.get(YouAgainClient.class);
		List<AuthToken> tokens = ya.getAuthTokens(state);
		if (state.getAction() == null) {
			return;
		}
		// logged in?					
		if (Utils.isEmpty(tokens)) throw new NoAuthException(state);
	}



	protected void doAction(WebRequest state) {
		// make a new thing?
		if (state.actionIs(ACTION_NEW)) {
			// add is "special" as the only request that doesn't need an id
			String id = getId(state);
			jthing = doNew(state, id);
			jthing.setType(type);
		}
		// save?
		if (state.actionIs("save") || state.actionIs(ACTION_NEW)) {
			doSave(state);
		}
		if (state.actionIs("discard-edits")) {
			jthing = doDiscardEdits(state);
		}
		if (state.actionIs("delete")) {
			jthing = doDelete(state);
		}
		// publish?
		if (state.actionIs(ACTION_PUBLISH)) {
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
		if (ACTION_NEW.equals(_id)) {
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
		if (status!=null && status != KStatus.ALL_BAR_TRASH) {
			s.setIndex(
					esRouter.getPath(dataspace, type, null, status).index()
					);
		} else {
			s.setIndices(
					esRouter.getPath(type, null, KStatus.PUBLISHED).index(),
					esRouter.getPath(type, null, KStatus.DRAFT).index()
				);
		}
		
		// query
		String q = state.get("q");
		QueryBuilder qb = null;
		if ( q != null) {
			// TODO match on all?
			QueryStringQueryBuilder qsq = new QueryStringQueryBuilder(q); // QueryBuilders.queryStringQuery(q); // version incompatabilities in ES code :(			
//			multimatchquery, 
//					"id", "name", "keywords")
//							.operator(Operator.AND);
			qb = qsq;
		}
		QueryBuilder exq = doList2_query(state);
		if (exq!=null) {
			if (qb==null) {
				qb = exq;
			} else {
				qb = QueryBuilders.boolQuery().must(exq).must(qb);
			}
		}
		if (qb!=null) s.setQuery(qb);
		
		// TODO paging!
		s.setSize(10000);
		s.setDebug(true);
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
		
		// NB: may be Map or AThing
		List hits2 = Containers.apply(hits, h -> h.get("_source"));
		
		// de-dupe by status: remove draft, etc if we have published
		// NB: assumes you can't have same status and same ID (so no need to de-dupe)
		if (status==KStatus.ALL_BAR_TRASH) {
			HashSet pubIds = new HashSet();
			for(Object hit : hits2) {
				Object id = getId(hit);
				String hs = getStatus(hit);
				if ("PUBLISHED".equals(hs)) {
					pubIds.add(id);
				}
			}
			hits2 = Containers.filter(hits2, h -> {
				String hs = getStatus(h);
				if ( ! "PUBLISHED".equals(hs)) {
					boolean dupe = pubIds.contains(getId(h));
					return ! dupe;
				}
				return true;
			});
		}
		
		// HACK: send back csv?
		if (state.getResponseType() == KResponseType.csv) {
			doSendCsv(state, hits2);
			return;
		}
			
		long total = sr.getTotal();
		String json = Dep.get(Gson.class).toJson(
				new ArrayMap(
					"hits", hits2, 
					"total", total
				));
		JsonResponse output = new JsonResponse(state).setCargoJson(json);
		WebUtils2.sendJson(output, state);		
	}
	
	
	private String getStatus(Object h) {
		Object s;
		if (h instanceof Map) s = ((Map)h).get("status");
		else s = ((AThing)h).getStatus();
		return String.valueOf(s);
	}



	private Object getId(Object hit) {
		Object id;
		if (hit instanceof Map) id = ((Map)hit).get("id");
		else id = ((AThing)hit).getId();
		return id;
	}



	protected void doSendCsv(WebRequest state, List<Map> hits2) {
		// ?? maybe refactor and move into a default method in IServlet?
		StringWriter sout = new StringWriter();
		CSVWriter w = new CSVWriter(sout, new CSVSpec());
	
		// what headers??
		ArrayMap<String, String> hs = doSendCsv2_getHeaders(state, hits2);
		
		// write
		w.write(hs.values());
		for (Map hit : hits2) {
			List<Object> line = Containers.apply(hs, h -> {
				String[] p = h.split("\\.");
				return SimpleJson.get(hit, p);
			});
			w.write(line);
		}
		w.close();
		// send
		String csv = sout.toString();
		state.getResponse().setContentType(WebUtils.MIME_TYPE_CSV); // + utf8??
		WebUtils2.sendText(csv, state.getResponse());
	}
	

	/**
	 * 
	 * @param state
	 * @param hits2 
	 * @return
	 */
	protected ArrayMap<String,String> doSendCsv2_getHeaders(WebRequest state, List<Map> hits2) {
		if (hits2.isEmpty()) return new ArrayMap();
		Map hit = hits2.get(0);
		ArrayMap map = new ArrayMap();
		for(Object k : hit.keySet()) {
			map.put(""+k, ""+k);
		}
		return map;
//		// TODO proper recursive
//		ObjectDistribution<String> headers = new ObjectDistribution();
//		for (Map<String,Object> hit : hits2) {
//			getHeaders(hit, new ArrayList(), headers);
//		}
//		// prune
//		if (hits2.size() >= 1) {
//			int min = (int) (hits2.size() * 0.2);
//			if (min>0) headers.pruneBelow(min);
//		}
//		// sort
//		ArrayList<String> hs = new ArrayList(headers.keySet());
//		// all the level 1 headers
//		List<String> level1 = Containers.filter(hs, h -> ! h.contains("."));
//		hs.removeAll(level1);
//		Collections.sort(hs);
//		Collections.sort(level1);		
//		// start with ID, name
//		level1.remove("name");
//		level1.remove("@id");
//		Collections.reverse(level1);
//		level1.add("name");
//		level1.add("@id");		
//		level1.forEach(h -> hs.add(0, h));
//		hs.removeIf(h -> h.contains("@type") || h.contains("value100"));
	}



	/**
	 * 
	 * @param state
	 * @return null or a query
	 */
	protected QueryBuilder doList2_query(WebRequest state) {
		return null;
	}



	protected void doSave(WebRequest state) {
		// debug FIXME		
		String json = getJson(state);
		Object jobj = JSON.parse(json);
		Object start = SimpleJson.get(jobj, "projects", 0, "start");
		
		XId user = state.getUserId(); // TODO save who did the edit + audit trail
		T thing = getThing(state);
		assert thing != null : state;
		// HACK set modified = true on maps
		if (thing instanceof Map) {
			((Map) thing).put("modified", true);	
		} else {
			// should we have an interface for this??
//			ReflectionUtils.setPrivateField(thing, fieldName, value);
			// NB: avoiding jthing.put() as that re-makes the java object, which is wasteful and confusing
//			jthing.put("modified", true);
		}
		
		// This has probably been done already in getThing(), but harmless to repeat
		// run the object through Java, to trigger IInit
		jthing.java();
		
		// FIXME debug
		Object start2 = SimpleJson.get(jthing.map(), "projects", 0, "start");
		Object startraw = SimpleJson.get(jthing.map(), "projects", 0, "start_raw");
		T ngo = jthing.java();
		 
		
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
