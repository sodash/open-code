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
import com.winterwell.datalog.IStatStorage;
import com.winterwell.es.ESUtils;
import com.winterwell.es.client.ESConfig;

public class DataLogServer {

	private static JettyLauncher jl;

	static IStatStorage storage;
	
	public static LogFile logFile;
	
	public static Node node;

	public static DataLogSettings settings;

	public static void main(String[] args) {
		settings = ArgsParser.getConfig(new DataLogSettings(), args, new File("config/datalog.properties"), null);

		logFile = new LogFile(DataLogServer.settings.logFile)
					// keep 8 weeks of 1 week log files ??revise this??
					.setLogRotation(TUnit.WEEK.dt, 8);
		
		Log.i("Go!");
		// storage layer (eg ES)
		initStorage();
		assert jl==null;
		jl = new JettyLauncher(new File("web"), settings.port);
		jl.setup();
		jl.addServlet("/*", new MasterHttpServlet());
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();

		Log.i("Running...");
	}

	private static void initStorage() {
		try {
			storage = settings.storageClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw Utils.runtime(e);
		}
	}


}
