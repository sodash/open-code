package com.winterwell.web.app;

import java.util.Arrays;
import java.util.Map;

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
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.WebEx;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.JsonField;

public class AppUtils {


	public static final JsonField ITEM = new JsonField("item");
	
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
		Map<String, Object> json = sr.getSourceAsMap(); //SourceAsString();
		return json;
	}

	
	public static Map<String, Object> doPublish(ESPath draftPath, ESPath publishPath) {
		Map<String, Object> draft = get(draftPath);
//		Gson gson = Dep.get(Gson.class);
		if (draft.containsKey("modified")) draft.put("modified", false);
		// load from ES, merge, save		
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		UpdateRequestBuilder up = client.prepareUpdate(publishPath.index(), publishPath.type, publishPath.id);
		up.setDoc(draft);
		up.setDocAsUpsert(true);
		// NB: this doesn't return the merged item :(
		IESResponse resp = up.get().check();

		// OK - delete the draft (ignoring the race condition!)
		DeleteRequestBuilder del = client.prepareDelete(draftPath.index(), draftPath.type, draftPath.id);
		IESResponse ok = del.get().check();		

		return draft;
	}

	public static  void doDelete(ESPath path) {		
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		DeleteRequestBuilder del = client.prepareDelete(path.index(), path.type, path.id);
		IESResponse ok = del.get().check();		
	}

	public static Map<String,Object> doSaveEdit(ESPath path, Map item, WebRequest state) {
		assert path.index().toLowerCase().contains("draft") : path;
		ESHttpClient client = new ESHttpClient(Dep.get(ESConfig.class));
		XId user = state.getUserId();
//		Map item = (Map) state.get(ITEM);		
		String version = state.get("version");
//		boolean isDraft = "draft".equals(version) || version==null;
//		assert isDraft : version;
//		item.put("modified", isDraft);
		// turn it into a charity (runs some type correction)
//		NGO mod = Thing.getThing(item, NGO.class);		
		// save update		
		String id = (String) item.get("@id"); //mod.getId();
		if (id==null) id = (String) item.get("id");
		assert id != null && ! id.equals("new");
		assert id.equals(path.id) : path+" vs "+id;
//		String idx = isDraft? config.charityDraftIndex : config.charityIndex;		
		UpdateRequestBuilder up = client.prepareUpdate(path.index(), path.type, path.id);
//		item = new ArrayMap("name", "foo"); // FIXME
		// This should merge against what's in the DB
		up.setDoc(item);
		up.setDocAsUpsert(true);
		// NB: this doesn't return the merged item :(
		IESResponse resp = up.get().check();
//		Map<String, Object> item2 = resp.getParsedJson();
		
		return item;
	}

	
}
