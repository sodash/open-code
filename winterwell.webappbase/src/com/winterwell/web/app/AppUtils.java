package com.winterwell.web.app;

import java.util.Arrays;
import java.util.Map;

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
	
	/**
	 * Will try path,indices in order if multiple
	 * @param path
	 * @return
	 */
	public static Map<String, Object> get(ESPath path) {
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		ESHttpClient.debug = true;

		GetRequestBuilder s = new GetRequestBuilder(client);		
		s.setIndices(path.indices[0]).setType(path.type).setId(path.id);
		s.setSourceOnly(true);
		GetResponse sr = s.get();
		if (sr.isSuccess()) {
			Map<String, Object> json = sr.getSourceAsMap(); //SourceAsString();
			return json;
		}
		Exception error = sr.getError();
		if (error!=null) {
			if (error instanceof WebEx.E404) {
				// was version=draft?
				if (path.indices.length > 1) {
					ESPath path2 = new ESPath(Arrays.copyOfRange(path.indices, 1, path.indices.length), path.type, path.id);
					return get(path2);
				}
				// 404
				return null;
			}
			throw Utils.runtime(error);
		}
		return null;
	}

	
	public static JThing doPublish(JThing draft, ESPath draftPath, ESPath publishPath) {
		// prefer being given the draft to avoid ES race conditions
		if (draft==null) {
			Map<String, Object> draftMap = get(draftPath);
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
		UpdateRequestBuilder upd = client.prepareUpdate(draftPath);
		upd.setDoc(draft.map());
		upd.setDocAsUpsert(true);
		IESResponse respd = upd.get().check();
		
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

	
}
