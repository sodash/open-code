package com.winterwell.web.app;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.ajax.JSON;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.winterwell.data.AThing;
import com.winterwell.data.KStatus;
import com.winterwell.depot.IInit;
import com.winterwell.es.ESPath;
import com.winterwell.es.IESRouter;
import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.query.ESQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JThing;
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
		String slug = state.getSlug();
		if (slug.endsWith("/_list") || slug.equals("_list")) {
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
			// TODO privacy: potentially filter some stuff from the json!
			JsonResponse output = new JsonResponse(state).setCargoJson(json);			
			WebUtils2.sendJson(output, state);
			return;
		}
		// return blank / messages
		if (state.getAction()==null) {
			// no thing?
			throw new WebEx.E404(state.getRequestUrl());
		}
		JsonResponse output = new JsonResponse(state);
		WebUtils2.sendJson(output, state);
	}
	
	protected void doSecurityCheck(WebRequest state) throws SecurityException {
		YouAgainClient ya = Dep.get(YouAgainClient.class);
		ReflectionUtils.setPrivateField(state, "debug", true); // FIXME
		List<AuthToken> tokens = ya.getAuthTokens(state);
		if (state.getAction() == null) {
			return;
		}
		// logged in?					
		if (Utils.isEmpty(tokens)) {
			Log.w("crud", "No auth tokens for "+this+" "+state);
			throw new NoAuthException(state);
		}
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
		// try to copy to trash
		try {
			JThing<T> thing = getThingFromDB(state);
			if (thing != null) {
				ESPath path = esRouter.getPath(dataspace, type, id, KStatus.TRASH);
				AppUtils.doSaveEdit2(path, thing, state, false);
			}
		} catch(Throwable ex) {
			Log.e(LOGTAG(), "copy to trash failed: "+state+" -> "+ex);
		}
		for(KStatus s : KStatus.main()) {
			if (s==KStatus.TRASH) continue;
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
		KStatus status = state.get(AppUtils.STATUS);
		// fetch from DB
		T obj = AppUtils.get(path, type);		
		if (obj!=null) {
			JThing thing = new JThing().setType(type).setJava(obj);
			// HACK force status?
			if (status==KStatus.DRAFT && AppUtils.getStatus(thing) == KStatus.PUBLISHED) {
				thing = AppUtils.setStatus(thing, status);
			}
			// success
			return thing;
		}
		
		// Not found :(
		// was version=draft?
		if (status == KStatus.DRAFT) {			
			// Try for the published version
			// NB: all published should be in draft, so this should be redundant
			WebRequest state2 = new WebRequest(state.request, state.response);
			state2.put(AppUtils.STATUS, KStatus.PUBLISHED);
			JThing<T> pubThing = getThingFromDB(state2);
			if (pubThing != null) {
				// NB: this won't exist in the draft DB yet (so that merely viewing an item in an editor doesn't make a draft)
				// -- but if it is edited, then save-edits should make a draft
				JThing<T> draftThing = AppUtils.setStatus(pubThing, KStatus.DRAFT);
				return draftThing;
			}
		}
		return null;
	}

	protected ESPath getPath(WebRequest state) {
		assert state != null;
		String id = getId(state);
		if ("list".equals(id)) {
			throw new WebEx.E400("Bad input: 'list' was interpreted as an ID -- use /_list.json to retrieve a list.");
		}
		ESPath path = esRouter.getPath(dataspace,type, id, state.get(AppUtils.STATUS, KStatus.PUBLISHED));
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

	protected final IESRouter esRouter;
	
	/**
	 * The focal thing's ID.
	 * This might be newly minted for a new thing
	 */
	private String _id;
	
	/**
	 * Optional support for dataspace based data access.
	 */
	protected CharSequence dataspace = null;
	
	public CrudServlet setDataspace(CharSequence dataspace) {
		this.dataspace = dataspace;
		return this;
	}
	
	/**
	 * suggested: date-desc
	 */
	protected String defaultSort;
	
	public static final SField SORT = new SField("sort");

	protected final JThing<T> doPublish(WebRequest state) {
		return doPublish(state, false, false);
	}
	protected JThing<T> doPublish(WebRequest state, boolean forceRefresh, boolean deleteDraft) {		
		String id = getId(state);
		Log.d("crud", "doPublish "+id+" "+state+" deleteDraft: "+deleteDraft);
		Utils.check4null(id); 
		ESPath draftPath = esRouter.getPath(dataspace,type, id, KStatus.DRAFT);
		ESPath publishPath = esRouter.getPath(dataspace,type, id, KStatus.PUBLISHED);
		// load (if not loaded)
		getThing(state);
		if (jthing==null) {
			jthing = getThingFromDB(state);
		}
		JThing obj = AppUtils.doPublish(jthing, draftPath, publishPath, forceRefresh, deleteDraft);
		return obj.setType(type);
	}

	protected String getId(WebRequest state) {
		if (_id!=null) return _id;
		_id = state.getSlugBits(1); // why 1 not 0??
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
		// TODO refactor to use makeESFilterFromSearchQuery
		SearchRequestBuilder s = new SearchRequestBuilder(es);
		/// which index? draft (which should include copies of published) by default
		KStatus status = state.get(AppUtils.STATUS, KStatus.DRAFT);
		if (status!=null && status != KStatus.ALL_BAR_TRASH) {
			s.setIndex(
					esRouter.getPath(dataspace, type, null, status).index()
					);
		} else {
			s.setIndices(
					esRouter.getPath(dataspace, type, null, KStatus.PUBLISHED).index(),
					esRouter.getPath(dataspace, type, null, KStatus.DRAFT).index()
				);
		}
		
		// query
		String q = state.get("q");
		ESQueryBuilder qb = null;
		if ( q != null) {
			// TODO match on all?
			// HACK strip out unset
			if (q.contains(":unset")) {
				Matcher m = Pattern.compile("(\\w+):unset").matcher(q);
				m.find();
				String prop = m.group(1);
				String q2 = m.replaceAll("").trim();
				q = q2;
				ESQueryBuilder setFilter = ESQueryBuilders.existsQuery(prop);
				qb = ESQueryBuilders.boolQuery().mustNot(setFilter);
			}	
			if ( ! Utils.isBlank(q)) {
				QueryStringQueryBuilder qsq = new QueryStringQueryBuilder(q); // QueryBuilders.queryStringQuery(q); // version incompatabilities in ES code :(			
				qb = ESQueryBuilders.must(qb, qsq);
			}
		}
		ESQueryBuilder exq = doList2_query(state);
		qb = ESQueryBuilders.must(qb, exq);

		if (qb!=null) s.setQuery(qb);
				
		// Sort e.g. sort=date-desc for most recent first
		String sort = state.get(SORT, defaultSort);
		if (sort!=null) {
			// HACK: order?
			SortOrder order = SortOrder.ASC;
			if (sort.endsWith("-desc")) {
				sort = sort.substring(0, sort.length()-5);
				order = SortOrder.DESC;
			} else if (sort.endsWith("-asc")) {
				sort = sort.substring(0, sort.length()-4);
			}
			s.addSort(sort, order);
		}
		
		// TODO paging!
		s.setSize(10000);
		s.setDebug(true);
		SearchResponse sr = s.get();
		Map<String, Object> jobj = sr.getParsedJson();
		List<Map> hits = sr.getHits();

		// If user requests ALL_BAR_TRASH, they want to see draft versions of items which have been edited
		// So when de-duping, give priority to entries from .draft indices where the object is status: DRAFT
		List hits2 = new ArrayList<Map>();
		
		if (status == KStatus.ALL_BAR_TRASH) {
			List<Object> idOrder = new ArrayList<Object>(); // original ordering
			Map<Object, Object> things = new HashMap<Object, Object>(); // to hold "expected" version of each hit
			
			for (Map h : hits) {
				// pull out the actual object from the hit (NB: may be Map or AThing)
				Object hit = h.get("_source");
				if (hit == null) continue;
				Object id = getId(hit);
				
				// First time we've seen this object? Save it.
				if (!things.containsKey(id)) {
					idOrder.add(id);
					things.put(id, hit);
					continue;
				}
				// Is this an object from .draft with non-published status? Overwrite the previous entry.
				Object index = h.get("_index");
				if (index != null && index.toString().contains(".draft")) {
					KStatus hitStatus = KStatus.valueOf(getStatus(hit));
					if (KStatus.DRAFT.equals(hitStatus) || KStatus.MODIFIED.equals(hitStatus)) {
						things.put(id, hit);
					}
				}
			}
			// Put the deduped hits in the list in their original order.
			for (Object id : idOrder) {
				if (things.containsKey(id)) hits2.add(things.get(id));
			}
		} else {
			// One index = no deduping necessary.
			hits2 = Containers.apply(hits, h -> h.get("_source"));
		}
		
		// sanitise for privacy
		hits2 = cleanse(hits2, state);
		
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


	/**
	 * TODO remove sensitive details for privacy
	 * @param hits2
	 * @param state
	 * @return
	 */
	protected List cleanse(List hits2, WebRequest state) {
		return hits2;
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
	protected ESQueryBuilder doList2_query(WebRequest state) {
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
			assert id != null : "No id? cant save! "+state; 
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
