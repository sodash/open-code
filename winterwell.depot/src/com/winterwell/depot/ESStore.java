package com.winterwell.depot;

import java.io.File;
import java.util.Set;

import org.eclipse.jetty.util.ajax.JSON;

import com.google.common.util.concurrent.ListenableFuture;
import com.winterwell.depot.Desc;
import com.winterwell.depot.IHasVersion.IHasBefore;
import com.winterwell.depot.MetaData;
import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.ESHttpResponse;
import com.winterwell.es.client.GetRequestBuilder;
import com.winterwell.es.client.GetResponse;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.IndexRequestBuilder;
import com.winterwell.es.client.UpdateRequestBuilder;
import com.winterwell.gson.FlexiGson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;

/**
 * TODO support for diffs 
 * stash a copy of the ES json when it loads, and to use that (with Merger) when it saves it.
 * see {@link IHasBefore}
 *  
 * @author daniel
 *
 */
public class ESStore implements IStore {
	
	@Override
	public String getRaw(Desc desc) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		String index = "depot_"+Utils.or(desc.getTag(), "untagged");
		String type = "artifact";
		GetRequestBuilder getter = new GetRequestBuilder(esc).setIndex(index).setType(type).setId(desc.getId()).setSourceOnly(true);
		GetResponse resp = getter.get();
		String json = resp.getJson();
		return json;
	}

	@Override
	public void remove(Desc desc) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		String index = "depot_"+Utils.or(desc.getTag(), "untagged");
		String type = "artifact";
		DeleteRequestBuilder del = esc.prepareDelete(index, type, desc.getId());
		IESResponse resp = del.get();
		// bark on failure??
		if (resp.getError()!=null) {
			Log.e("ESStore", resp.getError());
		}
	}

	@Override
	public boolean contains(Desc desc) {
		return getRaw(desc) != null;
	}

	@Override
	public <X> void put(Desc<X> desc, X artifact) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		String index = "depot_"+Utils.or(desc.getTag(), "untagged");
		String type = "artifact";
		FlexiGson gson = Dep.get(FlexiGson.class);
		String json = gson.toJson(artifact);		
		IndexRequestBuilder put = esc.prepareIndex(index, type, desc.getId());
		put.setBodyJson(json);
		IESResponse resp = put.get().check();		
	}

	@Override
	public <X> X get(Desc<X> desc) {
		String raw = getRaw(desc);
		FlexiGson gson = Dep.get(FlexiGson.class);
		Object obj = gson.fromJson(raw);
		return (X) obj;
	}

	@Override
	public Set<Desc> loadKeys(Desc partialDesc) {
		throw new UnsupportedOperationException(); 
	}

	@Override
	public MetaData getMetaData(Desc desc) {
		return new MetaData(desc);
	}

	@Override
	public File getLocalPath(Desc desc) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
