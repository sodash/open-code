package com.winterwell.web.app;

import java.io.File;
import java.util.function.Supplier;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.TUnit;

/**
 * TODO can we refactor some more common code here?
 * 
 * @author daniel
 *
 * @param <ConfigType>
 */
public class AMain<ConfigType> {

	private static JettyLauncher jl;
	
	public static String projectName = FileUtils.getWorkingDirectory().getName();
	
	public static LogFile logFile = new LogFile(new File(projectName+".log"))
									.setLogRotation(TUnit.DAY.dt, 14);

	protected void doMain(String[] args) {
		ConfigType config = initConfig(args);
		init(config);
		launchJetty();
	}

	protected void init(ConfigType config) {
		// TODO Auto-generated method stub
		
	}

	protected ConfigType initConfig(String[] args) {
		// TODO Auto-generated method stub
		return null;
	}

	private void launchJetty() {
		Log.i("Go!");
		assert jl==null;
		jl = new JettyLauncher(getWebRootDir(), getPort());
		jl.setup();		
		
		jl.addServlet("/manifest", new HttpServletWrapper(ManifestServlet::new));
		addJettyServlets(jl);
				
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();		
		
		Log.i("Running...");
	}

	/**
	 * Override!
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
	 * Override to set e.g. /* -> Master servlet
	 * @param jl
	 */
	protected void addJettyServlets(JettyLauncher jl) {
	}

}
