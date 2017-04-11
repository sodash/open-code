package com.winterwell.datalog.server;

import java.io.File;

import org.elasticsearch.node.Node;

import com.winterwell.utils.Utils;
import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.JettyLauncher;
import com.google.common.util.concurrent.ListenableFuture;
import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.ESStorage;
import com.winterwell.datalog.IDataLog;
import com.winterwell.datalog.IDataLogAdmin;
import com.winterwell.datalog.IDataLogStorage;
import com.winterwell.datalog.StatConfig;
import com.winterwell.datalog.DataLogImpl;
import com.winterwell.es.ESUtils;
import com.winterwell.es.client.ESConfig;

/**
 * Runs this for a standalone DataLog micro-service server.
 * @author daniel
 *
 */
public class DataLogServer {

	private static JettyLauncher jl;
	
	public static LogFile logFile;

	public static DataLogSettings settings;

	public static void main(String[] args) {
		settings = ArgsParser.getConfig(new DataLogSettings(), args, new File("config/datalog.properties"), null);
		assert settings != null;
		
		logFile = new LogFile(DataLogServer.settings.logFile)
					// keep 8 weeks of 1 week log files ??revise this??
					.setLogRotation(TUnit.WEEK.dt, 8);
		
		Log.i("Go!");
		// storage layer (eg ES)
		init();
		assert jl==null;
		jl = new JettyLauncher(new File("web"), settings.port);
		jl.setup();
		jl.addServlet("/*", new MasterHttpServlet());
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();

		Log.i("Running...");
	}

	private static void init() {
		IDataLog dli = DataLog.getImplementation();
		StatConfig myConfig = (StatConfig) dli.getConfig();
		myConfig.storageClass = settings.storageClass;
		DataLog.setConfig(myConfig);
		
		// register the tracking event
		IDataLogAdmin admin = DataLog.getAdmin();
		admin.registerDataspace(DataLog.getDataspace());
		admin.registerEventType(DataLog.getDataspace(), TrackingPixelServlet.DATALOG_EVENT_TYPE);
	}


}
