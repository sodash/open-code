package com.winterwell.datalog.server;

import java.io.File;

import org.elasticsearch.node.Node;

import com.winterwell.utils.log.Log;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.es.ESUtils;
import com.winterwell.es.client.ESConfig;

public class DataExperimentServer {

	private static JettyLauncher jl;
	
	public static ESConfig esconfig;
	
	private static Node node;

	public static void main(String[] args) {
		assert jl==null;
		jl = new JettyLauncher(new File("web"), 8765);
		jl.setup();
		jl.addServlet("/project/*", new MasterHttpServlet());
		jl.addServlet("/*", new FileServlet().setListDir(true));
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();
		
		// start up ES
		initES();
		Log.i("Running...");
	}

	static void initES() {
		esconfig = new ESConfig();
		esconfig.port = 8766;
		File dataDir = new File("DataExperimentServer-data");
		node = ESUtils.startLocalES(esconfig.port, true, dataDir);	
	}

}
