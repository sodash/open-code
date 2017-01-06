package com.winterwell.datalog.server;

import java.io.File;

import org.elasticsearch.node.Node;

import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.es.ESUtils;
import com.winterwell.es.client.ESConfig;

public class DataExperimentServer {

	private static JettyLauncher jl;
	
	public static ESConfig esconfig;
	
	static LogFile logfile = new LogFile().setLogRotation(TUnit.DAY.dt, 10);
	
	private static Node node;

	public static void main(String[] args) {
		Log.i("Go!");
		assert jl==null;
		jl = new JettyLauncher(new File("web"), 8765);
		jl.setup();
		jl.addServlet("/project/*", new MasterHttpServlet());
		jl.addServlet("/*", new FileServlet() {
			public void doGet(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) throws javax.servlet.ServletException ,java.io.IOException {
				if (WebUtils2.getRequestURL(req).contains("compare.html") && ! "uoivf78".equals(req.getParameter("key"))) {
					WebUtils2.sendError(401, "Please provide a valid key in the url", resp);
					return;
				}
				super.doGet(req, resp);
			};
			
		}.setListDir(true));
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
