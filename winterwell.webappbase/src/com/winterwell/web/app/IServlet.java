package com.winterwell.web.app;

import java.io.IOException;

import javax.servlet.http.HttpServlet;

/**
 * Unlike HttpServlet, a fresh IServlet should be made for each request - so they can use fields.
 * @author daniel
 *
 */
public interface IServlet {

	public void process(WebRequest state) throws Exception;

}
