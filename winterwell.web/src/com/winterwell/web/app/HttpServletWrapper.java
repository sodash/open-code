package com.winterwell.web.app;

import java.io.IOException;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;

/**
 * Turn IServlet classes (which get created fresh for each request) into HttpServlet objects (which stick around).
 * Adds in resource clean-up and error handling.
 * @author daniel
 *
 */
public class HttpServletWrapper extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	Supplier<IServlet> factory;

	private boolean debug;
	
	public HttpServletWrapper setDebug(boolean debug) {
		this.debug = debug;
		return this;
	}
	
	public HttpServletWrapper(Supplier<IServlet> factory) {
		this.factory = factory;
	}

	/**
	 * Convenient way to make a wrapper
	 * @param servlet
	 */
	public HttpServletWrapper(Class<? extends IServlet> servlet) {
		this(() -> {
			try {
				return servlet.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw Utils.runtime(e);
			}
		});
	}

	@Override
	public String toString() {
		return "HttpServletWrapper[factory=" + factory + "]";
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			WebRequest state = new WebRequest(req, resp);			
			IServlet servlet = getServlet(state);
			// log everything?
			if (debug) {
				Log.d(servlet.getClass().getSimpleName(), state);
			}
			servlet.process(state);
		} catch (Throwable ex) {
			doCatch(ex, resp);
		} finally {
			WebRequest.close(req, resp);
		}
	}

	protected IServlet getServlet(WebRequest state) {
		 return factory.get();
	}

	public static void doCatch(Throwable ex, HttpServletResponse resp) {
		WebEx wex = WebUtils2.runtime(ex);
		if (wex.code >= 500) {
			Log.e("error."+wex.getClass().getSimpleName(), ex);
		} else {
			Log.w(wex.getClass().getSimpleName(), ex);
		}
		WebUtils2.sendError(wex.code, wex.getMessage(), resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
//	@Override
//	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		// TODO Auto-generated method stub
//		super.doHead(req, resp);
//	}
}
