package com.winterwell.web.app;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.winterwell.data.AThing;
import com.winterwell.data.KStatus;
import com.winterwell.depot.IInit;
import com.winterwell.es.ESPath;
import com.winterwell.es.IESRouter;
import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.KRefresh;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.query.BoolQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.es.client.sort.KSortOrder;
import com.winterwell.es.client.sort.Sort;
import com.winterwell.gson.FlexiGson;
import com.winterwell.gson.Gson;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Key;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.WebRequest.KResponseType;
import com.winterwell.web.data.IHasXId;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.Checkbox;
import com.winterwell.web.fields.IntField;
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


	protected boolean dataspaceFromPath;
	public static final String ACTION_PUBLISH = "publish";
	public static final String ACTION_NEW = "new";
	/**
	 * get, or create if absent
	 */
	public static final String ACTION_GETORNEW = "getornew";
	public static final String ACTION_SAVE = "save";

	public CrudServlet(Class<T> type) {
		this(type, Dep.get(IESRouter.class));
	}
	
	
	public CrudServlet(Class<T> type, IESRouter esRouter) {
		this.type = type;
		this.esRouter = esRouter;
		Utils.check4null(type, esRouter);
	}

	protected JThing<T> doDiscardEdits(WebRequest state) {
		ESPath path = esRouter.getPath(dataspace, type, getId(state), KStatus.DRAFT);
		DeleteRequestBuilder del = es.prepareDelete(path.index(), path.type, path.id);
		IESResponse ok = del.get().check();		
		getThing(state);
		return jthing;
	}

	public void process(WebRequest state) throws Exception {
		// CORS??
		WebUtils2.CORS(state, false);
		
		// dataspace?
		if (dataspaceFromPath) {
			String ds = state.getSlugBits(1);
			setDataspace(ds);
		}
		
		doSecurityCheck(state);
		
		// list?
		String slug = state.getSlug();
		if (slug.endsWith("/_list") || LIST_SLUG.equals(slug)) {
			doList(state);
			return;
		}
		if (slug.endsWith("/_stats") || "_stats".equals(slug)) {
			doStats(state);
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
			Log.w("crud", "No auth tokens for "+this+" "+state+" All JWT: "+ya.getAllJWTTokens(state));
			throw new NoAuthException(state);
		}
	}

	/**
	 * ES takes 1 second to update by default, so save actions within a second could
	 * cause an issue. Allow an extra second to be safe.
	 */
	static final Cache<String,Boolean> ANTI_OVERLAPPING_EDITS_CACHE = CacheBuilder.newBuilder()
			.expireAfterWrite(2, TimeUnit.SECONDS)
			.build();
	private static final Checkbox ALLOW_OVERLAPPING_EDITS = new Checkbox("allowOverlappingEdits");
	
	protected void doAction(WebRequest state) {
		// Defend against repeat calls from the front end
		doAction2_blockRepeats(state);		
		// make a new thing?
		// ...only if absent?
		if (state.actionIs(ACTION_GETORNEW)) {
			JThing<T> thing = getThingFromDB(state);
			if (thing != null) {
				jthing = thing;
				return;
			}
			// absent => new
			state.setAction(ACTION_NEW);
		}
		// ...new
		if (state.actionIs(ACTION_NEW)) {
			// add is "special" as the only request that doesn't need an id
			String id = getId(state);
			jthing = doNew(state, id);
			jthing.setType(type);
		}
		
		// save?
		if (state.actionIs(ACTION_SAVE) || state.actionIs(ACTION_NEW)) {
			doSave(state);
			return;
		}
		// copy / save-as?
		if (state.actionIs("copy")) {
			doCopy(state);
			return;
		}
		if (state.actionIs("discard-edits") || state.actionIs("discardEdits")) {
			jthing = doDiscardEdits(state);
			return;
		}
		if (state.actionIs("delete")) {
			jthing = doDelete(state);
			return;
		}
		// publish?
		if (state.actionIs(ACTION_PUBLISH)) {
			jthing = doPublish(state);
			assert jthing.string().contains(KStatus.PUBLISHED.toString()) : jthing;
			return;
		}
		if (state.actionIs("unpublish")) {
			jthing = doUnPublish(state);
			return;
		}
	}


	protected void doAction2_blockRepeats(WebRequest state) {
		if (state.get(ALLOW_OVERLAPPING_EDITS)) {
			return;
		}
		String ckey = doAction2_blockRepeats2_actionId(state);
		Log.d(LOGTAG(), "Anti overlap key: "+ckey);
		if (ANTI_OVERLAPPING_EDITS_CACHE.getIfPresent(ckey)!=null) {
			throw new WebEx.E409Conflict("Duplicate request within 2 seconds. Blocked for edit safety. "+state
					+" Note: this behaviour could be switched off via "+ALLOW_OVERLAPPING_EDITS);
		}		
		ANTI_OVERLAPPING_EDITS_CACHE.put(ckey, true);
	}

	/**
	 * @param state
	 * @return the id for this action -- this determines what counts as identical (and hence will be blocked)
	 */
	protected String doAction2_blockRepeats2_actionId(WebRequest state) {
		Map<String, Object> pmap = state.getParameterMap();
		String ckey = state.getAction()+FlexiGson.toJSON(pmap);
		return ckey;
	}


	/**
	 * Copy / save-as -- this is almost the same as save. 
	 * But it can clear some values which should not be copied -- e.g. external linking ids.
	 * @param state
	 */
	protected void doCopy(WebRequest state) {
		// clear linking ids
		T thing = getThing(state);
		if (thing instanceof IHasXId) {
			try {
				((IHasXId) thing).setAka(new ArrayList());
			} catch(UnsupportedOperationException ex) {
				// oh well
			}
		}
		// save 
		doSave(state);
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
				del.setRefresh("wait_for");
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
	 * @throws TODO WebEx.E403
	 */
	protected JThing<T> getThingFromDB(WebRequest state) throws WebEx.E403 {
		ESPath path = getPath(state);
		KStatus status = state.get(AppUtils.STATUS);
		// fetch from DB
		T obj = AppUtils.get(path, type);		
		if (obj!=null) {
			JThing thing = new JThing().setType(type).setJava(obj);
			return thing;
		}
		
		// Not found :(
		// was version=draft?
		if (status == KStatus.DRAFT) {			
			// Try for the published version
			// NB: all published should be in draft, so this should be redundant
			// ?? maybe refactor to use a getThinfFromDB2(state, status) method? But beware CharityServlet has overriden this
			WebRequest state2 = new WebRequest(state.request, state.response);
			state2.put(AppUtils.STATUS, KStatus.PUBLISHED);
			JThing<T> pubThing = getThingFromDB(state2);
			return pubThing;
		}
		// Was status unset? Maybe the published version got archived?
		if (status == null) {			
			// Try for an archived version
			WebRequest state2 = new WebRequest(state.request, state.response);
			state2.put(AppUtils.STATUS, KStatus.ARCHIVED);
			JThing<T> pubThing = getThingFromDB(state2);
			return pubThing;
		}		
		return null;
	}

	/**
	 * Use getId() to make an ESPath.
	 * NB: the path depends on status - defaulting to published 
	 * @param state
	 * @return
	 */
	protected ESPath getPath(WebRequest state) {
		assert state != null;
		String id = getId(state);
		if ("list".equals(id)) {
			throw new WebEx.E400(
					state.getRequestUrl(),
					"Bad input: 'list' was interpreted as an ID -- use /_list.json to retrieve a list.");
		}
		KStatus status = state.get(AppUtils.STATUS, KStatus.PUBLISHED);
		ESPath path = esRouter.getPath(dataspace,type, id, status);
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
	// NB: the Dataspace class is not in the scope of this project, hence the super-class CharSequence
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
	public static final String LIST_SLUG =  "_list";
	private static final IntField SIZE = new IntField("size");
	public static final SField Q = new SField("q");
	public static final String ALL = "all";

	protected final JThing<T> doPublish(WebRequest state) {
		// wait 1 second??
		return doPublish(state, KRefresh.WAIT_FOR, false);
	}
	
	protected JThing<T> doPublish(WebRequest state, KRefresh forceRefresh, boolean deleteDraft) {		
		String id = getId(state);
		Log.d("crud", "doPublish "+id+" by "+state.getUserId()+" "+state+" deleteDraft: "+deleteDraft);
		Utils.check4null(id); 
		// load (if not loaded)
		getThing(state);
		if (jthing==null) {
			jthing = getThingFromDB(state);
		}
		return doPublish2(dataspace, jthing, forceRefresh, deleteDraft, id);
	}



	/**
	 * @param _jthing 
	 * @param forceRefresh
	 * @param deleteDraft
	 * @param id
	 * @return
	 */
	protected JThing<T> doPublish2(CharSequence dataspace, JThing<T> _jthing, KRefresh forceRefresh, boolean deleteDraft, String id) {
		ESPath draftPath = esRouter.getPath(dataspace, type, id, KStatus.DRAFT);
		ESPath publishPath = esRouter.getPath(dataspace, type, id, KStatus.PUBLISHED);
		// id must match
		if (_jthing.java() instanceof AThing) {
			String thingId = ((AThing) _jthing.java()).getId();
			if (thingId==null || ACTION_NEW.equals(thingId)) {
				_jthing.put("id", id);
			} else if ( ! thingId.equals(id)) {
				throw new IllegalStateException("ID mismatch "+thingId+" vs "+id);
			}
		}
		
		JThing obj = AppUtils.doPublish(_jthing, draftPath, publishPath, forceRefresh, deleteDraft);
		return obj.setType(type);
	}

	protected JThing<T> doUnPublish(WebRequest state) {
		String id = getId(state);
		Log.d("crud", "doUnPublish "+id+" by "+state.getUserId()+" "+state);
		Utils.check4null(id); 
		// load (if not loaded)
		getThing(state);
		if (jthing==null) {
			jthing = getThingFromDB(state);
		}

		ESPath draftPath = esRouter.getPath(dataspace,type, id, KStatus.DRAFT);
		ESPath publishPath = esRouter.getPath(dataspace,type, id, KStatus.PUBLISHED);

		// set state to draft
		AppUtils.setStatus(jthing, KStatus.DRAFT);
		
		AppUtils.doSaveEdit(draftPath, jthing, state);
		Log.d("crud", "unpublish doSave "+draftPath+" by "+state.getUserId()+" "+state+" "+jthing.string());

		AppUtils.doDelete(publishPath);
		state.addMessage(id+" has been moved from published to draft");
		return jthing;
	}

	/**
	 * `new` gets turned into userid + nonce
	 * @param state
	 * @return 
	 */
	protected String getId(WebRequest state) {
		if (_id!=null) return _id;
		// Beware if ID can have a / in it!
		String[] slugBits = state.getSlugBits();
		
		String sid = slugBits[slugBits.length - 1]; 
		// NB: slug-bit-0 is the servlet, slug-bit-1 might be the ID - or the dataspace for e.g. SegmentServlet
		_id = getId2(state, sid);
		return _id;
	}

	protected String getId2(WebRequest state, String sid) {
		if (ACTION_NEW.equals(sid)) {
			String nicestart = StrUtils.toCanonical(
					Utils.or(state.getUserId(), state.get("name"), type.getSimpleName()).toString()
					).replace(' ', '_');
			sid = nicestart+"_"+Utils.getRandomString(8);
			// avoid ad, 'cos adblockers dont like it!
			if (sid.startsWith("ad")) {
				sid = sid.substring(2, sid.length());
			}
		}
		return sid;
	}


	protected void doStats(WebRequest state) {
		throw new WebEx.E404(state.getRequestUrl(), "_stats not available for "+type);
	}

	/**
	 * 
	 * @param state
	 * @return for debug purposes! The results are sent back in state
	 * @throws IOException
	 */
	public final List doList(WebRequest state) throws IOException {
		KStatus status = state.get(AppUtils.STATUS, KStatus.DRAFT);
		String q = state.get(Q);
		String prefix = state.get("prefix");
		String sort = state.get(SORT, defaultSort);		
		int size = state.get(SIZE, 1000);
		
		SearchResponse sr = doList2(q, prefix, status, sort, size, state);
		
		Map<String, Object> jobj = sr.getParsedJson();
		List<Map> hits = sr.getHits();

		// TODO dedupe can cause the total reported to be off
		List hits2 = doList3_source_dedupe(status, hits);
		
		// sanitise for privacy
		hits2 = cleanse(hits2, state);

		// HACK: send back csv?
		if (state.getResponseType() == KResponseType.csv) {
			doSendCsv(state, hits2);
			return hits2;
		}
			
		long total = sr.getTotal();
		String json = gson().toJson(
				new ArrayMap(
					"hits", hits2, 
					"total", total
				));
		JsonResponse output = new JsonResponse(state).setCargoJson(json);
		WebUtils2.sendJson(output, state);
		return hits2;
	}


	/**
	 * Do the search! 
	 * 
	 * Does NOT dedupe (eg multiple copies with diff status) or security cleanse.
	 * @param prefix 
	 * @param num 
	 */
	public final SearchResponse doList2(String q, String prefix, KStatus status, String sort, int size, WebRequest stateOrNull) {
		// copied from SoGive SearchServlet
		// TODO refactor to use makeESFilterFromSearchQuery
		SearchRequestBuilder s = new SearchRequestBuilder(es);
		/// which index? draft (which should include copies of published) by default
		switch(status) {
		case ALL_BAR_TRASH:
			s.setIndices(
					esRouter.getPath(dataspace, type, null, KStatus.PUBLISHED).index(),
					esRouter.getPath(dataspace, type, null, KStatus.DRAFT).index(),
					esRouter.getPath(dataspace, type, null, KStatus.ARCHIVED).index()
				);
			break;
		case PUB_OR_ARC:
			s.setIndices(
					esRouter.getPath(dataspace, type, null, KStatus.PUBLISHED).index(),
					esRouter.getPath(dataspace, type, null, KStatus.ARCHIVED).index()
				);
			break;
		default:
			// normal
			s.setIndex(esRouter.getPath(dataspace, type, null, status).index());
		}
		
		// query
		ESQueryBuilder qb = doList3_ESquery(q, prefix, stateOrNull);

		if (qb!=null) s.setQuery(qb);
				
		// Sort e.g. sort=date-desc for most recent first
		if (sort!=null) {			
			// HACK: order?
			KSortOrder order = KSortOrder.asc;
			if (sort.endsWith("-desc")) {
				sort = sort.substring(0, sort.length()-5);
				order = KSortOrder.desc;
			} else if (sort.endsWith("-asc")) {
				sort = sort.substring(0, sort.length()-4);
			}
			Sort _sort = new Sort().setField(sort).setOrder(order);			
			s.addSort(_sort);
		}
		
		// TODO paging!
		s.setSize(size);
		s.setDebug(true);

		// Call the DB
		SearchResponse sr = s.get();		
		return sr;
	}


	protected ESQueryBuilder doList3_ESquery(String q, String prefix, WebRequest stateOrNull) {
		ESQueryBuilder qb = null;
		if ( q != null) {
			// convert "me" to specific IDs
			if (Pattern.compile("\\bme\\b").matcher(q).find()) {
				if (stateOrNull==null) {
					throw new NullPointerException("`me` requires webstate to resolve who: "+q);
				}
				YouAgainClient ya = Dep.get(YouAgainClient.class);
				List<AuthToken> tokens = ya.getAuthTokens(stateOrNull);
				StringBuilder mes = new StringBuilder();
				for (AuthToken authToken : tokens) {
					mes.append(authToken.xid+" OR ");
				}
				if (mes.length()==0) {
					Log.w("crud", "No mes "+q+" "+stateOrNull);
					mes.append("ANON OR " ); // fail - WTF? How come no logins?!
				}
				StrUtils.pop(mes, 4);
				q = q.replaceAll("\\bme\\b", mes.toString());
			}
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
			if ( ! Utils.isBlank(q) && ! ALL.equalsIgnoreCase(q)) { // ??make all case-sensitive??
				SearchQuery sq = new SearchQuery(q);
				BoolQueryBuilder esq = AppUtils.makeESFilterFromSearchQuery(sq, null, null);
//				QueryStringQueryBuilder qsq = new QueryStringQueryBuilder(q); // QueryBuilders.queryStringQuery(q); // version incompatabilities in ES code :(			
				qb = ESQueryBuilders.must(qb, esq);
			}
		} // ./q
		if (prefix != null) {			
			// prefix is on a field -- we use name
			ESQueryBuilder qp = ESQueryBuilders.prefixQuery("name", prefix);
			qb = ESQueryBuilders.must(qb, qp);
		}
		
		// NB: exq can be null for ALL
		ESQueryBuilder exq = doList4_ESquery_custom(stateOrNull);
		qb = ESQueryBuilders.must(qb, exq);
		return qb;
	}


/**
 * 		// If user requests ALL_BAR_TRASH, they want to see draft versions of items which have been edited
		// So when de-duping, give priority to entries from .draft indices where the object is status: DRAFT

 * @param status
 * @param hits
 * @return unique hits, source
 */
	private List doList3_source_dedupe(KStatus status, List<Map> hits) {
		if (status != KStatus.ALL_BAR_TRASH && status!=KStatus.PUB_OR_ARC) {
			// One index = no deduping necessary.
			ArrayList<Object> hits2 = Containers.apply(hits, h -> h.get("_source"));
			return hits2;
		}
		List hits2 = new ArrayList<Map>();
		// de-dupe
//		KStatus preferredStatus = status==KStatus.ALL_BAR_TRASH? KStatus.DRAFT : KStatus.PUB_OR_ARC;
		List<Object> idOrder = new ArrayList<Object>(); // original ordering
		Map<Object, Object> things = new HashMap<Object, Object>(); // to hold "expected" version of each hit
		
		for (Map h : hits) {
			// pull out the actual object from the hit (NB: may be Map or AThing)
			Object hit = h.get("_source");
			if (hit == null) continue;
			Object id = getIdFromHit(hit);			
			// First time we've seen this object? Save it.
			if ( ! things.containsKey(id)) {
				idOrder.add(id);
				things.put(id, hit);
				continue;
			}
			// Which copy to keep?
			// Is this an object from .draft with non-published status? Overwrite the previous entry.
			Object index = h.get("_index");
			KStatus hitStatus = KStatus.valueOf(getStatus(hit));
			if (status==KStatus.ALL_BAR_TRASH) {
				// prefer draft
				if (index != null && index.toString().contains(".draft")) {
					things.put(id, hit);	
				}
			} else {
				// prefer published over archived
				if (KStatus.PUBLISHED == hitStatus) {
					things.put(id, hit);	
				}										
			}
		}
		// Put the deduped hits in the list in their original order.
		for (Object id : idOrder) {
			if (things.containsKey(id)) hits2.add(things.get(id));
		}
		return hits2;
	}


	/**
	 * convenience
	 * @return
	 */
	protected Gson gson() {
		return Dep.get(Gson.class);
	}



	/**
	 * Remove sensitive details for privacy - override to do anything!
	 * 
	 * This is (currently) only used with the _list endpoint!
	 * TODO expand to get-by-id requests too -- but carefully, as there's more risk of breaking stuff.
	 * 
	 * @param hits
	 * @param state
	 * @return hits
	 */
	protected List<Map> cleanse(List<Map> hits, WebRequest state) {
		return hits;
	}



	private String getStatus(Object h) {
		Object s;
		if (h instanceof Map) s = ((Map)h).get("status");
		else s = ((AThing)h).getStatus();
		return String.valueOf(s);
	}



	/**
	 * 
	 * @param hit Map from ES, or AThing
	 * @return
	 */
	private Object getIdFromHit(Object hit) {
		Object id;
		if (hit instanceof Map) id = ((Map)hit).get("id");
		else id = ((AThing)hit).getId();
		return id;
	}



	protected void doSendCsv(WebRequest state, List<Map> hits2) {
		StringWriter sout = new StringWriter();
		CSVWriter w = new CSVWriter(sout, new CSVSpec());
		
		Json2Csv j2c = new Json2Csv(w);		

		// send
		String csv = sout.toString();
		state.getResponse().setContentType(WebUtils.MIME_TYPE_CSV); // + utf8??
		WebUtils2.sendText(csv, state.getResponse());
	}
	




	/**
	 * Override to add custom filtering.
	 * @param state
	 * @return null or a query. This is ANDed to the normal query.
	 */
	protected ESQueryBuilder doList4_ESquery_custom(WebRequest state) {
		return null;
	}


	/**
	 * 
	 * NB: doPublish does NOT save first!
	 * 
	 * NB: Uses AppUtils#doSaveEdit2(ESPath, JThing, WebRequest, boolean) to do a *merge* into ES.
	 * So this will not remove parts of a document (unless you provide an over-write value).
	 * 
	 * Why use merge?
	 * This allows for partial editors (e.g. edit the budget of an advert), and reduces the collision
	 * issues with multiple online editors.
	 * 
	 * 
	 * @param state
	 */
	protected void doSave(WebRequest state) {		
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
		T pojo = jthing.java();
		 
		// add security?
		doSave2_setSecurity(state, pojo);
		
		{	// update
			String id = getId(state);
			assert id != null : "No id? cant save! "+state; 
			ESPath path = esRouter.getPath(dataspace,type, id, KStatus.DRAFT);
			AppUtils.doSaveEdit(path, jthing, state);
			Log.d("crud", "doSave "+path+" by "+state.getUserId()+" "+state+" "+jthing.string());
		}
	}
	

	/**
	 * Override to implement!
	 * @param state
	 * @param pojo
	 */
	protected void doSave2_setSecurity(WebRequest state, T pojo) {
		// TODO Auto-generated method stub		
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
