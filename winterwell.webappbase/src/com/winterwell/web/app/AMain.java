package com.winterwell.web.app;

import java.io.File;
import java.util.function.Supplier;

import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.web.LoginDetails;

/**
 * TODO can we refactor some more common code here?
 * 
 * @author daniel
 *
 * @param <ConfigType>
 */
public abstract class AMain<ConfigType> {

//	public static final Time startTime = new Time();
	
	protected static JettyLauncher jl;
	
	/**
	 * aka app name
	 */
	public static String projectName;
	
	public static LogFile logFile;

	protected static boolean initFlag;

	protected ConfigType config;

	public AMain() {
		this(FileUtils.getWorkingDirectory().getName().toLowerCase());
	}
	public AMain(String projectName) {
		this.projectName = projectName;
	}

	public ConfigType getConfig() {
		return config;
	}
	
	public void doMain(String[] args) {
		logFile = new LogFile(new File(projectName+".log"))
					.setLogRotation(TUnit.DAY.dt, 14);
		init(args);
		launchJetty();
	}

	/**
	 * Calls initConfig() then init(config)
	 * @param args
	 */
	protected final void init(String[] args) {
		config = init2_config(args);
		init2(config);
	}
	public final void init() {
		init(new String[0]);
	}

	protected void init2(ConfigType config) {
		if (initFlag) return;
		initFlag = true;		
		try {
			init3_emailer();
		} catch(Throwable ex) {
			// compact whitespace => dont spew a big stacktrace, so we don't scare ourselves in dev
			Log.e("init", StrUtils.compactWhitespace(Printer.toString(ex, true)));
			// oh well, no emailer
		}
	}

	protected void init3_emailer() {
		if (Dep.has(Emailer.class)) return;
		EmailConfig ec = AppUtils.getConfig(projectName, new EmailConfig(), null);
		Emailer emailer = new Emailer(ec.getLoginDetails());
		Dep.set(Emailer.class, emailer);		
	}
	
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
	protected int getPort() {
		return 80;
	}

	/**
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
	}

}
