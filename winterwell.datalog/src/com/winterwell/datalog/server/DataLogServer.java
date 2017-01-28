package com.winterwell.datalog.server;

import java.io.File;

import org.elasticsearch.node.Node;

import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.es.ESUtils;
import com.winterwell.es.client.ESConfig;

public class DataLogServer {

	private static JettyLauncher jl;
	
	public static ESConfig esconfig;
	
	public static LogFile logFile;
	
	public static Node node;

	public static DataLogSettings settings;

	public static void main(String[] args) {
		settings = ArgsParser.getConfig(new DataLogSettings(), args, new File("config/datalog.properties"), null);
		logFile = new LogFile(DataLogServer.settings.logFile)
					// keep 8 weeks of 1 week log files ??revise this??
					.setLogRotation(TUnit.WEEK.dt, 8);

		Log.i("Go!");
		assert jl==null;
		jl = new JettyLauncher(new File("web"), settings.port);
		jl.setup();
		jl.addServlet("/*", new MasterHttpServlet());
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();
		
		// start up ES
		try {
			initES();
		} catch(Exception ex) {
			Log.e("init", ex);
			// swallow! At least we'll have log files
		}
		Log.i("Running...");
	}

	static void initES() {
		esconfig = new ESConfig();
		esconfig.port = 8766;
		File dataDir = new File("DataLogServer-data");
		node = ESUtils.startLocalES(esconfig.port, true, dataDir);	
	}

}
