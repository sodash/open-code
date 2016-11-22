package com.winterwell.web.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.HtmlTable;

/**
 * A basic bare-bones web server.
 * 
 * @author daniel
 * 
 */
public class FileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Serve a file up over HTTP, closing the connection afterwards.
	 * @dependency mime-util.jar to sniff mime-types
	 * 
	 * @param file
	 * @param resp
	 * @throws IOException
	 */
	public static void serveFile(File file, WebRequest state)
			throws IOException {
		FileInputStream in = null;
		HttpServletResponse resp = state.getResponse();
		try {
			if (!file.exists()) {
				resp.sendError(404,
						"File does not exist " + file.getAbsolutePath());
				return;
			}
			if (!file.canRead()) {
				resp.sendError(404, "Cannot read file " + file);
				return;
			}
			// CORS? Assuming you've done security elsewhere
			WebUtils2.CORS(state, true);
			in = new FileInputStream(file);
			// Respond
			String mime = WebUtils2.getMimeType(file);
			resp.setContentType(mime);
			// Set file name
//			resp.setHeader("Content-Disposition", "attachment; filename=FILENAME"); // ??
			ServletOutputStream out = resp.getOutputStream();
			// Copy out
			// Note: Can cause issues if someone edits the
			// file whilst we are piping it out
			FileUtils.copy(in, out);
			Log.v("file","Served "+file+" as "+mime );
		} finally {
			FileUtils.close(in);
			FileUtils.close(resp.getOutputStream());
		}

	}

	/**
	 * Can be set explicitly, or will try to guess this from the ServletConfig.
	 */
	private File baseDir;

	public FileServlet() {
		super();
	}

	public FileServlet(File baseDir) {
		super();
		setBaseDir(baseDir);
	}

	/**
	 * By default, uses {@link #serveFile(File, HttpServletResponse)} to send
	 * the file out. Subclasses can override.
	 * 
	 * @param file
	 * @param resp
	 * @throws IOException
	 */
	protected void doFile(File file, WebRequest resp)
			throws IOException {
		Log.v("file", file);
		serveFile(file, resp);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
		try {
			File file = getFile(req);
			// Serve it
			if (file.isDirectory()) {
				String servletPath = req.getServletPath();
				serveDirectory(servletPath, file, resp);
			} else {
				doFile(file, new WebRequest(this, req, resp));
			}
		} catch (Throwable e) {
			Log.report("file", e, Level.SEVERE);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	protected File getFile(HttpServletRequest req) {
		// Which file?
		String pi = req.getRequestURI();
		if (pi == null) {
			pi = "";
		}
		// convert e.g. "%20" -> " "
		pi = WebUtils.urlDecode(pi);
		// security check -- no hacking with .. or ;
		if ( ! FileUtils.isSafe(pi))
			throw new SecurityException("Illegal filename: " + pi);
		File file = new File(baseDir, pi);
		// Handle a directory: send index.html if it exists
		if (!file.isDirectory())
			return file;
		File index = new File(file, "index.html");
		if (index.exists())
			return index;
		index = new File(file, "index.htm");
		if (index.exists())
			return index;
		// oh well
		return file;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if (baseDir == null) {
			baseDir = WebUtils2.getWebAppBaseDir(config);
		}
	}

	protected void serveDirectory(String servletPath, File file, HttpServletResponse resp)
			throws IOException {
		resp.setContentType(WebUtils.MIME_TYPE_HTML);
		BufferedWriter writer = FileUtils.getWriter(resp.getOutputStream());
		HtmlTable table = new HtmlTable(Arrays.asList("Filename"));
		for (File f : file.listFiles()) {
			String path = FileUtils.getRelativePath(f, baseDir);
			table.addRow("<a href='/"+path+"'>" + f.getName() + "</a>");
		}
		String html = table.toHTML();
		html = WebUtils.stripScripts(html);
		writer.append(html);
		writer.close();
	}

	/**
	 * E.g. if the path for this servlet is /resources/*<br>
	 * and the baseDir is /home/me/web<br>
	 * then a url of http://myserver.com/resources/myfile.txt<br>
	 * would map to the file<br>
	 * /home/me/web/myfile.txt<br>
	 * Simples
	 * 
	 * @param baseDir
	 */
	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

}
