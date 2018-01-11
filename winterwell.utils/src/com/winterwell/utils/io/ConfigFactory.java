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

	private String[] args;
	private String appName;
	String serverType;
	private String machine;
	private boolean debug = true;
	private final List<ConfigBuilder> history = new ArrayList();
	
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
	 */
	public void setServerType(String serverType) {
		this.serverType = serverType;
	}	
	public void setArgs(String[] args) {
		this.args = args;
	}
	public void setMachine(String machine) {
		this.machine = machine;
	}
	
	public final <X> X getConfig(Class<X> configClass) {
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
		Log.d("ConfigFactory", "set "+cf);
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
			Log.d("ConfigBuilder", "1st try of new "+configClass.getSimpleName()+": "+ex);
			Constructor cons = configClass.getDeclaredConstructor();
			if ( ! cons.isAccessible()) cons.setAccessible(true);
			return cons.newInstance();	
		}			
	}

	public List<ConfigBuilder> getHistory() {
		return history;
	}
	
}
