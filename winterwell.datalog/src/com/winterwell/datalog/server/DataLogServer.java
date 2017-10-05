package com.winterwell.datalog.server;

import java.io.File;

import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.DataLogConfig;
import com.winterwell.datalog.IDataLogAdmin;
import com.winterwell.utils.Dep;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.TUnit;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.web.app.ManifestServlet;
import com.winterwell.youagain.client.YouAgainClient;

/**
 * Runs this for a standalone DataLog micro-service server.
 * @author daniel
 *
 */
public class DataLogServer {

	private static JettyLauncher jl;
	
	public static LogFile logFile;

	public static DataLogConfig settings;

	public static void main(String[] args) {						
		init(args);
		
		Log.i("Go!");
		assert jl==null;
		jl = new JettyLauncher(new File("web"), settings.port);
		jl.setup();
		jl.addServlet("/*", new MasterHttpServlet());
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();

		Log.i("Running...");
	}

	private static void init(String[] args) {
		ConfigBuilder cb = new ConfigBuilder(new DataLogConfig());
		settings = cb
				.setDebug(true)
				.setFromSystemProperties(null)
				.set(new File("config/datalog.properties"))
				.set(new File("config/datalog.local.properties"))
				.setFromMain(args)
				.get();
		assert settings != null;
		Dep.set(DataLogConfig.class, settings);
		// set the config
		DataLog.setConfig(settings);		
		ManifestServlet.addConfig(settings);
		ManifestServlet.addConfigBuilder(cb);
		
		logFile = new LogFile(DataLogServer.settings.logFile)
				// keep 8 weeks of 1 week log files ??revise this??
				.setLogRotation(TUnit.DAY.dt, 10);

		// app=datalog for login
		YouAgainClient yac = new YouAgainClient("datalog");
		Dep.set(YouAgainClient.class, yac);
				
		// register the tracking event
		IDataLogAdmin admin = DataLog.getAdmin();
		admin.registerDataspace(DataLog.getDataspace());
		admin.registerEventType(DataLog.getDataspace(), TrackingPixelServlet.DATALOG_EVENT_TYPE);
	}


}
