package com.winterwell.depot;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.util.ConcurrentHashSet;

import com.winterwell.depot.IHasVersion.IHasBefore;
import com.winterwell.es.ESPath;
import com.winterwell.es.ESType;
import com.winterwell.es.IESRouter;
import com.winterwell.es.client.BulkRequestBuilder;
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
import com.winterwell.utils.containers.Pair2;
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
public class ESDepotStore implements IStore {

//	/**
//	 * TODO Used for flush
//	 */
//	private Cache<String, String> indexCache  = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

	@Override
	public void init() {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		esc.checkConnection();
		DepotConfig dc = Dep.get(DepotConfig.class);
		// Is this defunct, and we init the first time we see a class??
		for(String tag : StrUtils.split(dc.tags)) {
			String index = "depot_"+tag;
			String type = "artifact";
			initIndex(index, type);
		}
	}
	
	@Override
	public void storeBatch(List<Pair2<Desc, Object>> add, List<Desc> remove) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		BulkRequestBuilder bulk = esc.prepareBulk();
		// remove all
		for (Desc desc : remove) {
			ESPath path = getPath(desc);
			DeleteRequestBuilder rm = esc.prepareDelete(path.index(), path.type, path.id);			
			bulk.add(rm);
		}
		// add all
		for (Pair2<Desc, Object> desc_artifact : add) {
			ESPath path = getPath(desc_artifact.first);
			IndexRequestBuilder index = esc.prepareIndex(path);
			ESStoreWrapper doc = new ESStoreWrapper(desc_artifact.second);
			index.setBodyDoc(doc);
//			indexCache.put(key, value);
			bulk.add(index);
		}
		IESResponse resp = bulk.get().check();			
	}


	@Override
	public String getRaw(Desc desc) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		ESPath path = getPath(desc);
		GetRequestBuilder getter = new GetRequestBuilder(esc).setPath(path).setSourceOnly(true);
		GetResponse resp = getter.get();
		String json = resp.getJson();
		if (json==null) return null;
		FlexiGson gson = Dep.get(FlexiGson.class);
		ESStoreWrapper essw = gson.fromJson(json, ESStoreWrapper.class);
		return essw.raw;
	}

	/**
	 * Key = baseIndex_type
	 */
	final Set<String> knownIndexes = new ConcurrentHashSet<>();
	
	/**
	 * Supports the use of index = an alias -> base, which allows for easier mapping updates
	 * and monthly data management.
	 * @param index
	 * @param esc
	 * @return e.g. index_dec18
	 */
	public String getBaseIndex(String index, ESHttpClient esc) {		
		return index+"_"+esc.getConfig().getIndexAliasVersion();
	}
	
	private void initIndex(String index, String type) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		// make index
		String bindex = getBaseIndex(index, esc);
		String knownIndexKey = bindex+"_"+type;
		if (knownIndexes.contains(knownIndexKey)) {
			// already done
			return;
		}
		initIndex2(index, type, esc);
		knownIndexes.add(knownIndexKey);
	}
	
	// after discussion, this doesn't need to be synchronized, but is probably faster that way
	private synchronized void initIndex2(String index, String type, ESHttpClient esc) {
		String bindex = getBaseIndex(index, esc);
		CreateIndexRequest pc = esc.admin().indices().prepareCreate(bindex);
		if ( ! bindex.equals(index)) {
			pc.setAlias(index);
		}
		pc.get(); // this will fail if it already exists - oh well
		// mapping
		PutMappingRequestBuilder pm = esc.admin().indices().preparePutMapping(index, type);
		// ES5+ types
		ESType mapping = new ESType()
				.property("raw", new ESType().object().noIndex());
		// TODO disable _all and other performance boosts
		pm.setMapping(mapping);
		IESResponse resp = pm.get().check();
		if (resp.getError()!=null) {
			Log.w("ES", resp.getError());
		}
		knownIndexes.add(bindex);
	}	

	@Override
	public void remove(Desc desc) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		ESPath path = getPath(desc);
		DeleteRequestBuilder del = esc.prepareDelete(path.index(), path.type, desc.getId());
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
	
	IESRouter esRouter;
	
	public void setEsRouter(IESRouter esRouter) {
		this.esRouter = esRouter;
	}

	@Override
	public <X> void put(Desc<X> desc, X artifact) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		ESPath path = getPath(desc);
		initIndex(path.index(), path.type);
		IndexRequestBuilder put = esc.prepareIndex(path);
		// ?? ESStoreWrapper uses XStream's xml encoding
		// ?? Sometimes, json would be better -- is there an elegant way to switch between the two?
		// ?? Would json be better here always? 
		// NB: our XStream does have some special-case handling for large blobs of data.
		// -- though that could be added to Gson too. See XStreamBinaryConverter
		ESStoreWrapper doc = new ESStoreWrapper(artifact);
		put.setBodyDoc(doc);
		IESResponse resp = put.get().check();
	}

	private ESPath getPath(Desc desc) {				
		if (esRouter != null) {
			return esRouter.getPath(desc.getType(), desc.getId(), null);
		}
		String tag = Utils.or(desc.getTag(), "untagged");
		String index = "depot_"+tag;
		String type = "artifact";
		return new ESPath(index, type, desc.getId());
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

	@Override
	public void flush() {
		// throw new TodoException();
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
