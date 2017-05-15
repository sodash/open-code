package com.winterwell.depot;

import java.io.File;
import java.util.Set;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.depot.Desc;
import com.winterwell.depot.IHasVersion.IHasBefore;
import com.winterwell.depot.MetaData;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.GetRequestBuilder;
import com.winterwell.es.client.GetResponse;
import com.winterwell.gson.FlexiGson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.TodoException;

/**
 * TODO support for diffs 
 * stash a copy of the ES json when it loads, and to use that (with Merger) when it saves it.
 * see {@link IHasBefore}
 *  
 * @author daniel
 *
 */
public class ESStore implements IStore {

	boolean diffWherePossible;
	
	@Override
	public String getRaw(Desc desc) {
		// TODO Auto-generated method stub
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		GetRequestBuilder getter = new GetRequestBuilder(esc).setIndex("").setType("").setId("").setSourceOnly(true);
		GetResponse resp = getter.get();
		String json = resp.getJson();
		return json;
	}

	@Override
	public void remove(Desc arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean contains(Desc desc) {
		return getRaw(desc) != null;
	}

	@Override
	public <X> void put(Desc<X> desc, X artifact) {
		// TODO Auto-generated method stub
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		FlexiGson gson = Dep.get(FlexiGson.class);
		String json = gson.toJson(artifact);		
		if (diffWherePossible && artifact instanceof IHasBefore) {
			String beforeJson = (String) ((IHasBefore) artifact).getBefore();
			Object beforeMap = JSON.parse(beforeJson);
			esc.prepareUpdate(index, type, id);
			// TODO
		} else {
			esc.prepareIndex(index, type, id);	
		}		
	}

	@Override
	public <X> X get(Desc<X> desc) {
		String raw = getRaw(desc);
		FlexiGson gson = Dep.get(FlexiGson.class);
		Object obj = gson.fromJson(raw);
		if (diffWherePossible && obj instanceof IHasBefore) {
			// stash the json
			((IHasBefore) obj).setBefore(raw);
		}
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
