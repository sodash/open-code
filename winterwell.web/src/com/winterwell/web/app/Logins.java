package com.winterwell.web.app;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.Dep;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.Option;
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

	static Logins dflt = init();
	
	static Logins init() {
		ConfigBuilder cb = ConfigFactory.get().getConfigBuilder(Logins.class);
		File f = new File(FileUtils.getWinterwellDir(), "logins/logins.misc.properties");
		if (f.isFile()) {
			cb.set(f);
		}
		Logins logins = cb.get();
		return logins;
	}	

	@Option
	Map<String,String> logins = new HashMap();

	public static LoginDetails get(String domain) {
		assert ! Utils.isBlank(domain);
		String _domain = domain.replace('.', '_');
		// what do we have?
		List<String> keys = Containers.filter(dflt.logins.keySet(), k -> k.startsWith(_domain));
		if (keys.isEmpty()) return null;
		LoginDetails ld = new LoginDetails(domain);
		for (String k : keys) {
			String f = k.substring(_domain.length()+1);
			ReflectionUtils.setPrivateField(ld, f, dflt.logins.get(k));
		}		
		return (LoginDetails) ld;
	}
	
	
}
