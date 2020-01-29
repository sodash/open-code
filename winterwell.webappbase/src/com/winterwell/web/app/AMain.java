package com.winterwell.web.app;

import java.io.File;

import com.winterwell.datalog.DataLog;
import com.winterwell.es.IESRouter;
import com.winterwell.es.StdESRouter;
import com.winterwell.es.XIdTypeAdapter;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.gson.Gson;
import com.winterwell.gson.GsonBuilder;
import com.winterwell.gson.KLoopPolicy;
import com.winterwell.gson.StandardAdapters;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.WebEx;
import com.winterwell.web.data.XId;
import com.winterwell.youagain.client.App2AppAuthClient;
import com.winterwell.youagain.client.AuthToken;
import com.winterwell.youagain.client.YouAgainClient;

/**
 * Common main/init code. To use:
 * 
 * 1. Create subclass. You should probably override
 * 	{@link #addJettyServlets(JettyLauncher)}
 * and maybe
 * {@link #doMain2()}
 * 
 * 2. Add a main method to the subclass:
 * <pre><code>
 * public static void main(String[] args) {
		MyMain amain = new MyMain();
		amain.doMain(args);
	}
 *  </code></pre>
 * 
 * That's it for basic use :)
 * 
 * @author daniel
 *
 * @param <ConfigType>
 */
public abstract class AMain<ConfigType extends ISiteConfig> {
	
	protected JettyLauncher jl;
	
	/**
	 * aka app name
	 */
	@Deprecated // access via the non-static getAppName() for preference
	public static String appName;
	
	/**
	 * @return e.g. "myapp" Note: Use "myapp.mydomain.com" with YouAgain
	 */
	public String getAppNameLocal() {
		return appName;
	}
	
	public static LogFile logFile;
	public static LogFile auditlogFile;

	protected static boolean initFlag;

	protected ConfigType config;

	protected Class<ConfigType> configType;

	protected volatile boolean pleaseStop;

	private Thread mainLoopThread;
	
	private volatile boolean readyFlag;


	public static AMain main;

	/**
	 * @deprecated This will guess the appName from the folder -- better to sepcify it. 
	 */
	public AMain() {
		this(FileUtils.getWorkingDirectory().getName().toLowerCase(), null);
	}
	
	public AMain(String projectName, Class<ConfigType> configType) {
		this.appName = projectName;
		this.configType = configType;
	}

	public ConfigType getConfig() {
		return config;
	}
	
	/**
	 * Call this to run your app!
	 * 
	 * NB: this should return after starting up Jetty. i.e. it does not sit in a forever loop.
	 * 
	 * Normally leave this alone and override {@link #doMain2()} and {@link #doMainLoop()}
	 * 
	 * @param args
	 */
	public void doMain(String[] args) {
		// logfile before log config??! Is that right?
		// Try to use the "logs" subdirectory - but use the app root if that's impossible.
		File logDir = new File("logs");
		boolean useSubDir = true;
		if (!logDir.exists()) {
			// Try to create it - use dir if successful
			useSubDir = logDir.mkdir();
		} else if (!logDir.isDirectory()) {
			// Exists but isn't a directory - don't use dir
			useSubDir = false;
		}
		File logLocation = new File((useSubDir ? "logs/" : "") + getAppNameLocal() + ".log");  
		logFile = new LogFile(logLocation).setLogRotation(TUnit.DAY.dt, 14);
		// also add a never-rotates! audit log for important audit trail info only (ie stuff tagged "audit"		
		File auditlogLocation = new File((useSubDir ? "logs/" : "") + getAppNameLocal() + ".audit");
		auditlogFile = new LogFile(auditlogLocation).setFilter(r -> "audit".equals(r.tag));
		
		try {
			assert "foo".contains("bar");
			Log.e("run", "Running Java WITHOUT assertions - please use the -ea flag!");
		} catch(AssertionError e) {
			// ok
		}
		init(args);
		launchJetty();
		doMain2();
		// loop?
		mainLoopThread = new Thread(getClass().getSimpleName()+".doMainLoop") {
			@Override
			public void run() {
				while( ! pleaseStop) {
					try {
						doMainLoop();
					} catch(Throwable ex) {
						Log.e(appName, ex);
						if (pleaseStop) return;
						// pause a moment
						Utils.sleep(100);
						// loop again... 
						// NB: use stop() to stop
					}
				}
			}
		};
		mainLoopThread.start();
		// ready
		readyFlag = true;
	}
	
	/**
	 * @return true once {@link #doMain(String[])} has completed 
	 */
	public boolean isReady() {
		return readyFlag;
	}

	/**
	 * Override to do other main stuff.
	 * This method must return. To implement an infinite loop -- use doMainLoop();
	 */
	protected void doMain2() {
		
	}
	
	/**
	 * Overide to do a loop. Use {@link #stop()} to stop.
	 * Any exceptions are caught, and the loop is restarted.
	 */
	protected void doMainLoop() throws Exception {
		
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

	/**
	 * Convenience for {@link #init(String[])}, for use in unit tests.
	 * 
	 * This will initialise things like Gson and the database.
	 * It won't start Jetty.
	 */
	public final void init() {
		init(new String[0]);
	}
	
	/**
	 * Stop the app!
	 * Sets {@link #pleaseStop} to true, requests Jetty stop, and interrupts the {@link #mainLoopThread} if used.
	 */
	public void stop() {
		pleaseStop = true;
		if (jl != null) {
			jl.stop();
			jl = null;
		}
		// break the loop, if we are in one
		if (mainLoopThread!=null && mainLoopThread.isAlive()) {
			mainLoopThread.interrupt();
		}
	}

	/**
	 * called after config has been loaded.
	 * This is the recommended method to override for custom init stuff.
	 * 
	 * This base method does:
	 *  - App YA auth
	 *  - DataLog
	 *  - Emailer, via {@link #init3_emailer()}
	 *  
	 *  NOT (YET):
	 *  - TODO ES via {@link #init3_ES()}
	 *  - TODO gson via {@link #init3_gson()}
	 *  
	 * @param config
	 */
	protected void init2(ConfigType config) {
		if (initFlag) return;
		initFlag = true;		
		// init DataLog
		DataLog.getImplementation();
		// YA - manual
//		init3_youAgain();
		// app auth - manual
//		init3_appAuth(config);
		// emailer
		try {
			init3_emailer();			
		} catch(Throwable ex) {
			// compact whitespace => dont spew a big stacktrace, so we don't scare ourselves in dev
			Log.e("init", StrUtils.compactWhitespace(Printer.toString(ex, true)));
			// oh well, no emailer
		}		
		// TODO init3_gson();
		// TODO init3_ES();
	}

	/**
	 * Not called by base class! Call in your apps overide, AFTER 
	 * {@link #init3_youAgain()}
	 * 
	 * Dep.set AuthToken, if we have login info
	 * @param config2
	 * @return token or null
	 */
	protected AuthToken init3_appAuth(ConfigType config2) {
		// idempotent
		if (Dep.has(AuthToken.class)) {
			return Dep.get(AuthToken.class);
		}
		YouAgainClient yac = Dep.get(YouAgainClient.class);
		String appAuthPassword = config2.getAppAuthPassword();
		String appAuthName = getAppNameLocal()+".good-loop.com"; // TODO get domain from the config		
		String appAuthJWT = config2.getAppAuthJWT();
		// use JWT if we have it
		if ( ! Utils.isBlank(appAuthJWT)) {
			AuthToken token = new AuthToken(appAuthJWT);
			Log.d(appName, "AuthToken set from config.getAppAuthJWT "+token.getXId());
			return Dep.set(AuthToken.class, token);
		}
		AuthToken token = yac.loadLocal(new XId(appAuthName+"@app"));
		if (token != null) {
			Log.d(appName, "AuthToken set from loadLocal .token folder "+token.getXId());
			return Dep.set(AuthToken.class, token);
		}
		if (Utils.isBlank(appAuthName) || Utils.isBlank(appAuthPassword)) {
			Log.d(getAppNameLocal(), ":( Expected config to provide appAuthJWT for connecting with YouAgain. Missing app-auth details: app-name: "+appAuthName+" p: "+appAuthPassword+" from "+config2.getClass());
			return null;
		}
		App2AppAuthClient a2a = yac.appAuth();
		try {
			token = a2a.getIdentityTokenFromYA(appAuthName, appAuthPassword);
			Log.d(appName, "AuthToken fetched by name+password "+token.getXId());
		} catch(Exception wex) {
			token = a2a.registerIdentityTokenWithYA(appAuthName, appAuthPassword);
			Log.d(appName, "AuthToken registered with name+password "+token.getXId());
		}
		return Dep.set(AuthToken.class, token);
	}

	/**
	 * Init the ES client and router (NOT any schemas/mappings)
	 * 
	 * Use {@link AppUtils#initESIndices(com.winterwell.data.KStatus[], Class[])}
	 * {@link AppUtils#initESMappings(com.winterwell.data.KStatus[], Class[], java.util.Map)}
	 */
	protected void init3_ES() {
		// config
		ESConfig esc = ConfigFactory.get().getConfig(ESConfig.class);
		// client
		ESHttpClient esjc = new ESHttpClient(esc);
		Dep.setIfAbsent(ESHttpClient.class, esjc);
		assert config != null;
		// Is the config the IESRouter?
		if (config instanceof IESRouter) {
			Dep.setIfAbsent(IESRouter.class, (IESRouter) config);
		} else {
			// nope - use a default
			Dep.setIfAbsent(IESRouter.class, new StdESRouter());
		}
	}
	
	protected Emailer init3_emailer() {
		if (Dep.has(Emailer.class)) {
			return Dep.get(Emailer.class);		
		}
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
	
	protected void init3_youAgain() {
		// idempotent
		if (Dep.has(YouAgainClient.class)) {
			return; // Dep.get(YouAgainClient.class);
		}
		// app=datalog for login
		YouAgainClient yac = new YouAgainClient(getAppNameLocal());
		Dep.set(YouAgainClient.class, yac);				
	}

	
	/**
	 * Suggestion: use AppUtils.getConfig()
	 * @param args
	 * @return
	 */
	protected ConfigType init2_config(String[] args) {
		Class ct = configType;
		if (ct==null) {
			ct = BasicSiteConfig.class;
			Log.w(getAppNameLocal(), "No ConfigType given - using "+ct.getSimpleName());
		}
		return (ConfigType) AppUtils.getConfig(getAppNameLocal(), ct, args);
	}

	private void launchJetty() {
		try {
			Log.i("Go!");
			assert jl==null;
			jl = new JettyLauncher(getWebRootDir(), getPort());
			jl.setup();		
			// no sessions!
			WebRequest.setStateless(true);
			addJettyServlets(jl);
					
			Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
			jl.run();		
			
			Log.i("Running...");
		} catch (Throwable ex) {
			// make sure it gets logged
			Log.e("launchJetty", ex);
			throw Utils.runtime(ex);
		}
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
	 * Adds /manifest and /testme
	 *
	 * Override! (but do call super) to set e.g. /* -> Master servlet
	 * Recommended code:
	 * 
	 * <pre><code>
		super.addJettyServlets(jl);
		MasterServlet ms = jl.addMasterServlet();	
		ms.add(MyServlet)
		</code></pre>
		
	 * @param jl
	 */
	protected void addJettyServlets(JettyLauncher jl) {
		jl.addServlet("/manifest", new HttpServletWrapper(ManifestServlet::new));
		// NB: not "test" cos there's often a test directory, and nginx gets confused
		jl.addServlet("/testme/*", new HttpServletWrapper(TestmeServlet::new));
	}

}
