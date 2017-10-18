package com.winterwell.web.app;

import java.io.File;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.TUnit;

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

	private void launchJetty() {
		Log.i("Go!");
		assert jl==null;
		jl = new JettyLauncher(new File("web-portal"), pc.port);
		jl.setup();		
		jl.addServlet("/*", new MasterHttpServlet());		
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();		
		
		Log.i("Running...");
	}

}
