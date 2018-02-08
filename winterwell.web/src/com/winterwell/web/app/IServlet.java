package com.winterwell.web.app;

/**
 * Unlike HttpServlet, a fresh IServlet should be made for each request - so they can use fields.
 * @author daniel
 *
 */
public interface IServlet {

	public void process(WebRequest state) throws Exception;

}
