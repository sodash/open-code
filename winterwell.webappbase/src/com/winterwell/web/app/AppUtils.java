package com.winterwell.web.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.util.ajax.JSON;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import com.winterwell.data.AThing;
import com.winterwell.data.JThing;
import com.winterwell.data.KStatus;
import com.winterwell.data.PersonLite;
import com.winterwell.depot.IInit;
import com.winterwell.depot.merge.Diff;
import com.winterwell.depot.merge.Merger;
import com.winterwell.es.ESPath;
import com.winterwell.es.ESType;
import com.winterwell.es.IESRouter;
import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.GetRequestBuilder;
import com.winterwell.es.client.GetResponse;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.ReindexRequest;
import com.winterwell.es.client.UpdateRequestBuilder;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.PutMappingRequestBuilder;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.es.fail.ESException;
import com.winterwell.gson.Gson;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.EnumField;
import com.winterwell.web.fields.JsonField;


/**
 * Stuff used across the projects, mostly ES / CRUD stuff.
 * @author daniel
 *
 */
public class AppUtils {


	public static final JsonField ITEM = new JsonField("item");
	public static final EnumField<KStatus> STATUS = new EnumField<>(KStatus.class, "status");
	
	private static final List<String> LOCAL_MACHINES = Arrays.asList(
			"stross", "aardvark"
			);
	private static final List<String> TEST_MACHINES = Arrays.asList(
			"hugh", "mail.soda.sh"
			);
	private static final List<String> PROD_MACHINES = Arrays.asList(
			"heppner"
			);
	
	KServerType serverType = AppUtils.getServerType(null); 

	
	/**
	 * Use ConfigFactory to get a config from standard places
	 * @param config
	 * @param args
	 * @return
	 */
	public static <X> X getConfig(String appName, Class<X> config, String[] args) {
		ConfigFactory cf = ConfigFactory.get();
		X c = cf.getConfig(config);
		// set them for manifest
		ManifestServlet.addConfig(c);
		assert config != null;
		return c;		
	}
		
	
	/**
	 * Convenience for {@link #get(ESPath, Class)} using Dep.get(IESRouter)
	 * @param id
	 * @param klass
	 * @return
	 */
	public static <X> X get(String id, Class<X> klass) {
		ESPath path = Dep.get(IESRouter.class).getPath(klass, id, KStatus.PUBLISHED);
		return get(path, klass);
	}
	
	/**
	 * Will try path.indices in order if multiple
	 * @param path
	 * @return
	 */
	public static <X> X get(ESPath path, Class<X> klass) {
		return get(path, klass, null);
	}
	
	public static <X> X get(ESPath path, Class<X> klass, AtomicLong version) {
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));

		GetRequestBuilder s = new GetRequestBuilder(client);
		// Minor TODO both indices in one call
		s.setIndices(path.indices[0]).setType(path.type).setId(path.id);
		if (version==null) s.setSourceOnly(true);
//		s.setDebug(true);
		GetResponse sr = s.get();
		if (sr.isSuccess()) {
			if (klass!=null) {
				Gson gson = Dep.get(Gson.class);
				String json = sr.getSourceAsString();
				X item = gson.fromJson(json, klass);
				// version?
				if (version!=null) {
					Long v = sr.getVersion();
					version.set(v);
				}
				return item;
			}
			Map<String, Object> json = sr.getSourceAsMap(); //SourceAsString();
			return (X) json;
		}
		Exception error = sr.getError();
		if (error!=null) {
			if (error instanceof WebEx.E404) {
				// was version=draft?
				if (path.indices.length > 1) {
					ESPath path2 = new ESPath(Arrays.copyOfRange(path.indices, 1, path.indices.length), path.type, path.id);
					return get(path2, klass);
				}
				// 404
				return null;
			}
			throw Utils.runtime(error);
		}
		return null;
	}
	
	public static JThing doUnPublish(JThing thing, ESPath draftPath, ESPath pubPath, KStatus newStatus) {
		Log.d("unpublish", draftPath+" "+pubPath+" "+newStatus);
		// prefer being given the thing to avoid ES race conditions
		if (thing==null) {
			Map<String, Object> draftMap = get(pubPath, null);
			thing = new JThing().setMap(draftMap);
		}
		assert thing != null : draftPath;
		// remove modified flag
		if (thing.map().containsKey("modified")) {
			thing.put("modified", false);
		}
		// set status
		thing.put("status", newStatus);
		// update draft // TODO just an update script to set status
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		UpdateRequestBuilder up = client.prepareUpdate(draftPath);
		up.setDoc(thing.map());
		up.setDocAsUpsert(true);
		// NB: this doesn't return the merged item :(
		IESResponse resp = up.get().check();
		
		// delete the published version	
		if ( ! draftPath.equals(pubPath)) {
			Log.d("unpublish", "deleting published version "+pubPath);
			DeleteRequestBuilder del = client.prepareDelete(pubPath.index(), pubPath.type, pubPath.id);
			IESResponse ok = del.get().check();		
		}
		
		return thing;
	}
	

	public static JThing doPublish(JThing draft, ESPath draftPath, ESPath publishPath) {
		return doPublish(draft, draftPath, publishPath, false, false);
	}
	
	public static JThing doPublish(AThing item, boolean forceRefresh, boolean deleteDraft) {		
		IESRouter esr = Dep.get(IESRouter.class);
		Class type = item.getClass();
		String id = item.getId();		
		ESPath draftPath = esr.getPath(type, id, KStatus.DRAFT);
		ESPath publishPath = esr.getPath(type, id, KStatus.PUBLISHED);
		JThing draft = new JThing(item);
		return doPublish(draft, draftPath, publishPath, forceRefresh, deleteDraft);
	}
	
	/**
	 * 
	 * @param draft
	 * @param draftPath
	 * @param publishPath
	 * @param forceRefresh
	 * @param deleteDraft Normally we leave the draft, for future editing. But if the object is not editable once published - delete the draft.
	 * @return
	 */
	public static JThing doPublish(JThing draft, ESPath draftPath, ESPath publishPath, boolean forceRefresh, boolean deleteDraft) 
	{
		Log.d("doPublish", "to "+publishPath+"... deleteDraft "+deleteDraft);
		// prefer being given the draft to avoid ES race conditions
		if (draft==null) {
			Map<String, Object> draftMap = get(draftPath, null);
			draft = new JThing().setMap(draftMap);
		}
		assert draft != null : draftPath;
		// remove modified flag
		if (draft.map().containsKey("modified")) {
			draft.put("modified", false);
		}
		// set status
		draft.put("status", KStatus.PUBLISHED);
		// publish
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		UpdateRequestBuilder up = client.prepareUpdate(publishPath);
		up.setDoc(draft.map());
		if (forceRefresh) up.setRefresh("true");
		up.setDocAsUpsert(true);
		// NB: this doesn't return the merged item :(
		IESResponse resp = up.get().check();
		
		// Also update draft?
		Log.d("doPublish", publishPath+" deleteDraft: "+deleteDraft);
		if ( ! draftPath.equals(publishPath)) {
			if (deleteDraft) {
				doDelete(draftPath);
			} else {
				Log.d("doPublish", "also update draft "+draftPath);
				UpdateRequestBuilder upd = client.prepareUpdate(draftPath);
				upd.setDoc(draft.map());
				upd.setDocAsUpsert(true);
				if (forceRefresh) upd.setRefresh("true");
				IESResponse respd = upd.get().check();
			}
		}

		return draft;
	}
	
	
	public static void doDelete(ESPath path) {
		try {
			Log.d("delete", path+" possible-state:"+WebRequest.getCurrent());
			ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
			DeleteRequestBuilder del = client.prepareDelete(path.index(), path.type, path.id);
			IESResponse ok = del.get().check();
		} catch(WebEx.E404 ex) {
			// oh well
			Log.d("delete", path+" 404 - already deleted?");
		}
	}

	public static JThing doSaveEdit(ESPath path, JThing item, WebRequest state) {
		assert path.index().toLowerCase().contains("draft") : path;		
		// TODO check security with YouAgain!		
		// update status TODO factor out the status logic
		Object s = item.map().get("status");
		if (Utils.streq(s, KStatus.PUBLISHED)) {
			item.put("status", KStatus.MODIFIED);
		} else {
			item.put("status", KStatus.DRAFT);
		}		
		// talk to ES
		return doSaveEdit2(path, item, state);
	}
	
	/**
	 * skips the status bit in {@link #doSaveEdit(ESPath, JThing, WebRequest)}
	 * @param path
	 * @param item
	 * @param stateCanBeNull
	 * @return
	 */
	@SuppressWarnings("unused")
	public static JThing doSaveEdit2(ESPath path, JThing item, WebRequest stateCanBeNull) {
		return doSaveEdit2(path, item, stateCanBeNull, false);
	}
	public static JThing doSaveEdit2(ESPath path, JThing item, WebRequest stateCanBeNull, boolean instant) {
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));		
		// save update
		
		JThing item2 = Utils.copy(item);
		String json = item2.string();
		Object start = SimpleJson.get(item2.map(), "projects", 0, "start");
		Object startraw = SimpleJson.get(item2.map(), "projects", 0, "start_raw");
		
		// prep object via IInit? (IInit is checked within JThing)
		// e.g. set the suggest field for NGO 
		Object jobj = item.java();

		item2 = Utils.copy(item);
		String json2 = item2.string();
		Object start2 = SimpleJson.get(item2.map(), "projects", 0, "start");
		Object startraw2 = SimpleJson.get(item2.map(), "projects", 0, "start_raw");
		
		// sanity check id matches path
		String id = (String) item.map().get("@id"); //mod.getId();
		if (id==null) {
			Object _id = item.map().get("id");
			if (_id instanceof String) id= (String) _id;
			if (_id.getClass().isArray()) id= (String) Containers.asList(_id).get(0);
		}
		assert id != null && ! id.equals("new") : "use action=new "+stateCanBeNull;
		assert id.equals(path.id) : path+" vs "+id;
		
		item2 = Utils.copy(item);
		String json3 = item2.string();
		Object start3 = SimpleJson.get(item2.map(), "projects", 0, "start");
		Object startraw3 = SimpleJson.get(item2.map(), "projects", 0, "start_raw");
		
		// save to ES
		UpdateRequestBuilder up = client.prepareUpdate(path);
		// This should merge against what's in the DB
		Map map = item.map();
		up.setDoc(map);
		up.setDocAsUpsert(true);
		// force an instant refresh?
		if (instant) up.setRefresh("true");
		// TODO delete stuff?? fields or items from a list
//		up.setScript(script)
		// NB: this doesn't return the merged item :(
		IESResponse resp = up.get().check();
//		Map<String, Object> item2 = resp.getParsedJson();
		
		return item;
	}	

	/**
	 * local / test / production
	 */
	public static KServerType getServerType(WebRequest state) {
		if (state != null && false) {			
			KServerType st = KServerType.PRODUCTION;
			String url = state.getRequestUrl();				
			if (url.contains("//local")) st = KServerType.LOCAL;
			if (url.contains("//test")) st = KServerType.TEST;			
			Log.d("AppUtils", "Using WebRequest serverType "+st+" from url "+url);
			return st;
		}
		// cache the answer
		if (_serverType==null) {
			_serverType = getServerType2();
			Log.d("AppUtils", "Using serverType "+_serverType);
		}
		return _serverType;
	}		
	
	private static KServerType _serverType;
	private static String _hostname;

	/**
	 * Determined in this order:
	 *
	 * 1. Is there a config rule "serverType=dev|production" in Statics.properties?
	 * (i.e. loaded from a server.properties file)
	 * 2. Is the hostname in the hardcoded PRODUCTION_ and DEV_MACHINES lists?
	 *
	 * @return
	 */
	private static KServerType getServerType2() {
		// explicit config
		if (Dep.has(Properties.class)) {
			String st = Dep.get(Properties.class).getProperty("serverType");
			if (st!=null) {
				Log.d("init", "Using explicit serverType "+st);			
				return KServerType.valueOf(st);
			} else {
				Log.d("init", "No explicit serverType in config");
			}
		} else {
			Log.d("init", "No Properties for explicit serverType");
		}
		// explicitly listed
		String hostname = getFullHostname();
		Log.d("init", "serverType for host "+hostname+" ...?");
		if (LOCAL_MACHINES.contains(hostname)) {
			Log.i("init", "Treating "+hostname+" as serverType = "+KServerType.LOCAL);
			return KServerType.LOCAL;
		}
		if (TEST_MACHINES.contains(hostname)) {
			Log.i("init", "Treating "+hostname+" as serverType = "+KServerType.TEST);
			return KServerType.TEST;
		}
		if (PROD_MACHINES.contains(hostname)) {
			Log.i("init", "Treating "+hostname+" as serverType = "+KServerType.PRODUCTION);
			return KServerType.PRODUCTION;
		}

		Log.i("init", "Fallback: Treating "+hostname+" as serverType = "+KServerType.PRODUCTION);
		return KServerType.PRODUCTION;
	}


	public static String getFullHostname() {
		if (_hostname==null) _hostname = WebUtils.fullHostname();
		return _hostname;
	}


	public static void addDebugInfo(WebRequest request) {
		request.getResponse().addHeader("X-Server", AppUtils.getFullHostname());
	}


	/**
	 * Make indices.
	 * Does not set mapping.
	 * @param main
	 * @param dbclasses
	 */
	public static void initESIndices(KStatus[] main, Class[] dbclasses) {
		IESRouter esRouter = Dep.get(IESRouter.class);
		ESHttpClient es = Dep.get(ESHttpClient.class);
		ESException err = null;
		for(KStatus s : main) {
			for(Class klass : dbclasses) {
				ESPath path = esRouter.getPath(null, klass, null, s);
				String index = path.index();
				if (es.admin().indices().indexExists(index)) {
					continue;
				}
				try {
					// make with an alias to allow for later switching if we change the schema
					String baseIndex = index+"_"+es.getConfig().getIndexAliasVersion();
					CreateIndexRequest pi = es.admin().indices().prepareCreate(baseIndex);
					pi.setFailIfAliasExists(true);
					pi.setAlias(index);
					IESResponse r = pi.get();
				} catch(ESException ex) {
					Log.e("ES.init", ex.toString());
					err = ex;
				}
			}
		}
		if (err!=null) throw err;
	}


	/**
	 * Create mappings. Some common fields are set: "name", "id", "@type"
	 * @param statuses
	 * @param dbclasses
	 * @param mappingFromClass Setup more fields. Can be null
	 */
	public static void initESMappings(KStatus[] statuses, Class[] dbclasses, Map<Class,Map> mappingFromClass) {
		IESRouter esRouter = Dep.get(IESRouter.class);
		ESHttpClient es = Dep.get(ESHttpClient.class);
		ESException err = null;
		for(KStatus status : statuses) {			
			for(Class k : dbclasses) {
				ESPath path = esRouter.getPath(null, k, null, status);
				try {
					// Normal setup
					String index = path.index();
					initESMappings2_putMapping(mappingFromClass, es, k, path, index);
					
				} catch(ESException ex) {
					// map the base index (so we can do a reindex with the right mapping)
					String index = path.index()
							+"_"+Dep.get(ESConfig.class).getIndexAliasVersion()
							;
					// make if not exists
					if ( ! es.admin().indices().indexExists(index)) {
						CreateIndexRequest pi = es.admin().indices().prepareCreate(index);
//						pi.setFailIfAliasExists(true);
//						pi.setAlias(path.index());
						IESResponse r = pi.get().check();
					}
					// setup the right mapping
					initESMappings2_putMapping(mappingFromClass, es, k, path, index);
					// attempt a simple reindex
					ReindexRequest rr = new ReindexRequest(es, path.index(), index);
					rr.execute(); // could be slow, so don't wait
					// and shout fail!
					//  -- but run through all the mappings first, so a sys-admin can update them all in one run.
					// c.f. https://issues.soda.sh/stream?tag=35538&as=su
					err = ex;
					Log.e("init", ex.toString());
				}
			}
		}
		if (err != null) throw err;
	}

	private static void initESMappings2_putMapping(Map<Class, Map> mappingFromClass, ESHttpClient es, Class k,
			ESPath path, String index) 
	{
		PutMappingRequestBuilder pm = es.admin().indices().preparePutMapping(
				index, path.type);
		ESType dtype = new ESType();
		// passed in
		Map mapping = mappingFromClass==null? null : mappingFromClass.get(k);
		if (mapping != null) {
			// merge in
			// NB: done here, so that it doesn't accidentally trash the settings below
			// -- because probably both maps define "properties"
			// Future: It'd be nice to have a deep merge, and give the passed in mapping precendent.
			dtype.putAll(mapping);
		}

		// some common props
		dtype.property("name", new ESType().text()
								// enable keyword based sorting
								.field("raw", "keyword"));
		// ID, either thing.org or sane version
		dtype.property("@id", new ESType().keyword());
		dtype.property("id", new ESType().keyword());
		// type
		dtype.property("@type", new ESType().keyword());
		
		
		pm.setMapping(dtype);
		IESResponse r2 = pm.get();
		r2.check();
	}

	/**
	 * 
	 * @param from
	 * @param info Optional {name, img, description, url}
	 * @return
	 */
	public static PersonLite getCreatePersonLite(XId from, Map info) {
		// it is strongly recommended that the router treat PersonLite == Person 
		IESRouter router = Dep.get(IESRouter.class);
		ESPath path = router.getPath(PersonLite.class, from.toString(), KStatus.PUBLISHED);
		PersonLite peep = get(path, PersonLite.class);
		if (peep!=null) {
			// not saving any edits here?!
			if (info != null) peep.setInfo(info);
			return peep;
		}
		// draft?
		path = router.getPath(PersonLite.class, from.toString(), KStatus.DRAFT);
		peep = get(path, PersonLite.class);		
		if (peep!=null) {
			// not saving any edits here?!
			if (info != null) peep.setInfo(info);
			return peep;
		}
		// make it		
		peep = new PersonLite(from);
		if (info != null) peep.setInfo(info);
		// store it NB: the only data is the id, so there's no issue with race conditions
		AppUtils.doSaveEdit(path, new JThing().setJava(peep), null);
		return peep;
	}


	/**
	 * 
	 * @param sq never null
	 * @param start
	 * @param end
	 * @return
	 */
	public static BoolQueryBuilder makeESFilterFromSearchQuery(SearchQuery sq, Time start, Time end) {
		assert sq != null;
		
		BoolQueryBuilder filter = QueryBuilders.boolQuery();
		
		if (start != null || end != null) {
			RangeQueryBuilder timeFilter = QueryBuilders.rangeQuery("time");
			if (start!=null) timeFilter = timeFilter.from(start.toISOString()); //, true) ES versioning pain
			if (end!=null) timeFilter = timeFilter.to(end.toISOString()); //, true);
			filter = filter.must(timeFilter);
		}
		
		// filters TODO a true recursive SearchQuery -> ES query mapping
		// TODO this is just a crude 1-level thing
		List ptree = sq.getParseTree();
		try {
			filter = filter.must(parseTreeToQuery(ptree));
		} catch (AssertionError e) {
			// Put full query info on an assertion failure
			assert (false) : e.getMessage() + " from " + sq;
		}
		
		return filter;
	}
	
	
	private static BoolQueryBuilder parseTreeToQuery(Object rawClause) {
		BoolQueryBuilder filter = QueryBuilders.boolQuery();
		
		// Map means propname=value constraint.
		if (rawClause instanceof Map) {
			Map<String, Object> clause = (Map<String, Object>) rawClause;
			// We expect only one pair per clause, but no reason not to tolerate multiples.
			for (String prop : clause.keySet()) {
				String val = (String) clause.get(prop);
				if (ESQueryBuilders.UNSET.equals(val)) {
					QueryBuilder setFilter = QueryBuilders.existsQuery(prop);
					return filter.mustNot(setFilter);
				} else {
					// normal key=value case
					QueryBuilder kvFilter = QueryBuilders.termQuery(prop, val);
					return filter.must(kvFilter);
				}	
			}
		}
	
		assert (rawClause instanceof List) : "clause is not list or map: " + rawClause;
		
		List clause = (List) rawClause;
		assert (! clause.isEmpty()) : "empty clause";
		
		// Only one element?
		if (clause.size() < 2) {
			Object entry = clause.get(0);
			// empty query string yields degenerate parse tree with just ["and"]
			if (entry instanceof String && SearchQuery.KEYWORD_AND.equalsIgnoreCase((String) entry)) {
				return filter;
			}
			// Well, try and parse it.
			return parseTreeToQuery(entry);
		}
		
		// 2+ elements, first is a String - is it a Boolean operator?
		Object maybeOperator = clause.get(0);
		if (maybeOperator instanceof String) {
			// Is it an explicit NOT clause, ie (NOT, x=y)?
			if (SearchQuery.KEYWORD_NOT.equals((String) maybeOperator)) {
				assert (clause.size() == 2) : "Explicit NOT clause with >1 operand??: " + clause;
				return filter.mustNot(parseTreeToQuery(clause.get(1)));
			}
			
			if (SearchQuery.KEYWORD_AND.equals((String) maybeOperator)) {
				for (Object term : clause.subList(1, clause.size())) {
					filter = filter.must(parseTreeToQuery(term));
				}
				return filter;
			}
			
			if (SearchQuery.KEYWORD_OR.equals((String) maybeOperator)) {
				for (Object term : clause.subList(1, clause.size())) {
					filter = filter.should(parseTreeToQuery(term));
				}
				return filter;
			}
		}
		
		// Fall-through clause: 2+ elements, first isn't a Boolean operator
		// Assume it's an implicit AND of all elements in list.
		for (Object term : clause) {
			filter = filter.must(parseTreeToQuery((List) term));
		}
		return filter;
	}


	@Deprecated
	public static <X> X getConfig(String appName, X config, String[] args) {
		return (X) getConfig(appName, config.getClass(), args);
	}


	
}
