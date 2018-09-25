package com.winterwell.depot;

import java.io.File;
import java.lang.reflect.Constructor;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.Option;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * Depot's configuration params. By default, this is loaded from config/Depot.properties
 * @author daniel
 *
 */
public class DepotConfig {

	@Option(description="Port to connect to for remote http/https get. 0 for default (no explicit port) behaviour.")
	int httpPort = 0;

	@Option
	String defaultRemoteHost = "datastore.soda.sh";

	public String getDefaultRemoteHost() {
		return defaultRemoteHost;
	}

	// FIXME implement this switch in Depot
	@Option(description="If set, uses in-memory merge instead of store-level merge. This is less efficient and may have worse race conditions, but it is more general purpose.")
	boolean mergeInMemory;
	
	public DepotConfig() {
		// default directory - the winterwell one if present, otherwise local depot
		try {
			dir = new File(FileUtils.getWinterwellDir(), "datastore");
		} catch(Exception ex) {
			dir = new File("depot");
		}
	}
	
	@Option
	String remoteUser = "winterwell";

	@Option
	String httpName;
	@Option
	String httpPassword;

	@Option
	String httpPath = "/shard-depot?as=su";

	/**
	 * There's a balance to be struck here:
	 * Write too often: waste time.
	 * Too big a gap: hold lots of memory.
	 */
	@Option
	Dt writeBehind = new Dt(60, TUnit.SECOND);

	/**
	 * Add some randomness to the delay -- to help avoid clashes between servers/processes.
	 * 
	 * In [0,1] Randomly adjust the writeBehind delay by * this much (so 0.1 = 10% = +/- 5%).
	 */
	@Option
	double writeBehindJitter = 0.1;
	
	@Option
	Dt batch;
	
	@Option
	File dir;

	@Option
	KErrorPolicy errorPolicy = KErrorPolicy.DELETE_CAUSE; // !!

	@Option(description="If true (the default), any exception when writing a sub-module will be swallowed. See @ModularXML")
	boolean allowModuleExceptions = true;

	@Option(description="How long a locally cached file can be used for. Can be over-ridden for a specific Desc.")
	public Dt maxAge = TUnit.MONTH.dt;

	@Option
	public boolean https  = true;

	@Option(description="What underlying key-value store to use? E.g. com.winterwell.depot.es.ESStore")
	public Class<? extends IStore> storeClass = 
									RemoteStore.class;

	@Option
	public String tags = "untagged";

	/**
	 * If part of a modular object is not found - should the whole get() fail? 
	 * Or should that part be treated as null?
	 */
	@Option
	public KErrorPolicy onArtifactModuleNotFound = KErrorPolicy.THROW_EXCEPTION;

	public IStore getStore(Depot depot) {
		IStore s;
		// Try for a constructor which takes in a DepotConfig object.
		// Fallback to a no-arg constructor
		try {
			Constructor<? extends IStore> cons = storeClass.getConstructor(DepotConfig.class);
			s = cons.newInstance(this);
		} catch (Exception e) {
			try {
				s = storeClass.newInstance();
			} catch (Exception e1) {
				throw Utils.runtime(e1);
			}
		}
		// init
		s.init();
		
		// SlowStorage?
		if (writeBehind!=null) {
			SlowStorage wb = new SlowStorage(s, writeBehind, depot);
			if (writeBehindJitter!=0) {
				if ( ! MathUtils.isProb(writeBehindJitter)) {
					Log.e("DepotConfig", "Invalid jitter "+writeBehindJitter);
				} else {
					wb.setDelayJitter(writeBehindJitter);
				}
			}
			if (batch!=null && batch.getMillisecs() != 0) {
				if (batch.getMillisecs() < 0) {
					Log.e("DepotConfig", "Invalid batch "+batch);
				} else {
					wb.setBatch(batch);
				}
			}
			return wb;
		}
		return s;
	}
}
