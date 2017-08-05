package com.winterwell.web.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.winterwell.bob.tasks.GitTask;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Trio;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.ajax.JsonResponse;


public class ManifestServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;


	public static final String PROPERTY_GIT_BRANCH = "branch";
	public static final String PROPERTY_GIT_COMMIT_ID = "lastCommitId";
	public static final String PROPERTY_GIT_COMMIT_INFO = "lastCommitInfo";
	public static final String PROPERTY_PUBLISH_DATE = "publishDate";
	
	/**
	 * Create version.properties
	 */
	public static void saveVersionProperties(File configDir) {
		try {
			File creolePropertiesForSite = new File(configDir, "version.properties");
			Properties props = creolePropertiesForSite.exists()? FileUtils.loadProperties(creolePropertiesForSite) : new Properties();
			// set the publish time
			props.setProperty(PROPERTY_PUBLISH_DATE, ""+System.currentTimeMillis());
			// TIMESTAMP code to avoid caching issues: epoch-seconds, mod 10000 to shorten it
			String timestampCode = "" + ((System.currentTimeMillis()/1000) % 10000);
			props.setProperty("timecode", ""+timestampCode);
			
			// set info on the git branch
			String branch = GitTask.getGitBranch(null);
			props.setProperty(PROPERTY_GIT_BRANCH, branch);
			// ...and commit IDs
			for(String repo : new String[]{"open-code"}) {
				File repodir = new File(FileUtils.getWinterwellDir(), repo);
				try {
					Map<String, Object> info = GitTask.getLastCommitInfo(repodir);
					props.setProperty(PROPERTY_GIT_COMMIT_ID+"."+repo, (String)info.get("hash"));
					props.setProperty(PROPERTY_GIT_COMMIT_INFO+"."+repo, StrUtils.compactWhitespace(XStreamUtils.serialiseToXml(info)));
	//				TODO props.setProperty(Creole.PROPERTY_GIT_BRANCH+"."+repo, (String)info.get("branch"));
				} catch(Exception ex) {
					// oh well;
					Log.d("git.info", ex);
				}
				try {
					String rbranch = GitTask.getGitBranch(repodir);
					props.setProperty(PROPERTY_GIT_BRANCH+"."+repo, rbranch);
				} catch(Throwable ex) {
					// oh well
					Log.d("git.info", ex);
				}
			}
	
			// Who did the push?
			try {
				props.setProperty("origin", WebUtils2.hostname());
			} catch(Exception ex) {
				// oh well
			}
	
			// save
			BufferedWriter w = FileUtils.getWriter(creolePropertiesForSite);
			props.store(w, null);
			FileUtils.close(w);
		} catch (Throwable ex) {
			Log.e("debug", ex);
		}
	}

	static Collection<File> configFiles;
	
	public static void setConfigFiles(Collection<File> configFiles) {
		ManifestServlet.configFiles = configFiles;
	}

	private static Time startTime = new Time();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			WebRequest state = new WebRequest(null, req, resp);
			process(state);
		} finally {
			WebRequest.close(req, resp);
		}
	}
	
	public void process(WebRequest state) throws IOException {	
		
		ArrayMap cargo = new ArrayMap();
		// what did we load from?
		cargo.put("configFiles", configFiles);
		// 
//		String pd = props.getProperty(Creole.PROPERTY_PUBLISH_DATE);
//		cargo.put(DB2Json.pubDate, pd);
//		String branch = props.getProperty(Creole.PROPERTY_GIT_BRANCH);
//		cargo.put("branch", branch);
		ArrayList repos = new ArrayList();
//		HtmlTable tbl = new HtmlTable("Repo", "Date", "Commit-message", "Author", "hash");
//		// Extra Branch Info, if we have it
		for(String repo : new String[]{"adserver","flexi-gson"}) {
//			String rhash = props.getProperty(Creole.PROPERTY_GIT_COMMIT_ID+"."+repo);
//			String rinfo = props.getProperty(Creole.PROPERTY_GIT_COMMIT_INFO+"."+repo);
//			String rbranch = props.getProperty(Creole.PROPERTY_GIT_BRANCH+"."+repo);
//			Map info = new ArrayMap("repo", repo, "hash", rhash, "branch", rbranch);
//			if (rinfo!=null) {
//				Map _info = XStreamUtils.serialiseFromXml(rinfo);
//				info.putAll(_info);
//			}		
//			repos.add(info);
		}
		cargo.put("git-repos", repos);

		// Where was the push done from?
//			String origin = props.getProperty("origin");
//			if (origin!=null) page.append("<p>Origin: "+origin+"</p>\n");
//			
//			page.append("<p>Running: "+DateFilter.prettyDate(CreoleMain.startTime)+"</p>\n");
//			page.append("<p>Base Directory: "+Statics.getWebAppDir()+"</p>\n");
//			page.append("<p>JTwitter version: "+Twitter.version+"</p>\n");
		cargo.put("hostname", WebUtils2.hostname());
		
		String uptime = TimeUtils.toString(startTime.dt(new Time()));
		cargo.put("uptime", uptime);
		
		// origin -- maybe
		try {
			Properties props = Dep.get(Properties.class);
			String origin = props.getProperty("origin");
			if (origin == null) origin = "";
			cargo.put("origin", origin);
		} catch(Exception ex) {
			// oh well
		}
		
		File creolePropertiesForSite = new File("config", "version.properties");
		if (creolePropertiesForSite.exists()) {
			try {
				Properties versionProps = FileUtils.loadProperties(creolePropertiesForSite);
//				ArrayMap vps = new ArrayMap(versionProps);				
				cargo.put("version", versionProps);
			} catch(Exception ex) {
				cargo.put("error", ex);
			}
		}

		
		JsonResponse output = new JsonResponse(state, cargo);
		WebUtils2.sendJson(output, state);
	}
	
	
}
