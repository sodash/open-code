package com.winterwell.depot;

import java.io.File;
import java.lang.reflect.Constructor;

import winterwell.utils.Utils;
import winterwell.utils.reporting.Log.KErrorPolicy;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.Option;

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

	@Option
	File dir = new File(FileUtils.getWinterwellDir(), "datastore");

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
		
		if (writeBehind!=null) {
			SlowStorage wb = new SlowStorage(s, writeBehind, depot);
			return wb;
		}
		return s;
	}
}
