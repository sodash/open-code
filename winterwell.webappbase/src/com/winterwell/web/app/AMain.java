package com.winterwell.web.app;

import java.io.File;
import java.util.function.Supplier;

import com.winterwell.datalog.DataLog;
import com.winterwell.es.XIdTypeAdapter;
import com.winterwell.gson.Gson;
import com.winterwell.gson.GsonBuilder;
import com.winterwell.gson.KLoopPolicy;
import com.winterwell.gson.StandardAdapters;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.data.XId;

/**
 * TODO can we refactor some more common code here?
 * 
 * @author daniel
 *
 * @param <ConfigType>
 */
public abstract class AMain<ConfigType extends ISiteConfig> {

//	public static final Time startTime = new Time();
	
	protected JettyLauncher jl;
	
	/**
	 * aka app name
	 */
	public static String appName;
	
	public static LogFile logFile;

	protected static boolean initFlag;

	protected ConfigType config;

	public static AMain main;

	public AMain() {
		this(FileUtils.getWorkingDirectory().getName().toLowerCase());
	}
	public AMain(String projectName) {
		this.appName = projectName;
	}

	public ConfigType getConfig() {
		return config;
	}
	
	public void doMain(String[] args) {
		logFile = new LogFile(new File(appName+".log"))
					.setLogRotation(TUnit.DAY.dt, 14);
		init(args);
		launchJetty();
	}

	/**
	 * Calls initConfig() then init2(config)
	 * @param args
	 */
	protected final void init(String[] args) {
		main = this;
		init2a_configFactory();
		config = init2_config(args);
		init2(config);
	}
	
	private void init2a_configFactory() {
		ConfigFactory cf = ConfigFactory.get();
		cf.setAppName(appName);
		KServerType serverType = AppUtils.getServerType(null);
		cf.setServerType(serverType.toString());
	}
	
	/**
	 * TODO refactor so all AMains use this (poss overriding it)
	 */
	protected void init3_gson() {
		Gson gson = new GsonBuilder()
		.setLenientReader(true)
		.registerTypeAdapter(Time.class, new StandardAdapters.TimeTypeAdapter())
		.registerTypeAdapter(XId.class, new XIdTypeAdapter())
		.serializeSpecialFloatingPointValues()
		.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
//		.setClassProperty(null)
		.setLoopPolicy(KLoopPolicy.QUIET_NULL)
		.create();
		Dep.set(Gson.class, gson);
	}

	
	public final void init() {
		init(new String[0]);
	}

	/**
	 * called after config has been loaded.
	 * This is the recommended method to override for custom init stuff
	 * @param config
	 */
	protected void init2(ConfigType config) {
		if (initFlag) return;
		initFlag = true;		
		// init DataLog
		DataLog.getImplementation();
		try {
			init3_emailer();			
		} catch(Throwable ex) {
			// compact whitespace => dont spew a big stacktrace, so we don't scare ourselves in dev
			Log.e("init", StrUtils.compactWhitespace(Printer.toString(ex, true)));
			// oh well, no emailer
		}		
		// TODO init3_gson();
	}

	protected Emailer init3_emailer() {
		if (Dep.has(Emailer.class)) return Dep.get(Emailer.class);		
		EmailConfig ec = AppUtils.getConfig(appName, EmailConfig.class, null);
		Log.i("init", "Emailer with config "+ec);
		LoginDetails ld = ec.getLoginDetails();
		if (ld == null) {
			Log.i("init", "No Emailer: no login details");
			return null;
		}
		Emailer emailer = new Emailer(ld);
		Dep.set(Emailer.class, emailer);
		return emailer;
	}
	
	/**
	 * Suggestion: use AppUtils.getConfig()
	 * @param args
	 * @return
	 */
	protected abstract ConfigType init2_config(String[] args);

	private void launchJetty() {
		Log.i("Go!");
		assert jl==null;
		jl = new JettyLauncher(getWebRootDir(), getPort());
		jl.setup();		
		
		addJettyServlets(jl);
				
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();		
		
		Log.i("Running...");
	}

	/**
	 * Override! This should read from config
	 * @return
	 */
	protected final int getPort() {
		return getConfig().getPort();
	}

	/**
	 * TODO move this into ISiteConfig
	 * Override!
	 * @return
	 */
	protected File getWebRootDir() {
		return new File("web");
	}

	/**
	 * Adds /manifest
	 *
	 * Override! (but do call super) to set e.g. /* -> Master servlet
	 * @param jl
	 */
	protected void addJettyServlets(JettyLauncher jl) {
		jl.addServlet("/manifest", new HttpServletWrapper(ManifestServlet::new));
		// NB: not "test" cos there's often a test directory, and nginx gets confused
		jl.addServlet("/testme/*", new HttpServletWrapper(TestmeServlet::new));
	}

}
