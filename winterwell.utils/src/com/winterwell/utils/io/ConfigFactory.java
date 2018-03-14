package com.winterwell.utils.io;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;

/**
 * This applies a "default" lookup strategy for properties files to the use of {@link ConfigBuilder} and {@link Dep}.
 * 
 * You can over-ride it to implement your own config strategy.
 * 
 * ConfigBuilder is library-like (you can use it with any setup), 
 * whilst this is framework-like (it will do more for you, but it expects your project to follow the structure here).
 * 
 * 
 * @author daniel
 *
 */
public class ConfigFactory {

	private static final String LOGTAG = ConfigBuilder.LOGTAG;
	private String[] args;
	private String appName;
	String serverType;
	private String machine = WebUtils.hostname();
	private boolean debug = true;
	private final List<ConfigBuilder> history = new ArrayList();
	
	private boolean strict;
	
	/**
	 * If true, then duplicate config loading will cause an exception.
	 * @param strict
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	/**
	 * Access via Dep! This constructor is for over-riding.
	 * 
	 * @param appName
	 * @param args
	 */
	protected ConfigFactory() {
		appName = FileUtils.getWorkingDirectory().getName(); // guess!
	}
	
	public void setAppName(String appName) {
		this.appName = appName;
	}
	/**
	 * e.g. local / test / production. See {@link KServerType}
	 * @param serverType
	 * @return 
	 */
	public ConfigFactory setServerType(String serverType) {
		this.serverType = serverType;
		Log.d(LOGTAG, "serverType: "+serverType);
		return this;
	}	
	public ConfigFactory setArgs(String[] args) {
		this.args = args;
		return this;
	}
	/**
	 * Not normally needed - defaults to hostname
	 * @param machine
	 * @return
	 */
	public ConfigFactory setMachine(String machine) {
		this.machine = machine;
		return this;
	}
	
	/**
	 * 
	 * @param configClass
	 * @return
	 * @throws IllegalStateException if the config has already been created
	 */
	public final <X> X getConfig(Class<X> configClass) throws IllegalStateException {
		// try to avoid duplicate config loading
		if (Dep.has(configClass)) {
			String msg = "Duplicate call to ConfigFactory.getConfig() for "+configClass
					+" Use Dep.has() / Dep.get() for subsequent calls, or set(null) or use getConfigBuilder() if you need to recreate one.";
			if (strict) {
				throw new IllegalStateException(msg);
			} else {
				Log.w(LOGTAG, msg);
			}
		}
		try {
			ConfigBuilder cb = getConfigBuilder(configClass);
			X config = cb.get();
			assert config != null;
			// set Dep
			Dep.set(configClass, config);		
			// keep them?
			if (debug) {
				history.add(cb);
			}				
			return config;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	/**
	 * This is the core bit, which determines what files to look at (and in what order)
	 * @param configClass
	 * @return
	 */
	public List<File> getPropFileOptions(Class configClass) {
		String thingy = configClass.getSimpleName().toLowerCase().replace("config", "");
		Object[] options = new Object[] {
				thingy,
				appName,
				// live, local, test?
				serverType,
				// or in a logins file (which should not be in the git repo!), for passwords?
				"logins", 
				machine
		};		
		List<File> files = new ArrayList();
		// WW logins
		files.add(new File(FileUtils.getWinterwellDir(), "logins/logins."+appName+".properties"));
		files.add(new File(FileUtils.getWinterwellDir(), "logins/"+thingy+".properties"));		
		// the normal set of options
		for(Object option : options) {
			if (option==null) continue;		
			files.add(new File("config/"+option.toString().toLowerCase()+".properties"));
		}
		return files;
	}	

	public synchronized static ConfigFactory get() {
		if (Dep.has(ConfigFactory.class)) {
			ConfigFactory cf = Dep.get(ConfigFactory.class);
			return cf;
		}
		// Set a default (which has no app-name or Main args)
		ConfigFactory cf = new ConfigFactory();
		Dep.set(ConfigFactory.class, cf);
		return cf;
	}
	/**
	 * Override the default
	 * @param cf 
	 * @return cf for chaining
	 */
	public static ConfigFactory set(ConfigFactory cf) {
		Log.i(LOGTAG, "set global ConfigFactory "+cf);
		Dep.set(ConfigFactory.class, cf);
		return cf;
	}

	/**
	 * Most users should use {@link #getConfig(Class)}.
	 * 
	 * This allows the caller to add in more properties (which will take precedence)
	 * before calling get().
	 * @param configClass
	 * @return
	 */
	public ConfigBuilder getConfigBuilder(Class configClass) {
		if (configClass==null) throw new NullPointerException();
		try {
			// make a config object
			Object c = getConfigBuilder2_newConfigObject(configClass);
			final ConfigBuilder cb = new ConfigBuilder(c);
			cb.setDebug(debug);
			// system props
			cb.setFromSystemProperties(null);
			// check several config files
			List<File> propsPath = getPropFileOptions(configClass);
			for(File pp : propsPath) {
				cb.set(pp);
			}
			// args
			cb.setFromMain(args);
			if (debug) {
				history.add(cb);
			}
			return cb;
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	protected Object getConfigBuilder2_newConfigObject(Class configClass) throws Exception {
		try {
			return configClass.newInstance();
		} catch(Exception ex) {
//			NO need to log this. If the code below fails, it will throw an exception Log.d(LOGTAG, "1st try of new "+configClass.getSimpleName()+": "+ex);
			Constructor cons = configClass.getDeclaredConstructor();
			if ( ! cons.isAccessible()) cons.setAccessible(true);
			return cons.newInstance();	
		}			
	}

	public List<ConfigBuilder> getHistory() {
		return history;
	}
	
}
