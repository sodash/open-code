package com.winterwell.web.app;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.winterwell.data.JThing;
import com.winterwell.data.KStatus;
import com.winterwell.es.ESPath;
import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.GetRequestBuilder;
import com.winterwell.es.client.GetResponse;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.UpdateRequestBuilder;
import com.winterwell.gson.Gson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Key;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
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
			"stross"
			);
	private static final List<String> TEST_MACHINES = Arrays.asList(
			"hugh", "mail.soda.sh"
			);
	
	/**
	 * Will try path,indices in order if multiple
	 * @param path
	 * @return
	 */
	public static <X> X get(ESPath path, Class<X> klass) {
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		ESHttpClient.debug = true;

		GetRequestBuilder s = new GetRequestBuilder(client);		
		s.setIndices(path.indices[0]).setType(path.type).setId(path.id);
		s.setSourceOnly(true);
		GetResponse sr = s.get();
		if (sr.isSuccess()) {
			if (klass!=null) {
				Gson gson = Dep.get(Gson.class);
				X item = gson.fromJson(sr.getSourceAsString(), klass);
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
		up.setDocAsUpsert(true);
		// NB: this doesn't return the merged item :(
		IESResponse resp = up.get().check();
		
		// Also update draft		
		if ( ! draftPath.equals(publishPath)) {
			UpdateRequestBuilder upd = client.prepareUpdate(draftPath);
			upd.setDoc(draft.map());
			upd.setDocAsUpsert(true);
			IESResponse respd = upd.get().check();
		}
		
		// Keep the draft!
//		// OK - delete the draft (ignoring the race condition!)
//		DeleteRequestBuilder del = client.prepareDelete(draftPath.index(), draftPath.type, draftPath.id);
//		IESResponse ok = del.get().check();		

		return draft;
	}
	
	
	public static  void doDelete(ESPath path) {		
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		DeleteRequestBuilder del = client.prepareDelete(path.index(), path.type, path.id);
		IESResponse ok = del.get().check();		
	}

	public static JThing doSaveEdit(ESPath path, JThing item, WebRequest state) {
		assert path.index().toLowerCase().contains("draft") : path;
		
		// update status TODO factor out the status logic
		Object s = item.map().get("status");
		if (Utils.streq(s, KStatus.PUBLISHED)) {
			item.put("status", KStatus.MODIFIED);
		} else {
			item.put("status", KStatus.DRAFT);
		}
		
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		XId user = state.getUserId();		
		// save update		
		// sanity check id matches path
		String id = (String) item.map().get("@id"); //mod.getId();
		if (id==null) id = (String) item.map().get("id");
		assert id != null && ! id.equals("new") : "use action=new "+state;
		assert id.equals(path.id) : path+" vs "+id;
		// save to ES
		UpdateRequestBuilder up = client.prepareUpdate(path);
		// This should merge against what's in the DB
		up.setDoc(item.map());
		up.setDocAsUpsert(true);
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
		}
		Log.d("AppUtils", "Using serverType "+_serverType);
		return _serverType;
	}		
	
	private static KServerType _serverType;

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
		String hostname = WebUtils.fullHostname();
		Log.d("init", "serverType for host "+hostname+" ...?");
		if (LOCAL_MACHINES.contains(hostname)) {
			Log.i("init", "Treating "+hostname+" as serverType = "+KServerType.LOCAL);
			return KServerType.LOCAL;
		}
		if (TEST_MACHINES.contains(hostname)) {
			Log.i("init", "Treating "+hostname+" as serverType = "+KServerType.TEST);
			return KServerType.TEST;
		}

		Log.i("init", "Treating "+hostname+" as serverType = "+KServerType.PRODUCTION);
		return KServerType.PRODUCTION;
	}


	
}
