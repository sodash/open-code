package winterwell.web.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * A very simple web-app using Jetty. Included here as an example.
 */
public class HelloJetty extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static void main(String[] args) throws Exception {
		// Create a Jetty server listening on port 8001
		Server server = new Server(8001);
		// Create a catch-all web-app context
		ServletContextHandler root = new ServletContextHandler(server, "/",
				ServletContextHandler.SESSIONS);
		// Add a servlet - this will respond to http://localhost:8001/hello
		HttpServlet servlet = new HelloJetty();
		String path = "/hello";
		root.addServlet(new ServletHolder(servlet), path);
		// Start the server
		server.start();
		// That's it - the server will stay running until stopped
	}

	/**
	 * Handle a web page request in the normal J2EE way - by over-riding methods
	 * in HttpServlet.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// Set mime type to html
		resp.setContentType("text/html");
		// Write a web page
		PrintWriter writer = resp.getWriter();
		writer.append("<html><body><h1>Hello World!</h1></body></html>");
		writer.close();
	}
}
