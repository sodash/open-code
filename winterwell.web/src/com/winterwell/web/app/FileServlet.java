package com.winterwell.web.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import com.winterwell.web.WebPage;

/**
 * A basic bare-bones web server.
 * 
 * @author daniel
 * 
 */
public class FileServlet extends HttpServlet implements IServlet {
	private static final long serialVersionUID = 1L;
	
	@Override
	public String toString() {
		return "FileServlet[" + baseDir + "]";
	}

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
//			Log.v("file","Served "+file+" as "+mime );
		} finally {
			FileUtils.close(in);
			FileUtils.close(state);
		}

	}

	/**
	 * Can be set explicitly, or will try to guess this from the ServletConfig.
	 */
	File baseDir;

	/**
	 * @deprecated Better to set a dir!
	 */
	public FileServlet() {
		super();
	}

	public FileServlet(File baseDir) {
		super();
		setBaseDir(baseDir);
	}

	boolean listDir = true;
	private boolean chopServlet;
	public FileServlet setChopServlet(boolean chopServlet) {
		this.chopServlet = chopServlet;
		return this;
	}
	
	/**
	 * If true (default), provide a dynamic index
	 * @param listDir
	 * @return 
	 */
	public FileServlet setListDir(boolean listDir) {
		this.listDir = listDir;
		return this;
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
		WebRequest request = new WebRequest(this, req, resp);
		try {
			File file = getFile(req);
			file = file.getCanonicalFile();
			// Serve it
			if (file.isDirectory()) {
				serveDirectory(file, request);
			} else {
				doFile(file, request);
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
		// chop leading path bit?		
		if (chopServlet && pi.startsWith(req.getServletPath())) {
			pi = pi.substring(req.getServletPath().length());
		}
		if (pi.isEmpty()) pi = "/";
		// security check -- no hacking with .. or ;
		if ( ! FileUtils.isSafe(pi)) {
			throw new SecurityException("Illegal filename: " + pi);
		}
		File file = new File(baseDir, pi);
		// Handle a directory: send index.html if it exists
		if (!file.isDirectory())
			return file;
		File index = new File(file, "index.html");
		if (index.exists()) return index;
		index = new File(file, "index.htm");
		if (index.exists()) return index;		
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

	protected void serveDirectory(File file, WebRequest request)
			throws IOException {
		if ( ! listDir) {
			throw new SecurityException();
		}
		request.getResponse().setContentType(WebUtils.MIME_TYPE_HTML);		
		WebPage page = new WebPage();
//		WebPage page = new WebPage();
		page.append("<h1>"+file+"</h1>\n");
		HtmlTable table = new HtmlTable(Arrays.asList("Filename"));
		List<File> files = Arrays.asList(file.listFiles());
		Collections.sort(files);
		for (File f : files) {
			String path = FileUtils.getRelativePath(f, baseDir);
			table.addRow("<a href='/"+path+"'>" + f.getName() + "</a>");
		}
		String html = table.toHTML();
		html = WebUtils.stripScripts(html);
		page.append(html);
		request.setPage(page);
		request.sendPage();
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
	 * @return 
	 */
	public FileServlet setBaseDir(File baseDir) {
		this.baseDir = baseDir;
		return this;
	}

	@Override
	public void process(WebRequest state) throws Exception {
		doGet(state.getRequest(), state.getResponse());
	}

}
