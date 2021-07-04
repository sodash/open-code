package com.winterwell.web.app;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import com.winterwell.bob.wwjobs.BuildHacks;
import com.winterwell.data.AThing;
import com.winterwell.data.KStatus;
import com.winterwell.data.PersonLite;
import com.winterwell.es.ESKeyword;
import com.winterwell.es.ESNoIndex;
import com.winterwell.es.ESPath;
import com.winterwell.es.ESType;
import com.winterwell.es.IESRouter;
import com.winterwell.es.client.DeleteRequest;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.GetRequest;
import com.winterwell.es.client.GetResponse;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.KRefresh;
import com.winterwell.es.client.ReindexRequest;
import com.winterwell.es.client.SearchRequest;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.UpdateRequest;
import com.winterwell.es.client.admin.ClusterAdminClient;
import com.winterwell.es.client.admin.ClusterOverridReadOnlyRequest;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.IndicesAliasesRequest;
import com.winterwell.es.client.admin.PutMappingRequest;
import com.winterwell.es.client.query.BoolQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.es.fail.ESException;
import com.winterwell.es.fail.ESIndexReadOnlyException;
import com.winterwell.gson.Gson;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.AString;
import com.winterwell.utils.Dep;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.EnumField;
import com.winterwell.web.fields.JsonField;
import com.winterwell.youagain.client.App2AppAuthClient;
import com.winterwell.youagain.client.AuthToken;
import com.winterwell.youagain.client.ShareToken;
import com.winterwell.youagain.client.YouAgainClient;


/**
 * Stuff used across the projects, mostly ES / CRUD stuff.
 * @author daniel
 *
 */
public class AppUtils {

	public static SearchResponse search(ESPath path, SearchQuery q) {
		ESHttpClient esjc = Dep.get(ESHttpClient.class);
		SearchRequest s = new SearchRequest(esjc);
		s.setPath(path);
		com.winterwell.es.client.query.BoolQueryBuilder f = makeESFilterFromSearchQuery(q, null, null);
		s.setQuery(f);
		s.setDebug(DEBUG);
		SearchResponse sr = s.get();
		return sr;
	}
	

	public static final JsonField ITEM = new JsonField("item");
	public static final EnumField<KStatus> STATUS = new EnumField<>(KStatus.class, "status");
	
	
	public static boolean DEBUG = true;
	
	KServerType serverType = AppUtils.getServerType(null); 

	
	/**
	 * Use ConfigFactory to get a config from standard places. 
	 * This is for loading configs during initialisation.
	 * It also calls Dep.set()
	 * @param config
	 * @param args
	 * @return
	 */
	public static <X> X getConfig(String appName, Class<X> config, String[] args) {
		ConfigFactory cf = ConfigFactory.get();
		if (args!=null) {
			cf.setArgs(args);
		}
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
	 * @return object or null for 404
	 */
	public static <X> X get(ESPath path, Class<X> klass) {
		return get(path, klass, null);
	}
	
	/**
	 * 
	 * @param path
	 * @param klass
	 * @param version
	 * @return object or null for 404
	 */
	public static <X> X get(ESPath path, Class<X> klass, AtomicLong version) {
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		GetRequest s = new GetRequest(client);
		return get2(path, klass, version, s);
	}
	
	public static <X> X get2(ESPath path, Class<X> klass, AtomicLong version, GetRequest s) {
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
		// set state to draft (NB: archived is handled by an action=archive flag)
		thing = setStatus(thing, newStatus);

		// update draft // TODO just an update script to set status
		Log.d("crud", "unpublish "+newStatus+" doSave "+draftPath);
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		UpdateRequest up = client.prepareUpdate(draftPath);
		up.setDoc(thing.map());
		up.setDocAsUpsert(true);
		// NB: this doesn't return the merged item :(
		IESResponse resp = up.get().check();
		
		// delete the published version	
		if ( ! draftPath.equals(pubPath)) {
			AppUtils.doDelete(pubPath);
		}
		// done
		return thing;
	}

	public static JThing doPublish(JThing draft, ESPath draftPath, ESPath publishPath) {
		return doPublish(draft, draftPath, publishPath, KRefresh.FALSE, false);
	}
	
	public static JThing doPublish(AThing item, KRefresh refresh, boolean deleteDraft) {		
		IESRouter esr = Dep.get(IESRouter.class);
		Class type = item.getClass();
		String id = item.getId();		
		ESPath draftPath = esr.getPath(type, id, KStatus.DRAFT);
		ESPath publishPath = esr.getPath(type, id, KStatus.PUBLISHED);
		JThing draft = new JThing(item);
		return doPublish(draft, draftPath, publishPath, refresh, deleteDraft);
	}
	
	/**
	 * 
	 * @param draft
	 * @param draftPath Can be null
	 * @param publishPath
	 * @param forceRefresh true - use refresh=true to make the index update now
	 * @param deleteDraft Normally we leave the draft, for future editing. But if the object is not editable once published - delete the draft.
	 * @return
	 */
	public static JThing doPublish(
			JThing draft, ESPath draftPath, ESPath publishPath, 
			KRefresh refresh, boolean deleteDraft) 
	{
		return doPublish(draft, draftPath, publishPath, null, refresh, deleteDraft);
	}
	
	// TODO refactor to consume IESRouter
	public static JThing doPublish(
			JThing draft, ESPath draftPath, ESPath publishPath, ESPath archivePath,
			KRefresh refresh, boolean deleteDraft) 
	{
		Log.d("doPublish", "to "+publishPath+"... deleteDraft "+deleteDraft);
		// prefer being given the draft to avoid ES race conditions
		if (draft==null) {
			assert draftPath != null : "no draft or draftpath! "+publishPath;
			Map<String, Object> draftMap = get(draftPath, null);
			draft = new JThing().setMap(draftMap);
		}
		assert draft != null : draftPath;
		// set status
		draft = setStatus(draft, KStatus.PUBLISHED);
		// publish
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		UpdateRequest up = client.prepareUpdate(publishPath);
		up.setDoc(draft.map());
		up.setRefresh(refresh);		
		up.setDocAsUpsert(true);
		// NB: this doesn't return the merged item :(
		IESResponse resp = up.get().check();
		
		// Also update draft?
		Log.d("doPublish", publishPath+" deleteDraft: "+deleteDraft);
		if (draftPath!=null && ! draftPath.equals(publishPath)) {
			if (deleteDraft) {
				doDelete(draftPath);
			} else {
				Log.d("doPublish", "also update draft "+draftPath);
				UpdateRequest upd = client.prepareUpdate(draftPath);
				upd.setDoc(draft.map());
				upd.setDocAsUpsert(true);
				upd.setRefresh(refresh);
				IESResponse respd = upd.get().check();
			}
		}
		// Delete any archived copies
		if (archivePath !=null && ! archivePath.equals(publishPath)) {
			AppUtils.doDelete(archivePath);
		}			

		return draft;
	}
	
	
	public static void doDelete(ESPath path) {
		try {
			Log.d("delete", path+" possible-state:"+WebRequest.getCurrent());
			ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
			DeleteRequest del = client.prepareDelete(path.index(), path.type, path.id);
			IESResponse ok = del.get().check();
		} catch(WebEx.E404 ex) {
			// oh well
			Log.d("delete", path+" 404 - already deleted?");
		}
	}

	/**
	 * Save edits to *draft*. Modifies status.
	 * @param path
	 * @param item
	 * @param state Can be null
	 * @return
	 */
	public static JThing doSaveEdit(ESPath path, JThing item, WebRequest state) {
		// NB: Most classes have a draft phase -- but not Task.java
		// assert path.index().toLowerCase().contains("draft") : path;
		
		// TODO check security with YouAgain!		
		
		// update status TODO factor out the status logic
		Object s = item.map().get("status");
		if (Utils.streq(s, KStatus.PUBLISHED)) {
			AppUtils.setStatus(item, KStatus.MODIFIED);
		} else if (!Utils.streq(s, KStatus.ARCHIVED)) {
			AppUtils.setStatus(item, KStatus.DRAFT);
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
		assert path.id != null : "need an id in path to save "+item;
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));		
		// save update
		
		// prep object via IInit? (IInit is checked within JThing)
		// e.g. set the suggest field for NGO 
		Object jobj = item.java();
		
		// sanity check id matches path
		String id = (String) item.map().get("@id"); //mod.getId();
		if (id==null) {
			Object _id = item.map().get("id");
			if (_id instanceof String) id= (String) _id;
			if (_id.getClass().isArray()) id= (String) Containers.asList(_id).get(0);
		}
		assert id != null && ! id.equals("new") : "use action=new "+stateCanBeNull;
		assert id.equals(path.id) : path+" vs "+id;
				
		// save to ES
		UpdateRequest up = client.prepareUpdate(path);
		if (DEBUG) up.setDebug(DEBUG); // NB: only set if its extra debugging??
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
	 * @deprecated replace with {@link BuildHacks#getServerType()}
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
		return getServerType();
	}
	
	
	/**
	 * @deprecated replace with {@link BuildHacks#getServerType()}
	 * local / test / production
	 */
	public static KServerType getServerType() {		
		return BuildHacks.getServerType();
	}
	
	
	private static KServerType _serverType;
	private static String _hostname;



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
	public static void initESIndices(KStatus[] main, Class... dbclasses) {
		initESIndices(main, null, dbclasses);
	}
	
	public static void initESIndices(KStatus[] main, CharSequence dataspace, Class... dbclasses) {
		IESRouter esRouter = Dep.get(IESRouter.class);
		ESHttpClient es = Dep.get(ESHttpClient.class);
		
		// if a class fails -- do the others, then report the failure
		ESException err = null;
		
		for(KStatus s : main) {
			for(Class klass : dbclasses) {
				try {
					initESindex2(esRouter, es, s, klass, dataspace);
				} catch (ESException ex) {
					Log.w("init.ES", ex);
					if (err==null) err = ex;					
				}
			}
		}
		if (err!=null) throw err;
	}


	private static void initESindex2(
			IESRouter esRouter, ESHttpClient es, KStatus s,
			Class klass, CharSequence dataspace) 
	{		
		ESPath path = esRouter.getPath(dataspace, klass, null, s);
		String index = path.index();
		if (es.admin().indices().indexExists(index)) {
			return;
		}
		Log.d("ES.init", "init index for "+klass+"...");			
		// make with an alias to allow for later switching if we change the schema
		String baseIndex = index+"_"+es.getConfig().getIndexAliasVersion();
		// what if base-index wo alias??
		if (es.admin().indices().indexExists(baseIndex)) {
			Log.d("ES.init", "Base index "+baseIndex+" exists but not the alias "+index+" - Let's link them...");
			IndicesAliasesRequest alias = es.admin().indices().prepareAliases();
			alias.addAlias(baseIndex, index);
			alias.setDebug(true);
			alias.get().check();
		} else {
			// make a new index
			CreateIndexRequest pi = es.admin().indices().prepareCreate(baseIndex);
			pi.setDebug(true);
			pi.setFailIfAliasExists(true);
			pi.setAlias(index);
			IESResponse r = pi.get().check();
		}
	}

	public static void initESMappings(KStatus[] statuses, Class[] dbclasses, Map<Class,Map> mappingFromClass) 
	{
		try {
			initESMappings(statuses, dbclasses, mappingFromClass, null);
		} catch (ESIndexReadOnlyException ex) {
			// ES being fussy about memory -- if local, poke the index
			if (AppUtils.getServerType() != KServerType.LOCAL) throw ex;
			ESHttpClient esjc = Dep.get(ESHttpClient.class);
			ClusterAdminClient cac = new ClusterAdminClient(esjc);
			ClusterOverridReadOnlyRequest oro = cac.prepareOverrideReadOnly();
			oro.setDebug(true);
			IESResponse ok = oro.get().check();
			// try again
			initESMappings(statuses, dbclasses, mappingFromClass, null);
		}
	}

	/**
	 * Create mappings. Some common fields are set: "name", "id", "@type"
	 * @param statuses
	 * @param dbclasses
	 * @param mappingFromClass Setup more fields. Can be null
	 * @param dataspace 
	 */
	public static void initESMappings(KStatus[] statuses, Class[] dbclasses, 
			Map<Class,Map> mappingFromClass, CharSequence dataspace) 
	{
		IESRouter esRouter = Dep.get(IESRouter.class);
		ESHttpClient es = Dep.get(ESHttpClient.class);
		String errMsg = null;			
		for(Class k : dbclasses) {
			for(KStatus status : statuses) {
				ESPath path = esRouter.getPath(dataspace, k, null, status);
				try {					
					// Normal setup
					String index = path.index();
					initESMappings2_putMapping(mappingFromClass, es, k, path, index);
				} catch(Exception ex) {
					// collect all the error messages
					String exMsg = initESMappings2_error(mappingFromClass, es, k, path, ex);
					errMsg = errMsg==null? exMsg : errMsg+"\n\n"+exMsg;
				}
			}
		}		
		// shout if we had an error
		if (errMsg != null) {	
			// ??IF we add auto reindex, then wait for ES
//			es.flush();
			throw new ESException(errMsg);
		}
	}

	/**
	 * Probably a mapping change. 
	 * @param mappingFromClass
	 * @param es
	 * @param k
	 * @param path
	 * @param ex
	 * @return
	 */
	private static String initESMappings2_error(
			Map<Class, Map> mappingFromClass, ESHttpClient es, Class k,
			ESPath path, final Exception ex) 
	{
		String msg = path.index()+" Mapping change?! "+ex;
		Log.w("ES.init", path.index()+" Mapping change?! "+ex);
		// map the base index (so we can do a reindex with the right mapping)
		// NB: The default naming, {index}_{month}{year}, assumes we only do one mapping change per month.
		ESConfig esConfig = Dep.get(ESConfig.class);
		String index = path.index()+"_"+esConfig.getIndexAliasVersion();
		// make if not exists (which it shouldn't)
		if ( ! es.admin().indices().indexExists(index)) {
			CreateIndexRequest pi = es.admin().indices().prepareCreate(index);
			// NB: no alias yet - the old version is still in place
			IESResponse r = pi.get().check();
		} else {
			// the index exists! use a temp index (otherwise the put below will fail like the one above)
			index = index+".fix";
			if ( ! es.admin().indices().indexExists(index)) {
				CreateIndexRequest pi = es.admin().indices().prepareCreate(index);
				// NB: no alias yet - the old version is still in place
				IESResponse r = pi.get().check();
			}
		}
		// setup the right mapping
		initESMappings2_putMapping(mappingFromClass, es, k, path, index);
		// attempt a simple reindex?
		// No - cos a gap would open between the data in the two versions. We have to reindex and switch as instantaneously as we can.
//					ReindexRequest rr = new ReindexRequest(es, path.index(), index);
		if (AppUtils.getServerType() == KServerType.LOCAL) {
			Log.i("ES.init", "LOCAL - So trying to reindex now");
//								"curl -XPOST http://localhost:9200/_reindex -d '{\"source\":{\"index\":\""+path.index()+"\"},\"dest\":{\"index\":\""+index+"\"}}'\n");
			ReindexRequest rr = new ReindexRequest(es, path.index(), index);
			rr.setDebug(true);
			IESResponse resp = rr.get();
			if (resp.isSuccess()) Log.d(resp); else Log.e("ES.init.reindex.fail", resp);
		} else {
			// dont auto reindex test or live
			String reindexMsg = "To reindex:\n"+
					"curl -XPOST http://localhost:9200/_reindex -d '{\"source\":{\"index\":\""+path.index()+"\"},\"dest\":{\"index\":\""+index+"\"}}' -H 'Content-Type:application/json'\n";
			Log.i("ES.init", reindexMsg);
			msg += "\n"+reindexMsg;
		}
		// and shout fail!
		//  -- but run through all the mappings first, so a sys-admin can update them all in one run.
		// c.f. https://issues.soda.sh/stream?tag=35538&as=su
		
		// After this, the sysadmin should (probably) remove the link old-base -> alias, 
		// and put in a new-base -> alias link
		
		// To see mappings:
		Log.i("ES.init", "\n\nTo see mappings:\n"
				+" curl http://localhost:9200/_cat/aliases/"+k.getSimpleName().toLowerCase()+"*\n"
				+" curl http://localhost:9200/_cat/indices/"+k.getSimpleName().toLowerCase()+"*\n"
				);
		
		// Switch Info
		// https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-aliases.html
		String alias = path.index();					
		String OLD = "OLD";
		try {
			List<String> oldIndexes = es.admin().indices().getAliasesResponse(alias);
			OLD = "'"+oldIndexes.get(0)+"'";
		} catch(Exception aex) {
			// oh well
		}

		// Switch aliases
		// do it if local
		IndicesAliasesRequest ar = es.admin().indices().prepareAliases();
		ar.addAlias(index, alias);
		// NB: the index has had ''s added to it
		ar.removeAlias(OLD.substring(1, OLD.length()-1), alias);
		if (AppUtils.getServerType() == KServerType.LOCAL) {
			ar.setDebug(true);
			IESResponse resp = ar.get();
			System.out.println(resp);
		} else {
			// log how to switch
			String switchjson = 
					ar.getBodyJson();
//								("{'actions':[{'remove':{'index':"+OLD+",'alias':'"+alias+"'}},{'add':{'index':'"+index+"','alias':'"+alias+"'}}]}")
//								.replace('\'', '"');
			String switchMsg = "To switch old -> new:\n\n"
					+"curl http://localhost:9200/_aliases -d '"+switchjson+"' -H 'Content-Type:application/json'\n\n";
			Log.i("ES.init", switchMsg);
			msg += "\n"+switchMsg;
		}
		// record fail - but loop over the rest so we catch all the errors in one loop
		Log.e("init", ex.toString());
		return msg;
	}
	

	private static void initESMappings2_putMapping(
			Map<Class, Map> mappingFromClass, ESHttpClient es, 
			Class k,
			ESPath path, String index) 
	{
		PutMappingRequest pm = es.admin().indices().preparePutMapping(index);
		
		// ESType
		// passed in
		Map mapping = mappingFromClass==null? null : mappingFromClass.get(k);		
		ESType dtype = estypeForClass(k, mapping);
		
		// Call ES...
		pm.setMapping(dtype);
		pm.setDebug(true); //false); //DEBUG);
		IESResponse r2 = pm.get();
		r2.check();
	}

	public static ESType estypeForClass(Class k, Map mapping) {
		final ESType dtype = new ESType();
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
		dtype.property("@id", ESType.keyword);
		dtype.property("id", ESType.keyword);
		// shares NB: these dont use the ESKeyword annotation to avoid a dependency in YAC
		if (ReflectionUtils.hasField(k, "shares")) {
			List<String> noIndex = Arrays.asList("item","token","type","app");
			List<Field> fields = ReflectionUtils.getAllFields(ShareToken.class);
			ESType shares = new ESType();
			for (Field field : fields) {		
				if (noIndex.contains(field.getName())) {
					shares.property(field.getName(), new ESType().keyword().noIndex());
				} else {
					// treat String and XId as keywords		
					shares.property(field.getName(), ESType.keyword);
				}
			}
			dtype.property("shares", shares);
		}
		
		// reflection based
		estypeForClass2_reflection(k, dtype, new ArrayList());
		
		// done
		return dtype;
	}


	/**
	 * Look for ESKeyword annotations on fields.
	 * @param k
	 * @param dtype
	 * @param seenAlready
	 */
	private static void estypeForClass2_reflection(Class k, ESType dtype, Collection<Class> seenAlready) {
		Map props = (Map) dtype.get("properties");
		// Java class type (from Gson) - not indexed
		if (props == null || ! props.containsKey("@type")) {
			dtype.property("@type", new ESType().keyword().noIndex());
		}
		if (props == null || ! props.containsKey("@class")) {
			dtype.property("@class", new ESType().keyword().noIndex());
		}

		List<Field> fields = ReflectionUtils.getAllFields(k);
		for (Field field : fields) {
			String fname = field.getName();
			
			// already setup?			
			if (props != null && props.containsKey(fname)) {
				continue;
			}
			
			Class<?> type = field.getType();
			// loop check??
			if (dtype.containsKey(fname) || dtype.containsKey(fname.toLowerCase())) {
				continue;
			}
			ESType propType = estypeForClass3_reflection_field(field, type);
			if (propType==null) {
				continue; // eg no-index or default primitive
			}
			// set?
			if ( ! propType.isEmpty()) {
				dtype.property(fname, propType);
			}			
			// Recurse (but not into everything)
			if ( ! propType.isIndexed()) {
				continue;
			}
			if (type == Object.class 
				|| type.isPrimitive() 
				|| type.isArray() 
				|| ReflectionUtils.isa(type, Collection.class)
				|| ReflectionUtils.isa(type, Map.class)
				|| ReflectionUtils.isa(type, Throwable.class)
				|| type.isEnum()
				|| String.class.equals(type) 
			) {
				continue;
			}
			if (propType.get("type") != null && ! "object".equals(propType.get("type"))) {
				continue;
			}
			if (seenAlready.contains(type)) {
				continue; // no infinite recursion
			}
			ArrayList<Class> seenAlready2 = new ArrayList(seenAlready);
			seenAlready2.add(type);
			estypeForClass2_reflection(type, propType, seenAlready2);
			// set (in case we didnt earlier)
			if ( ! propType.isEmpty()) {
				dtype.property(fname, propType);
			}					
		}
	}

	
	private static ESType estypeForClass3_reflection_field(Field field, Class<?> type) 
	{			
		// HACK bugfix
		if ("_version".equals(field.getName())) {
			return null;
		}
		// keyword annotation?
		ESKeyword esk = field.getAnnotation(ESKeyword.class);
		if (esk!=null) {
			return ESType.keyword;
		}
		// class -> type
		// enum = keyword
		ESType est = null;
		if (type.isEnum()) {
			est = ESType.keyword;
		}
		if (String.class.equals(type)) {
			// trust the text default? -- That's not always wise!
			est = new ESType().text();
		}
		// IDs
		if (type.equals(XId.class) 
				|| ReflectionUtils.isa(type, AString.class) 
				|| "id".equals(field.getName())) {
			est = ESType.keyword;
		}
		// Time
		if (type.equals(Time.class)) {
			est = new ESType().date();
		}
		if (ReflectionUtils.isaNumber(type) || boolean.class.equals(type) || Boolean.class.equals(type)) {
			try {
				est = new ESType().setType(type);
			} catch(Exception ex) {
				// oh well
				Log.d("AppUtils", "ES mapping: Oh well: ESType.setType(): "+ex);
			}
		}
		// ??anything else ES is liable to guess wrong??		
		ESNoIndex esno = field.getAnnotation(ESNoIndex.class);
//		// trust the defaults for some stuff 
//		// BUT this breaks searching on a field before it has been indexed once
//		if (ReflectionUtils.isaNumber(type) || boolean.class.equals(type) || Boolean.class.equals(type)) {
//			if (esno==null) {
//				return null;			
//			}
//			return new ESType().setType(type).index(false);
//		}	
		if (est==null) est = new ESType(); 
		if (esno != null) {
			est = est.copy().noIndex(); // NB: copy for keyword, which is locked against edits
		}
		return est;
	}


	/**
	 * 
	 * @param from
	 * @param info Optional {name, img, description, url}
	 * @return
	 */
	public static PersonLite getCreatePersonLite(XId from, Map info) {
		assert from != null : info;
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
	 *  NB: not in {@link ESQueryBuilders} 'cos that cant see the SearchQuery class
	 * 
	 * @param sq never null
	 * @param start
	 * @param end
	 * @return
	 */
	public static BoolQueryBuilder makeESFilterFromSearchQuery(SearchQuery sq, Time start, Time end) {
		assert sq != null;
		
		BoolQueryBuilder filter = ESQueryBuilders.boolQuery();
		
		if (start != null || end != null) {
			if (start !=null && end !=null && ! end.isAfter(start)) {
				if (end.equals(start)) {
					throw new WebEx.E400("Empty date range - start = end = "+start+" Search: "+sq);
				}
				throw new WebEx.E400("Bad date range: start: "+start+" end: "+end+" Search: "+sq);
			}
			ESQueryBuilder timeFilter = ESQueryBuilders.dateRangeQuery("time", start, end);
			filter = filter.must(timeFilter);
		}
		
		// filters TODO a true recursive SearchQuery -> ES query mapping
		// TODO this is just a crude 1-level thing
		List ptree = sq.getParseTree();
		try {
			ESQueryBuilder q = parseTreeToQuery(ptree);
			if (q instanceof BoolQueryBuilder && ! ((BoolQueryBuilder) q).isEmpty()) {
				if (filter.isEmpty()) filter = (BoolQueryBuilder) q;
				else filter = filter.must(q);
			}
		} catch (Throwable e) {
			// Put full query info on an assertion failure
			throw new WebEx.E40X(400, "bad query "+sq, e);
		}
		
		return filter;
	}
	
	
	private static ESQueryBuilder parseTreeToQuery(Object rawClause) {
		if (rawClause instanceof String) {
			return ESQueryBuilders.simpleQueryStringQuery((String) rawClause);
		}
		if ( ! (rawClause instanceof List) && ! (rawClause instanceof Map)) {
			throw new IllegalArgumentException("clause is not list or map: " + rawClause);
		}						
		
		// Map means propname=value constraint.
		if (rawClause instanceof Map) {
			Map<String, Object> clause = (Map<String, Object>) rawClause;
			return parseTreeToQuery2_keyVal(clause);
		}					
		
		List clause = (List) rawClause;
		BoolQueryBuilder filter = ESQueryBuilders.boolQuery();
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
					ESQueryBuilder andTerm = parseTreeToQuery(term);
					filter = filter.must(andTerm);
				}
				return filter;
			}
			
			if (SearchQuery.KEYWORD_OR.equals((String) maybeOperator)) {
				for (Object term : clause.subList(1, clause.size())) {
					filter = filter.should(parseTreeToQuery(term));
				}
				return filter;
			}
			
			if (SearchQuery.KEYWORD_QUOTED.equals((String) maybeOperator)) {
				assert clause.size() == 2 : clause;
				String term = (String) clause.get(1);				
				ESQueryBuilder mp = ESQueryBuilders.matchPhrase(term);
				filter = filter.must(mp);
				return filter;
			}
		}
		
		// Fall-through clause: 2+ elements, first isn't a Boolean operator
		// Assume it's an implicit AND of all elements in list.
		for (Object term : clause) {
			filter = filter.must(parseTreeToQuery(term));
		}
		return filter;
	}
	
	/**
	 * @param clause key:value
	 * @return
	 */
	private static ESQueryBuilder parseTreeToQuery2_keyVal(Map<String, Object> clause) {
		// We expect only one pair per clause, but no reason not to tolerate multiples.
		BoolQueryBuilder filter = ESQueryBuilders.boolQuery();
		for (String prop : clause.keySet()) {
			Object val = clause.get(prop);
			// handle special "unset" value
			if (ESQueryBuilders.UNSET.equals(val)) {
				ESQueryBuilder setFilter = ESQueryBuilders.existsQuery(prop);				
				filter = filter.mustNot(setFilter);
				continue; // NB no "just one?" streamlining for mustNot
			}
			ESQueryBuilder kvFilter = parseTreeToQuery3_keyVal2(prop, val);
			// just one?
			if (clause.size() == 1) {
				return kvFilter; // no extra wrapping
			}
			filter = filter.must(kvFilter);				
		}
		// return must/musnt key(s)=value(s)
		return filter;
	}


	private static ESQueryBuilder parseTreeToQuery3_keyVal2(String prop, Object val) {		
		ESQueryBuilder kvFilter;
		// HACK due:before:
		if (val instanceof Map) {
			// HACK before/after?
			assert ((Map) val).size() == 1 : val;
			Map.Entry<String, String> kv = (Entry<String, String>) Containers.first(((Map) val).entrySet());
			String val2 = kv.getValue();
			double n;
			switch(kv.getKey()) {
			case "before":					
				Time end = TimeUtils.parseExperimental(val2);
				kvFilter = ESQueryBuilders.dateRangeQuery(prop, null, end);
				break;
			case "after":
				Time start = TimeUtils.parseExperimental(val2);
				kvFilter = ESQueryBuilders.dateRangeQuery(prop, start, null);
				break;
			case "above":
				n = MathUtils.toNum(val2);
				kvFilter = ESQueryBuilders.rangeQuery(prop, n, null, false);
				break;
			case "below":
				n = MathUtils.toNum(val2);
				kvFilter = ESQueryBuilders.rangeQuery(prop, null, n, false);
				break;
			default:
				throw new TodoException(prop+": "+val);
			}
			return kvFilter;
		}
		// regex?
		if (val instanceof String) {
			String sval = (String) val;
			if (sval.startsWith("/") && sval.endsWith("/")) {
				String regex = sval.substring(1, sval.length()-1);
				ESQueryBuilder regexp = ESQueryBuilders.regexp(prop, regex);
				return regexp;
			}
		}
		// normal key=value case
		kvFilter = ESQueryBuilders.termQuery(prop, val);
		return kvFilter;		
	}


	@Deprecated
	public static <X> X getConfig(String appName, X config, String[] args) {
		return (X) getConfig(appName, config.getClass(), args);
	}


	public static <T> JThing<T> setStatus(JThing<T> thing, KStatus newStatus) {
		Utils.check4null(thing, newStatus);
		thing.put("status", newStatus);
		return thing;
	}


	public static KStatus getStatus(JThing thing) {
		Object s = thing.map().get("status");
		if (s==null) {
			return null; // odd
		}
		if (s instanceof KStatus) return (KStatus) s;
		return KStatus.valueOf((String) s);
	}


	/**
	 * 
	 * @param mtype
	 * @param domain e.g. "as.good-loop.com"
	 * @return e.g. "https://testas.good-loop.com"
	 */
	public static StringBuilder getServerUrl(KServerType mtype, String domain) {
		assert ! domain.startsWith("http") && ! domain.endsWith("/") : domain;
		
		// SoGive uses "app.sogive.org", "test.sogive.org", "local.sogive.org" for Historical Reasons
		if ("app.sogive.org".equals(domain) && mtype != KServerType.PRODUCTION) {
			domain = ".sogive.org";
		}
		
		StringBuilder url = new StringBuilder();
		url.append(mtype==KServerType.LOCAL? "http" : "https");
		url.append("://"); url.append(mtype==KServerType.PRODUCTION? "" : mtype.toString().toLowerCase());
		url.append(domain);
		return url;
	}


	/**
	 * Std implementation of IESRouter
	 * @param dataspaceIgnored
	 * @param type
	 * @param id
	 * @param status
	 * @return
	 */
	public static ESPath getPath(CharSequence dataspaceIgnored, Class type, CharSequence id, Object status) {		 
		String stype = type==null? null : type.getSimpleName().toLowerCase();
		// HACK NGO -> charity
		if ("ngo".equals(stype)) stype = "charity";
		// HACK map personlite and person to the same DB
		if (type==PersonLite.class) stype = "person";
		
		String index = stype;
		KStatus ks = (KStatus) status;
		if (ks==null) ks = KStatus.PUBLISHED;
		switch(ks) {
		case PUBLISHED: case ARCHIVED: case PUB_OR_ARC:
			break;
		case DRAFT: case PENDING: case REQUEST_PUBLISH: case MODIFIED:
			index += ".draft";
			break;
		case TRASH:
			index += ".trash";
			break;		
		case ALL_BAR_TRASH: case PUB_OR_DRAFT: // NB PUB_OR_DRAFT is overridden where ARCHIVED is an index
			String i1 = index; // pub or arc
			String i2 = index + ".draft"; // draft etc
			ESPath esp = new ESPath(new String[] {i1, i2}, stype, id);
			return esp;
		default:
			throw new IllegalArgumentException(type+" "+status);
		}
		return new ESPath(index, stype, id);
	}

/**
 * Setup notes - see App2AppAuthClientTest
 * @param config
 * @param appAuthName
 * @return
 */
	public static AuthToken initAppAuth(ISiteConfig config, String appAuthName) {
		// idempotent
		if (Dep.has(AuthToken.class)) {
			return Dep.get(AuthToken.class);
		}
		YouAgainClient yac = Dep.get(YouAgainClient.class);
		String appAuthJWT = config.getAppAuthJWT();
		// use JWT if we have it
		if ( ! Utils.isBlank(appAuthJWT)) {
			AuthToken token = new AuthToken(appAuthJWT);
			Log.d("init.auth", "AuthToken set from config.getAppAuthJWT "+token.getXId());
			return Dep.set(AuthToken.class, token);
		}
		AuthToken token = yac.loadLocal(new XId(appAuthName+"@app"));
		if (token != null) {
			Log.d("init.auth", "AuthToken set from loadLocal .token folder "+token.getXId());
			return Dep.set(AuthToken.class, token);
		}
		String appAuthPassword = config.getAppAuthPassword();			
		if (Utils.isBlank(appAuthName) || Utils.isBlank(appAuthPassword)) {
			Log.d(appAuthName, ":( Expected config to provide appAuthJWT for connecting with YouAgain. Missing app-auth details: app-name: "+
					appAuthName+" p: "+appAuthPassword+" from "+config.getClass());
			return null;
		}
		App2AppAuthClient a2a = yac.appAuth();
		try {
			token = a2a.getIdentityTokenFromYA(appAuthName, appAuthPassword);
			Log.d("init.auth", "AuthToken fetched by name+password "+token.getXId());
		} catch(Exception wex) {
			token = a2a.registerIdentityTokenWithYA(appAuthName, appAuthPassword);
			Log.d("init.auth", "AuthToken registered with name+password "+token.getXId());
		}
		return Dep.set(AuthToken.class, token);
	}


	/**
	 * @deprecated Better to manage the save-path directly.
	 * @param item
	 * @param state
	 */
	public static void doSaveEdit(AThing item, WebRequest state) {
		ESPath path = getPath(null, item.getClass(), item.getId(), item.getStatus());
		doSaveEdit(path, new JThing(item), state);		
	}


	public static String maybeLocalTestUrl(String url, KServerType st) {
		if (st==null || st==KServerType.PRODUCTION) {
			return url;
		}
		if (st==KServerType.LOCAL) {
			url = url.replace("s://", "://local");
		} else if (st==KServerType.TEST) {
			url = url.replace("://", "://test");
		}
		return url;
	}


	
}
