package com.winterwell.web.app.build;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.winterwell.bob.BobSettings;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.bob.tasks.MakeVersionPropertiesTask;
import com.winterwell.bob.tasks.ProcessTask;
import com.winterwell.utils.Environment;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.IFn;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.web.email.SimpleMessage;

import com.winterwell.bob.wwjobs.BuildBob;
import com.winterwell.bob.wwjobs.BuildDataLog;
import com.winterwell.bob.wwjobs.BuildDepot;
import com.winterwell.bob.wwjobs.BuildFlexiGson;
import com.winterwell.bob.wwjobs.BuildMaths;
import com.winterwell.bob.wwjobs.BuildUtils;
import com.winterwell.bob.wwjobs.BuildWeb;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
import com.winterwell.bob.wwjobs.WWDependencyTask;


/**
 * 
 */
public class PublishProjectTask extends BuildTask {
	
	/** typeOfPublish can be set to either 'test' or 'production' or 'local' (local=your machine only)
	 */
	protected KPubType typeOfPublish = null;
	
	// preClean is no longer supported
	protected String preClean =
			"";
//			"clean";
	
	protected String remoteUser = "winterwell";
	protected String remoteWebAppDir;
	protected File localWebAppDir;
//	protected File jarFile;
	/**
	 * wwappbase.js/project-publisher.sh + projectname
	 */
	protected String bashScript;
	/**
	 * tmp-lib
	 */
	protected File localLib;

	/**
	 * frontend backend everything
	 */
	protected String codePart = "everything";

	protected boolean compile;

	private LogFile logfile;

	private boolean noPublishJustBuild;
			
	public PublishProjectTask setNoPublishJustBuild(boolean noPublishJustBuild) {
		this.noPublishJustBuild = noPublishJustBuild;
		return this;
	}
	
	/**
	 * 
	 * @param projectName
	 * @param remoteWebAppDir  
	 * @throws Exception
	 */
	public PublishProjectTask(String projectName, String remoteWebAppDir) {
		this(projectName, remoteWebAppDir, FileUtils.getWorkingDirectory());
	}
	
	public PublishProjectTask(String projectName, String remoteWebAppDir, File localWebAppDir) {
		Utils.check4null(projectName, remoteWebAppDir);
		this.projectName = projectName;
		this.remoteWebAppDir = remoteWebAppDir;
		// local
		this.localWebAppDir = localWebAppDir;
		logfile = new LogFile();
		localLib = new File(localWebAppDir,"tmp-lib");
//		jarFile = new File(localLib, projectName+".jar");
		bashScript = new File(FileUtils.getWinterwellDir(), "wwappbase.js/project-publisher.sh")+" "+projectName;
	}

	@Override
	public List<BuildTask> getDependencies() {		
		// the builder for this project?
		String pubTaskName = getClass().getName();
		String buildTaskName = pubTaskName.replace("Publish", "Build");
		try {
			Class btc = Class.forName(buildTaskName);
			BuildTask bt = (BuildTask) btc.newInstance();
			ArrayList deps = new ArrayList();
			deps.add(bt);
			return deps;
		} catch(Throwable ohwell) {
			Log.d(LOGTAG, "No build task for "+buildTaskName+": "+ohwell);
		}
		
		// no builder found -- list std ww projects
		// All the WW libs
		List<BuildTask> deps = new ArrayList(Arrays.asList(
				new BuildUtils(),
				new BuildMaths(),
				new BuildBob(),
				new BuildWeb(),
				new BuildDataLog(),
				new BuildDepot(),				
				
				// these might not be on the classpath
				new WWDependencyTask("jtwitter", "winterwell.jtwitter.BuildJTwitter"),
				new WWDependencyTask("winterwell.webappbase", "com.winterwell.web.app.BuildWWAppBase"),
				new WWDependencyTask("youagain-java-client", "com.winterwell.youagain.client.BuildYouAgainJavaClient"),
				new WWDependencyTask("elasticsearch-java-client", "com.winterwell.es.BuildESJavaClient"),
				new BuildFlexiGson()
				));
		for (BuildTask buildTask : deps) {
			if (buildTask instanceof BuildWinterwellProject) {
				((BuildWinterwellProject) buildTask).setCompile(compile);
				// don't scp??
				((BuildWinterwellProject) buildTask).setScpToWW(false);
			}
		}
		
		return deps;
	}

	@Override
	protected void doTask() throws Exception {
		assert typeOfPublish!=null : "Set typeOfPublish to test or production!"; 
		if (typeOfPublish==KPubType.production && GuiUtils.isInteractive()) {
			boolean ok = GuiUtils.confirm(
				"Are you sure you want to push "+GitTask.getGitBranch(localWebAppDir)+" to production?");
			if ( ! ok) throw new FailureException();			
		}
		// Setup file paths
		// Check that we are running from a plausible dir:
		if ( ! localWebAppDir.exists()) { 
			throw new IOException("Not in the expected directory! dir="+FileUtils.getWorkingDirectory()+" but no "+localWebAppDir);
		}
		if ( ! new File(localWebAppDir, ".classpath").exists()) { 
			throw new IOException("Not in the expected directory! dir="+FileUtils.getWorkingDirectory()+" but no .classpath");
		}

		Log.i("publish", "Publishing to "+typeOfPublish+":"+ remoteWebAppDir);
		// What's going on?
		Environment.get().push(BobSettings.VERBOSE, true);		
		
		// make version.properties					
		MakeVersionPropertiesTask mvpt = new MakeVersionPropertiesTask().setAppDir(localWebAppDir);
		Properties props = new Properties();
		mvpt.setProperties(props);
		mvpt.run();
			
		// Find jars and move them into tmp-lib
		collectJars();
		
		if (noPublishJustBuild) {
			return;
		}
		
//		// Bash script which does the rsync work
		if (typeOfPublish== KPubType.local) {
			Log.i(LOGTAG, "local -- no publish step.");
			return;
		}
		ProcessTask pubas = new ProcessTask(bashScript+" "+typeOfPublish + " "+codePart+" "+preClean);
		pubas.setEcho(true);
		pubas.run();
		if ( ! Utils.isBlank(pubas.getError())) {
			Log.w(pubas.getCommand(), pubas.getError());
		}
		pubas.close();
		Log.d(pubas.getCommand(), pubas.getOutput());
		
	}

	/**
	 * Find jars and move them into tmp-lib
	 */
	void collectJars() {
		EclipseClasspath ec = new EclipseClasspath(localWebAppDir);
		Set<File> jars = ec.getCollectedLibs();
		// Create local lib dir			
		localLib.mkdirs();
		assert localLib.isDirectory();
		// Ensure desired jars are present
		for (File jar : jars) {
			File localJar = new File(localLib, jar.getName()).getAbsoluteFile();
			if (localJar.isFile() && localJar.lastModified() >= jar.lastModified()) {
				continue;
			}
			FileUtils.copy(jar, localJar);
		}
		
		// Remove unwanted jars? -- no too dangerous
		
		// WW jars - by project
		List<String> projects = ec.getReferencedProjects();
		IFn<String, File> epf = ec.getProjectFinder();
		for (String project : projects) {
			File pdir = epf.apply(project);
			if (pdir != null && pdir.isDirectory()) {
				BuildWinterwellProject bwp = new BuildWinterwellProject(pdir, project);
				File jar = bwp.getJar();
				if (jar.isFile()) {
					FileUtils.copy(jar, localLib);
				}
			} else {
				Log.d(LOGTAG, "Could not find Eclipse project "+project);
			}
		}
		
		// This jar -- done by BuildX, in deps above
	}
	
	protected void doSendEmail(String tos) {
		HackyEmailer he = new HackyEmailer();
		he.init();
		List<String> to = StrUtils.split(tos);
		String gitLine = "";
		String branch = "";
		try {
			File repodir = localWebAppDir;
			Map<String, Object> info = GitTask.getLastCommitInfo(repodir);
			gitLine = info.get("author")+": "+info.get("subject");
			String b = GitTask.getGitBranch(repodir).trim();
			if ( ! "master".equals(b)) {
				branch = "(branch: "+b+") ";
			}
		} catch(Exception ex) {
			// oh well
			System.out.println(ex);
		}
		
		SimpleMessage email = new SimpleMessage(he.getFrom(), to.get(0), 
				"CodeBot: Publishing *"+projectName+"* "+branch+"to the *"+typeOfPublish+"* server", 
				"Fresh code -- uploading now... :)\n\n"
				+gitLine
				);
		for (int i = 1; i < to.size(); i++) {
			email.addCC(to.get(i));
		}
		he.send(email);
		he.close();				
	}
	
	
	protected String projectName;

}
