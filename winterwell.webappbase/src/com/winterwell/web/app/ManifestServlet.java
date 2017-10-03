package com.winterwell.web.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.datalog.DataLogConfig;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Trio;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.SafeExecutor;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.ajax.JsonResponse;

/**
 * NB: Can be used directly or via {@link HttpServletWrapper}
 * @author daniel
 *
 */
public class ManifestServlet extends HttpServlet implements IServlet {

	private static final long serialVersionUID = 1L;


	public static final String PROPERTY_GIT_BRANCH = "branch";
	public static final String PROPERTY_GIT_COMMIT_ID = "lastCommitId";
	public static final String PROPERTY_GIT_COMMIT_INFO = "lastCommitInfo";
	public static final String PROPERTY_PUBLISH_DATE = "publishDate";


	private static volatile boolean initFlag;
	
	public ManifestServlet() {
		initManifest();
	}
	
	public static void initManifest() {
		if (initFlag) return;
		initFlag = true;
		// stick version info into ManifestServlet
		try {
			File f = new File("config/version.properties");
			if (f.isFile()) {
				configFiles.add(f);
			}
		} catch(Exception ex) {
			Log.e("manifest", ex);
		}		
	}
	
	/**
	 * Create version.properties
	 */
	public static void setVersionProperties(Properties props) {
		try {
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

		} catch (Throwable ex) {
			Log.e("debug", ex);
		}
	}

	static final Collection<File> configFiles = new ArraySet<>();
	
	public static void addConfigFiles(Collection<File> configFiles) {
		ManifestServlet.configFiles.addAll(configFiles);
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
	
	static List<Object> configs = new ArrayList();
	
	public static void addConfig(Object config) {
		configs.add(config);
	}
	
	public void process(WebRequest state) throws IOException {	
		
		ArrayMap cargo = new ArrayMap();
		// what did we load from?
		cargo.put("configFiles", configFiles);
		
		// server type
		cargo.put("serverType", AppUtils.getServerType(null));
				
		// what config did we pick up?
		// Screen for sensitive keys, e.g. passwords
		Map configsjson = new ArrayMap();		
		for(Object c : configs) {
			ArrayMap<String, Object> vs = new ArrayMap(Containers.objectAsMap(c));
			for(String k : vs.keySet()) {
				boolean protect = ConfigBuilder.protectPasswords(k);
				if (protect) vs.put(k, "****");
			}
			configsjson.put(c.getClass().getSimpleName(), vs);
		}
		cargo.put("config", configsjson);
		
		ArrayList repos = new ArrayList();
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
				// HACK
				String pubDate = versionProps.getProperty("publishDate");
				if (pubDate!=null) {
					cargo.put("version_published_date", new Time(pubDate).toString());
				}
			} catch(Exception ex) {
				cargo.put("error", ex);
			}
		}		
		
		Map<String,Object> manifests = getJarManifests();
		cargo.put(("jarManifests"), manifests);
		
		
		JsonResponse output = new JsonResponse(state, cargo);
		WebUtils2.sendJson(output, state);
	}

	/**
	 * Info about the jars
	 * @return
	 */
	private Map<String, Object> getJarManifests() {
		ConcurrentMap<String, Object> manifestFromJar = new ConcurrentHashMap();
		try {		
			File dir = new File(FileUtils.getWorkingDirectory(), "lib");
			ExecutorService pool = Executors.newFixedThreadPool(10);
			File[] files = dir.listFiles();
			for (File file : files) {
				pool.submit(() -> {
					Map<String, Object> manifest = JarTask.getManifest(file);
					// reduce down to avoid bloat
					ArrayMap smallMainfest = new ArrayMap();
					for(String k : new String[] {"Implementation-Version", "Packaging-Date", "Bundle-Version"}) {
						Object v = manifest.get(k);
						if (v!=null) smallMainfest.put(k, v);
					}					
					manifestFromJar.put(file.getName(), smallMainfest);
				});	
			}
			pool.shutdown();
			pool.awaitTermination(10, TimeUnit.SECONDS);			
		} catch(Throwable ex) {
			manifestFromJar.put("error", ex);
		}
		return manifestFromJar;
	}

	public static void addConfigBuilder(ConfigBuilder cb) {
		// TODO store more from cb
		for(Object s : cb.getSources()) {
			if (s instanceof File) {
				addConfigFiles(Arrays.asList((File)s));
			}
		}
	}
	
	
}
