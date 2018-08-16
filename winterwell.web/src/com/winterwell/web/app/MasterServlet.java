package com.winterwell.web.app;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;

/**
*
*/
public class MasterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private boolean debug = true;

	private Map<String,Class> classForPrefix = new HashMap();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
	public MasterServlet() {
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	private IServlet newServlet(String servletName) throws Exception {
		Class klass = classForPrefix.get(servletName);
		if (klass==null) {
			throw new WebEx.E404(null, "No such servlet: "+servletName);
		}
		IServlet s = (IServlet) klass.newInstance();
		return s;
	}

	protected String servletNameFromPath(String path) {
		String[] pathBits = path.split("/");
		// NB: paths always start with a / so pathBits[0]=""
		return FileUtils.getBasename(pathBits[1]);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			WebRequest state = new WebRequest(req, resp);
			// everyone wants CORS
			WebUtils2.CORS(state, false);
			// servlet
			String path = state.getRequestPath();						
			String servletName = servletNameFromPath(path);		
			Thread.currentThread().setName("servlet: "+servletName);
			// make a servlet
			IServlet s = newServlet(servletName);					
			if (debug) {
				Log.d(servletName, state);
			}
			// do stuff
			s.process(state);
		} catch(Throwable ex) {
			HttpServletWrapper.doCatch(ex, resp);
		} finally {
			Thread ct = Thread.currentThread();
			ct.setName("...done: "+ct.getName());
			WebRequest.close(req, resp);
		}
	}

	public void setDebug(boolean b) {
		this.debug = b;
	}

	public void addServlet(String path, Class<? extends IServlet> klass) {
		Utils.check4null(path, klass);
		// / * is an annoyingly fiddly part of the standard J2EE -- lets make it irrelevant
		if (path.endsWith("*")) {
			path = path.substring(0, path.length()-1);
		} 
		if (path.endsWith("/")) {
			path = path.substring(0, path.length()-1);
		}
		// chop leading /
		if (path.startsWith("/")) {
			path = path.substring(1, path.length());
		}
		assert ! path.contains("/") : path;
		
		classForPrefix.put(path, klass);
	}

	@Override
	public String toString() {
		return "MasterServlet"+classForPrefix;
	}
}
