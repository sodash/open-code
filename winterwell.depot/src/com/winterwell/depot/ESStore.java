package com.winterwell.depot;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.winterwell.depot.IHasVersion.IHasBefore;
import com.winterwell.es.ESType;
import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.GetRequestBuilder;
import com.winterwell.es.client.GetResponse;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.IndexRequestBuilder;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.PutMappingRequestBuilder;
import com.winterwell.gson.FlexiGson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.XStreamUtils;

/**
 * TODO support for diffs 
 * stash a copy of the ES json when it loads, and to use that (with Merger) when it saves it.
 * see {@link IHasBefore}
 *  
 * @author daniel
 *
 */
public class ESStore implements IStore {

	private Cache<String, String> indexCache  = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

	@Override
	public void init() {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		DepotConfig dc = Dep.get(DepotConfig.class);
		for(String tag : StrUtils.split(dc.tags)) {
			String index = "depot_"+tag;
			String type = "artifact";
			initIndex(index, type);
		}
	}
	
	@Override
	public String getRaw(Desc desc) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		String index = "depot_"+Utils.or(desc.getTag(), "untagged");
		String type = "artifact";
		initIndex(index, type);
		GetRequestBuilder getter = new GetRequestBuilder(esc).setIndex(index).setType(type).setId(desc.getId()).setSourceOnly(true);
		GetResponse resp = getter.get();
		String json = resp.getJson();
		if (json==null) return null;
		FlexiGson gson = Dep.get(FlexiGson.class);
		ESStoreWrapper essw = gson.fromJson(json, ESStoreWrapper.class);
		return essw.raw;
	}

	private void initIndex(String index, String type) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		// make index
		String bindex = index+"_"+esc.getConfig().getIndexAliasVersion();
		CreateIndexRequest pc = esc.admin().indices().prepareCreate(bindex);
		pc.setAlias(index);
		pc.get(); // this will fail if it already exists - oh well
		// mapping
		PutMappingRequestBuilder pm = esc.admin().indices().preparePutMapping(index, type);
		ESType mapping = new ESType()
				.property("raw", new ESType().text().noIndex());
		// TODO disable _all and other performance boosts
		pm.setMapping(mapping);
		IESResponse resp = pm.get().check();
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
//		FlexiGson gson = Dep.get(FlexiGson.class);
//		String json = gson.toJson(artifact);		
		IndexRequestBuilder put = esc.prepareIndex(index, type, desc.getId());;
		put.setBodyDoc(new ESStoreWrapper(artifact));
		IESResponse resp = put.get().check();	
		indexCache.put(index, desc.getTag());
	}
	
	
	
	
	@Override
	public void flush() {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		String indexList = StrUtils.join(indexCache.asMap().keySet(), ",");
		// recent indices
		// TODO call /indexList/_refresh
	}

	@Override
	public <X> X get(Desc<X> desc) {
		String raw = getRaw(desc);
//		FlexiGson gson = Dep.get(FlexiGson.class);
//		Object obj = gson.fromJson(raw);
		Object obj = XStreamUtils.serialiseFromXml(raw);
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

class ESStoreWrapper {

	private transient Object artifact;

	public ESStoreWrapper(Object artifact) {
		this.artifact = artifact;
		// Base 64 encode too??
		this.raw = XStreamUtils.serialiseToXml(artifact);
	}

	public String raw;
	
}
