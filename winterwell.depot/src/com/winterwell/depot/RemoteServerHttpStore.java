package com.winterwell.depot;

import java.io.File;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.FakeBrowser;

/**
 * Get over http from a servlet. 
 * 
 * Relies on DepotServlet in creole, and uses SoDash login credentials.
 * 
 * Status: NOT used yet
 * @author daniel
 * @testedby  RemoteServerHttpStoreTest}
 */
public class RemoteServerHttpStore extends AStore {
	
	/**
	 * Default winterwell.com setup!
	 */
	public RemoteServerHttpStore(DepotConfig config) {
		super(config, new FileStore(config));
	}

	public <X> void put(com.winterwell.depot.Desc<X> desc, X artifact) {
		// write local
		getBase().put(desc, artifact);
		// Local only?
		if (RemoteStore.localOnly(desc) || desc.getServer().equals(Desc.ANY_SERVER)) {
			return;
		}
		// TODO!
	}

	@Override
	public boolean contains(Desc desc) {
		if (getBase().contains(desc)) return true;
		if (RemoteStore.localOnly(desc)) {
			return false;
		}
		throw new TodoException();
	}

	<X> X getRangedData(Desc<X> desc) {
		assert desc.range != null;
		assert ! RemoteStore.localOnly(desc);
		throw new TodoException(desc);
	}

	@Override
	public <X> X get(Desc<X> desc) {
		// Remote fetch of ranged data?
		if (desc.range != null && ! RemoteStore.localOnly(desc)) {
			return getRangedData(desc);
		}
		// Do we have a local version?
		X local = getBase().get(desc);
		if (local!=null) {
			MetaData meta = getBase().getMetaData(desc);
			if (meta.isValid()) return local;
		}

		// Local only?
		if (RemoteStore.localOnly(desc)) {
			return local; // Possibly out of date!
		}

		File localPath = ((FileStore)getBase()).getFilingFn().apply(desc);
		try {
			localPath.getParentFile().mkdirs();
			FakeBrowser fb = new FakeBrowser();
			if (config.httpPassword!=null) {
				fb.setAuthentication(config.httpName, config.httpPassword);
			}
			fb.setIgnoreBinaryFiles(false);
			fb.setSaveToFile(localPath);
			Map<String, String> vars = new ArrayMap(
				"desc", XStreamUtils.serialiseToXml(desc)
			);			

			// What server?
			String server = desc.server==null || Desc.CENTRAL_SERVER.equals(desc.server)? 
					config.defaultRemoteHost : desc.server;
			
			// FIXME SECURITY HOLE! https gets an error?!
			String url = (config.https?"https" : "http")+"://"+server+(config.httpPort>0? ":"+config.httpPort : "") + config.httpPath;
			fb.post(url, vars);

//			assert localPath.exists() : localPath; The remote server might return null!
		} catch(Throwable ex) {
			// oh well
			Log.w(Depot.TAG, "Couldn't get "+desc+": "+ex);
			return local; // possibly out-of-date!
		}

//		// TODO fetch the meta data too
//		try {
//			File remoteMetaPath = new File(remotePath+".meta");
//			File localMetaPath = localStore.getMetaFile(localPath);
//
//			fetchFile(remoteMetaPath, localMetaPath, desc);
//			// update meta-data
////			MetaData md = getMetaData(desc);
////			md.loadTime = new Time();
//		} catch(Throwable ex) {
//			// oh well
//			Log.report(Depot.TAG, "Couldn't get metadata for "+desc+": "+ex, Level.INFO);
//		}
		// It should now be in the local store
		local = getBase().get(desc);
		return local;
	}


	@Override
	public Set<Desc> loadKeys(Desc partialDesc) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "RemoteServerStore->"+getBase();
	}

	@Override
	public void remove(Desc desc) {
		getBase().remove(desc);
		// remote delete?
		if (RemoteStore.localOnly(desc)) return;
		// TODO
	}

}
