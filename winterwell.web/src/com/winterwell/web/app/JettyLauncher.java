package com.winterwell.web.app;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;

class AdminServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final ServletContextHandler root;
	private final Server server;

	public AdminServlet(Server server, ServletContextHandler root) {
		this.server = server;
		this.root = root;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String url3 = WebUtils2.getRequestURL(req);
			if (!url3.startsWith("http://localhost"))
				throw new ServletException(
						"Security Breach: This can only be called from localhost");
			try {
				Log.report("SHUTDOWN REQUESTED! from " + url3 + " "
						+ req.getRemoteAddr());
				// root. destroy();
				server.destroy();
			} finally {
				Log.report("Shutdown requested from the web! Shutting down immediately.");
				System.exit(0);
			}
		} finally {
			WebRequest.close(req, resp);
		}
	}
}

class DummyLogger implements Logger {

	@Override
	public void debug(String arg0, Object... arg1) {
		return;
	}

	@Override
	public void debug(String arg0, Throwable arg1) {
		return;
	}

	@Override
	public void debug(Throwable arg0) {
		return;
	}

	@Override
	public Logger getLogger(String arg0) {
		return this;
	}

	@Override
	public String getName() {
		return "dummy";
	}

	@Override
	public void ignore(Throwable arg0) {
		return;
	}

	@Override
	public void info(String arg0, Object... arg1) {
		return;
	}

	@Override
	public void info(String arg0, Throwable arg1) {
		return;
	}

	@Override
	public void info(Throwable arg0) {
		return;
	}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public void setDebugEnabled(boolean arg0) {
		return;
	}

	@Override
	public void warn(String arg0, Object... arg1) {
		return;
	}

	@Override
	public void warn(String arg0, Throwable arg1) {
		return;
	}

	@Override
	public void warn(Throwable arg0) {
		return;
	}

	@Override
	public void debug(String arg0, long arg1) {	
	}

}

/**
 * Launch a Jetty server from code. Can load a web.xml, or be used as a
 * convenience for "manual" programmatic setup & launch.
 * 
 * @author daniel
 * 
 */
public class JettyLauncher {

	private static final String LOGTAG = "jetty";
	
	private boolean canShutdown = true;
	private boolean catchAllServletDefined;
	private boolean jettyLog;
	/**
	 * limit the server to ONE THREAD! For debugging only
	 */
	boolean oneThread;
	private final int port;

	private ServletContextHandler root;

	private Server server;

	/**
	 * The folder that is served up to anyone over http.
	 */
	private File webRootDir;
	File webXmlFile = null; //new File("web/WEB-INF/web.xml");

	/**
	 * 
	 * @param webAppDir
	 * 			  Can be null (=> use the working directory, but don't look for web.xml).<br>
	 *            Will look for the optional web.xml in the sub-dir WEB-INF.
	 *            with null.
	 * @param port
	 */
	public JettyLauncher(File webRootDir, int port) {
		if (webRootDir==null) {
			setWebXmlFile(null);
			webRootDir = FileUtils.getWorkingDirectory();
		}
		setWebRootDir(webRootDir);
		this.port = port;
	}
	
	public void setWebRootDir(File webRootDir) {
		this.webRootDir = webRootDir;
		assert server==null;
	}
	
	public int getPort() {
		return port;
	}

	/**
	 * A servlet to accept die requests TODO something nicer
	 * 
	 * @param srvr
	 * @param root
	 */
	private void addAdminServlet(Server srvr, ServletContextHandler rt) {
		AdminServlet sh = new AdminServlet(srvr, rt);
		addServlet("/jettyadmin", sh);
	}

	/**
	 * Add a servlet. Should be called after setup() and before run().
	 * 
	 * @param path This should usually end with a * to catch the slug bit, e.g. "/myservlet*" will get "/myservlet/foo"
	 * but "/myservlet" would not!
	 * @param servlet
	 */
	public void addServlet(String path, HttpServlet servlet) {
		if (root==null) {
			setup();
		}
		Log.d(LOGTAG, "add servlet "+path+" = "+servlet);
		if (path.equals("/") || path.equals("/*")) {
			catchAllServletDefined = true;
		}		
		root.addServlet(new ServletHolder(servlet), path);
	}

	public ServletContextHandler getRootContext() {
		return root;
	}

	/**
	 * @return the Jetty Web Server
	 */
	public Server getServer() {
		return server;
	}

	/**
	 * Calls setup first
	 */
	public void run() {
		setup();
		// Add a catch-all web server
		if ( ! catchAllServletDefined) {
			FileServlet fs = new FileServlet();
			fs.setBaseDir(webRootDir);
			String path = "/";
			root.addServlet(new ServletHolder(fs), path);
			Printer.out("	Adding " + fs.getClass().getName() + " at " + path);
		}
		// Special admin servlet?
		if (canShutdown) {
			addAdminServlet(server, root);
		}
		// GO!!!
		try {
			Log.i(LOGTAG, "Starting Jetty server on port "+port);
			server.start();
		} catch (Exception e) {
			Log.report(e);
			throw Utils.runtime(e);
		}
	}

	private void run2_webXml() {
		Log.d(LOGTAG, "Processing web.xml file "+webXmlFile);
		String webXml = FileUtils.read(webXmlFile);
		// WebXmlConfiguration jettyConfig = new WebXmlConfiguration();
		List<String> servletClasses = WebUtils.extractXmlTags("servlet-class",
				webXml, false);
		List<String> urlPatterns = WebUtils.extractXmlTags("url-pattern",
				webXml, false);
		assert servletClasses.size() == urlPatterns.size();
		for (int i = 0; i < servletClasses.size(); i++) {
			String clazz = servletClasses.get(i);
			String path = urlPatterns.get(i);
			try {
				Class.forName(clazz);
			} catch (ClassNotFoundException e1) {
				Log.report("	Skipping non-existent class: " + clazz + " at "
						+ path);
				continue;
			}
			try {
				HttpServlet servlet = (HttpServlet) Class.forName(clazz)
						.newInstance();
				addServlet(path, servlet);
			} catch (Exception e) {
				Log.report("Problem with " + clazz + " at " + path);
				Log.report(e);
			}
		}
	}

	public void runOrDie() {
		try {
			run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param canShutdown
	 *            true by default. If false, the shutdown url is not installed.
	 */
	public void setCanShutdown(boolean canShutdown) {
		this.canShutdown = canShutdown;
	}

	/**
	 * true to use jetty logging.
	 * 
	 * @param jettyLog
	 */
	public void setJettyLog(boolean jettyLog) {
		assert server == null; // must be called before server construction
		this.jettyLog = jettyLog;
	}

	/**
	 * Limit the server to ONE THREAD! For debugging only
	 */
	public void setOneThreadOnly(boolean oneThread) {
		this.oneThread = oneThread;
	}

	/**
	 * It is safe to call this several times. Subsequent calls will be ignored.
	 */
	public void setup() {
		if (server != null)
			return;		
		Log.d(LOGTAG, "setup");
		// no logging?
		if ( ! jettyLog) {
			Log.d(LOGTAG, "Disable Jetty logging");
			org.eclipse.jetty.util.log.Log.setLog(new DummyLogger());
		}
		server = new Server();

		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });
		
		if (oneThread) {
			Log.e(LOGTAG, "TODO oneThread support");
//			QueuedThreadPool threadPool = new QueuedThreadPool();
//			threadPool.setName("JettyWebServer");
//			Log.d(LOGTAG, "Single-thread pool");
//			threadPool.setMaxThreads(1);
//			Handler handler;
//			server.setHandler(handler);			
		}
		root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);

		// Switch off jsessionid-in-the-url badness
		Set<SessionTrackingMode> sessionModes = root.getSessionHandler().getEffectiveSessionTrackingModes();
		Set<SessionTrackingMode> sessionTrackingModes = new ArraySet();
		root.getSessionHandler().setSessionTrackingModes(sessionTrackingModes);
//				.setSessionIdPathParameterName("none");		
		root.setResourceBase(webRootDir.getAbsolutePath());

		// Attempted fix for Egan's transfer bug, Doesn't work :(
		root.setMaxFormContentSize(1000000);

		// Add servlets from web.xml
		if (webXmlFile != null) {
			if ( ! webXmlFile.exists()) {
				throw new IllegalArgumentException(webXmlFile.getAbsolutePath()+ " does not exist.");
			}
			run2_webXml();
		}
	}

	/**
	 * The web.xml file to use.
	 * 
	 * @param webXmlFile
	 *            Can be null for "do not use web.xml for it is a sucky way to
	 *            configure things"
	 */
	public void setWebXmlFile(File webXmlFile) {
		assert server == null;
		this.webXmlFile = webXmlFile;
	}
}
