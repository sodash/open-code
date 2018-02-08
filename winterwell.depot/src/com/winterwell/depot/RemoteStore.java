package com.winterwell.depot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.winterwell.bob.tasks.RemoteTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.utils.IFn;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * This has a local FileStore, combined with an SCP feature.
 * The behaviour is:
 * 
 * get: 
 * 1. try local
 * 2. then try remote & store locally
 * 
 * put:
 * 1. put local
 * 2. put remote ONLY if server has been set!
 * 
 * remove
 * 1. remove local ONLY??
 * 
 * @author daniel
 *
 */
public class RemoteStore implements IStore {

	@Override
	public void flush() {	
	}
	
	@Override
	public String getRaw(Desc desc) {
		return localStore.getRaw(desc);
	}
	
	private static final Dt MAX_SCP_TIME = new Dt(45, TUnit.MINUTE);

	private static final String LOGTAG = "remotestore";

	final FileStore localStore;
	
	File remoteDir = new File("/home/winterwell/datastore/");

	private final String remoteUser;
	/**
	 * This is used for {@link Desc#CENTRAL_SERVER} and also for misc requests.
	 */
	private final String defaultRemoteHost;

	private final IFn<Desc, File> desc2remotePath = new IFn<Desc, File>() {		
		@Override
		public File apply(Desc value) {
			File lf = localStore.getFilingFn().apply(value);
			String path = lf.isAbsolute()? FileUtils.getRelativePath(lf, localStore.dir) : lf.getPath();
			return new File(remoteDir, path);
		}
	};
	
	public File getLocalPath(Desc desc) {
		return localStore.getLocalPath(desc); 	
	}

	/**
	 * Default winterwell.com setup!
	 */
	public RemoteStore(DepotConfig config) {
		localStore = new FileStore(config);
		this.remoteUser = config.remoteUser;
		this.defaultRemoteHost = config.defaultRemoteHost;
	}

	public <X> void put(com.winterwell.depot.Desc<X> desc, X artifact) {
		// write local
		localStore.put(desc, artifact);
		File localPath = localStore.getFilingFn().apply(desc);
		
		// Local only?
		if (localOnly(desc) || desc.getServer().equals(Desc.ANY_SERVER)
			|| Desc.LOCAL_SERVER.equals(desc.serverHint))
		{
			return;
		}
		// TODO Are we the target server?
				
		File remotePath = getRemotePath(desc);						
		String userAtHost = getRemoteUserAtHost(desc);

		// Create remote dirs -- this is done by SCPTask!
		// ??We could remember which dirs exist to avoid extra calls
		
		// upload!		
		SCPTask scp = new SCPTask(localPath, userAtHost, remotePath.getPath());
		scp.setAtomic(true);
		scp.run();		
		scp.close();
	}
	
	/**
	 * TODO should this include Desc.ANY_SERVER??
	 * @param desc
	 * @return
	 */
	public static boolean localOnly(Desc desc) {
		if (desc.server==null || Desc.LOCAL_SERVER.equals(desc.server)) return true;
		String myServer = Desc.MY_SERVER();
		assert myServer.length() != 0 : desc;
		// Handle egan.soda.sh vs egan cases
		if ( ! myServer.contains(".") && desc.server.startsWith(myServer)) {
			return true;
		}
		if ( ! desc.server.contains(".") && myServer.startsWith(desc.server)) {
			Log.w("DataLog", "Dubious server match "+desc.server+" to "+myServer);
		}
		return myServer.equals(desc.server);
	}
	
	@Override
	public boolean contains(Desc config) {
		if (localStore.contains(config)) return true;
		if (localOnly(config)) {
			return false;
		}
		// ?? Could we be more efficient??		
		return get(config) != null;
	}

	public File getRemotePath(Desc desc) {
		File remotePath = desc2remotePath.apply(desc);
		// have we been given an absolute path?
		if (remotePath.isAbsolute()) {
			// resolve relative to remote?
			if (remotePath.getPath().startsWith(remoteDir.getPath())) {
				remotePath = new File(FileUtils.getRelativePath(remotePath, remoteDir));
			} else {
				// or local?
				remotePath = new File(FileUtils.getRelativePath(remotePath, localStore.dir));
			}							
		}		
		return new File(remoteDir, remotePath.getPath());
	}
	
	<X> X getRangedData(Desc<X> desc) {
		assert desc.range != null;
		assert ! localOnly(desc);
		// Fetch a directory listing
		File remotePath = getRemotePath(desc);
		File remotedDir = remotePath.getParentFile();
		// ...remote ls!
		RemoteTask ls = new RemoteTask(getRemoteUserAtHost(desc),
									"ls "+remotedDir.getPath());
		// timeout if it's not working (fast enough)
		ls.setMaxTime(TUnit.MINUTE.dt);
		ls.run();
		String listing = ls.getOutput();
//		System.out.println(listing);
		String[] pieces = listing.split("\\s+");
		// which bits do we want?
		List<File> remoteFiles = localStore.getRangedData3_bitsFilter(remotedDir, pieces, desc.range);
		
		// Fetch files
		File localDir = localStore.getFilingFn().apply(desc).getParentFile();
		localDir.mkdirs();
		List<File> localFiles = new ArrayList();
		for (File rmf : remoteFiles) {
			// TODO skip files we already have (but what about ones which have been edited?)
			File localPath = new File(localDir, rmf.getName());
			fetchFile(rmf, localPath, desc);
			localFiles.add(localPath);
		}
		
		// Deserialise		
		List<X> bits = new ArrayList();
		for (File file : localFiles) {
			X bit = localStore.get2_deserialise(file, desc); 
			bits.add(bit);
		}
		// filter & join
		X done = localStore.getRangedData2_filter_join(desc, bits);
		return done;
	}
	
	@Override
	public <X> X get(Desc<X> desc) {
		// Remote fetch of ranged data?
		if (desc.range != null && ! localOnly(desc)) {
			return getRangedData(desc);
		}
		// Do we have a local version?
		X local = localStore.get(desc);
		if (local!=null) {
			MetaData meta = localStore.getMetaData(desc);
			if (meta.isValid()) return local;
		}		
		
		// Local only?
		if (localOnly(desc)) {
			return local;
		}
		
		File remotePath = getRemotePath(desc);
		File localPath = localStore.getFilingFn().apply(desc);
		
		try {
			fetchFile(remotePath, localPath, desc);
			assert localPath.exists() : localPath;
		} catch(Throwable ex) {
			// oh well
			Log.report(Depot.TAG, "Couldn't get "+desc+": "+ex, Level.WARNING);
			return local;
		} 
		
		// fetch the meta data too
		try {
			File remoteMetaPath = new File(remotePath+".meta");
			File localMetaPath = localStore.getMetaFile(localPath);
			
			fetchFile(remoteMetaPath, localMetaPath, desc);
			// update meta-data
//			MetaData md = getMetaData(desc);
//			md.loadTime = new Time();			
		} catch(Throwable ex) {
			// oh well
			Log.report(Depot.TAG, "Couldn't get metadata for "+desc+": "+ex, Level.INFO);
		}
				
		// Files are a special case
		if (desc.getType()==File.class) {
			return (X) localPath;
		}
		// load with XStream
		Object artifact = FileUtils.load(localPath);
		return (X) artifact;
	}
	
	@Override
	public MetaData getMetaData(Desc desc) {
		// Local only?		
		if (localOnly(desc)) {
			return localStore.getMetaData(desc);
		}
		
		// Do we have a valid local version?
		MetaData local = localStore.getMetaData2(desc);
		if (local!=null) {
			if (local.isValid()) return local;
		}					
		
		File remotePath = getRemotePath(desc);
		File remoteMetaPath = new File(remotePath+".meta");
		File localPath = localStore.getFilingFn().apply(desc);
		File localMetaPath = localStore.getMetaFile(localPath);
		try {												
			// fetch & load
			fetchFile(remoteMetaPath, localMetaPath, desc);			
			MetaData meta = FileUtils.load(localMetaPath);
			return meta;
		} catch(Throwable ex) {
			MetaData md = new MetaData(desc);
			md.file = remotePath;
			// save it local
			FileUtils.save(md, localMetaPath);
			return md;
		}
	}
	
	
	void fetchFile(File remotePath, File localPath, Desc desc) throws RuntimeException {
		if (desc.range!=null) {
//			Hopefully, this is fetching one of the remote bits, & all OK 
		}
		assert remotePath.isAbsolute() : remotePath;
				
		localPath.getParentFile().mkdirs();
		assert ! localPath.isDirectory() : localPath;		
		
		// download!
		SCPTask scp = new SCPTask(getRemoteUserAtHost(desc),
									remotePath.getPath(), 
									localPath);
		scp.setAtomic(true);
		scp.setVerbosity(Level.OFF);
		// timeout if it's not working (fast enough)
		// WARNING: this does limit the maximum file size we can handle!!
		scp.setMaxTime(MAX_SCP_TIME);
		scp.run();
		scp.close();			
			
		assert localPath.exists() : localPath+" "+remotePath;
	}

	@Override
	public Set<Desc> loadKeys(Desc partialDesc) throws UnsupportedOperationException {
		Log.w(LOGTAG, "Only loading local keys for "+partialDesc);
		return localStore.loadKeys(partialDesc);
	}

	@Override
	public String toString() {
		return "RemoteStore->"+localStore;
	}
	
	@Override
	public void remove(Desc config) {
		// local delete
//		boolean del = 
		localStore.remove(config);		
		// remote delete?
		if (localOnly(config)) return;
			
		File remotePath = getRemotePath(config);						
		String userAtHost = getRemoteUserAtHost(config);
		try {
			RemoteTask delTask = new RemoteTask(userAtHost,
					"rm "+remotePath);
			// timeout if it's not working (fast enough)
			delTask.setMaxTime(TUnit.MINUTE.dt);
			delTask.run();
			return;
		} catch(Throwable ex) {
			Log.report(LOGTAG, "Couldn't delete "+remotePath+": "+ex, Level.INFO);
			return;
		}				
	}
	
	public FileStore getLocalStore() {
		return localStore;
	}

	/**
	 * Where to get files from.
	 * 
	 * @param remoteUserAtHost
	 */
	public String getRemoteUserAtHost(Desc desc) {
		assert remoteUser != null;
		String server = desc.server==null || Desc.CENTRAL_SERVER.equals(desc.server)? 
				defaultRemoteHost : desc.server;
		// How to handle "any"?
		// TODO have a pluggable distribution manager, which allocates artifacts to servers
		if (Desc.ANY_SERVER.equals(server)) {
			if (desc.serverHint!=null) {
				if (desc.serverHint.equals(Desc.CENTRAL_SERVER)) {
					server = defaultRemoteHost;
				} else {
					server = desc.serverHint;
				}
			} else {
				server = defaultRemoteHost;
				Log.report(Depot.TAG, "Trying default "+defaultRemoteHost+" for 'any' server", Level.WARNING);
			}
		}
		assert server!=null : this;
		return remoteUser+"@"+server;
	}

}
