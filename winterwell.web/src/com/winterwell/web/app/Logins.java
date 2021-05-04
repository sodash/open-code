package com.winterwell.web.app;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.Option;
import com.winterwell.utils.log.Log;
import com.winterwell.web.LoginDetails;

/**
 * Good-Loop specific convenience for managing login details.
 * This looks up login details from logins/logins.misc.properties
 *  -- which is NOT in a public git repo.
 *  
 * Status: Low coverage. Most of our logins are NOT in here.
 *  
 * @author daniel
 *
 */
public class Logins {

	private static final String LOGTAG = "Logins";

	static Logins dflt = init();
	
	private static File loginsDir;
	
	public static File getLoginsDir() {
		return loginsDir;
	}
	
	static Logins init() {
		ConfigBuilder cb = ConfigFactory.get().getConfigBuilder(Logins.class);
		loginsDir = new File(FileUtils.getWinterwellDir(), "logins");
		File f = new File(loginsDir, "logins.misc.properties");
		if (loginsDir.isFile()) {
			cb.set(loginsDir);
		}
		Logins logins = cb.get(); // This allows the logins map to be populated from the properties
		Log.i(LOGTAG, "init credentials: "+logins.logins.keySet());
		return logins;
	}	
	
	/**
	 * 
	 * @param appName
	 * @param filename
	 * @return never null - might not exist
	 */
	public static File getLoginFile(String appName, String filename) {
		File f = new File(loginsDir, appName+"/"+filename);
		if ( ! f.isFile()) {
			Log.i(LOGTAG, "No credentials file: "+f);
		}
		Log.i(LOGTAG, "Found credentials file: "+f);
		return f;
	}
	
	@Option
	Map<String,String> logins = new HashMap();

	public static LoginDetails get(String domain) {
		assert ! Utils.isBlank(domain);
		String _domain = domain.replace('.', '_');
		// what do we have?
		List<String> keys = Containers.filter(dflt.logins.keySet(), k -> k.startsWith(_domain));
		if (keys.isEmpty()) {
			return null;
		}
		LoginDetails ld = new LoginDetails(domain);
		for (String k : keys) {
			String f = k.substring(_domain.length()+1);
			ReflectionUtils.setPrivateField(ld, f, dflt.logins.get(k));
		}		
		return (LoginDetails) ld;
	}
	
	
}
