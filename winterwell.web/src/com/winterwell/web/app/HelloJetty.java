package com.winterwell.web.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;

/**
 * A very simple web-app using Jetty. Included here as an example.
 */
public class HelloJetty implements IServlet {

	private static final long serialVersionUID = 1L;

	public static void main(String[] args) throws Exception {
		// Create a Jetty server listening on port 8001
		JettyLauncher jl = new JettyLauncher(null, 8001);
		// Add a servlet - this will respond to http://localhost:8001/hello
		HttpServlet servlet = new HttpServletWrapper(HelloJetty::new);
		String path = "/hello";
		jl.addServlet("/hello", servlet);
		// Start the server
		jl.run();
		// That's it - the server will stay running until stopped
	}

	@Override
	public void process(WebRequest state) throws Exception {		
		// Set mime type to html
		HttpServletResponse resp = state.getResponse();
		resp.setContentType("text/html");
		// Write a web page
		PrintWriter writer = resp.getWriter();
		writer.append("<html><body><h1>Hello World!</h1></body></html>");
		writer.close();
	}
}
