package com.winterwell.web.app.build;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.winterwell.bob.wwjobs.BuildDataLog;
import com.winterwell.bob.wwjobs.BuildDepot;
import com.winterwell.bob.wwjobs.BuildFlexiGson;
import com.winterwell.bob.wwjobs.BuildMaths;
import com.winterwell.bob.wwjobs.BuildUtils;
import com.winterwell.bob.wwjobs.BuildWeb;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;
import com.winterwell.bob.wwjobs.WWDependencyTask;
import com.winterwell.utils.Environment;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.web.email.SimpleMessage;


/**
 * See also wwappbase.js/project-publisher.sh + projectname
 */
public class PublishProjectTask extends BuildTask {
	
	/**
	 * If true, pass the --notests flag into the bash script
	 * 
	 */
	protected boolean notests;
	
	/** typeOfPublish can be set to either 'test' or 'production' or 'local' (local=your machine only)
	 */
	protected KPubType typeOfPublish = null;
	
	
	protected String remoteUser = "winterwell";
	protected String remoteWebAppDir;
	protected File localWebAppDir;
//	protected File jarFile;
	/**
	 * wwappbase.js/project-publisher.sh + projectname + test|production|local frontend|
	 */
	protected String bashScript;
	/**
	 * tmp-lib
	 */
	protected File localLib;

	protected boolean compile;

	private LogFile logfile = new LogFile();

	private boolean noPublishJustBuild;

	private BuildTask buildProjectTask;

	protected KComponent component = KComponent.everything;
	
	public void setBuildProjectTask(BuildTask buildProjectTask) {
		this.buildProjectTask = buildProjectTask;
	}
			
	public PublishProjectTask setNoPublishJustBuild(boolean noPublishJustBuild) {
		this.noPublishJustBuild = noPublishJustBuild;
		return this;
	}
	
	public PublishProjectTask(String projectName) {
		this(projectName, "/home/winterwell/"+projectName);
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
		Utils.check4null(projectName);
		this.projectName = projectName;
		this.remoteWebAppDir = remoteWebAppDir; // could be null for local
		// local
		this.localWebAppDir = localWebAppDir;
		localLib = new File(localWebAppDir,"tmp-lib");
//		jarFile = new File(localLib, projectName+".jar");
		bashScript = new File(FileUtils.getWinterwellDir(), "wwappbase.js/project-publisher.sh")+" "+projectName;
	}

	/**
	 * BuildX + std WW libs
	 */
	@Override
	public List<BuildTask> getDependencies() {		
		// the builder for this project?
		if (buildProjectTask==null) {
			// guess the name is s/PublishX/BuildX/
			String pubTaskName = getClass().getName();
			String buildTaskName = pubTaskName.replace("Publish", "Build");
			try {
				Class btc = Class.forName(buildTaskName);
				buildProjectTask = (BuildTask) btc.newInstance();				
			} catch(Throwable ohwell) {
				Log.d(LOGTAG, "No build task for "+buildTaskName+": "+ohwell);
			}
		}
		if (buildProjectTask != null) {
			
			// FIXME nobble the compile
			// Weird bug: BuildSoGiveApp would run from command line bob, but fail from Eclipse junit
			// ?! Nov 2018
			((BuildWinterwellProject) buildProjectTask).setCompile(false);
			
//			((BuildWinterwellProject) buildProjectTask).setErrorHandler(IErrorHandler.forPolicy(KErrorPolicy.REPORT));

			ArrayList deps = new ArrayList();
			deps.add(buildProjectTask);
			return deps;
		}
		
		// no builder found :( -- list std ww projects
		// All the WW libs
		List<BuildTask> deps = new ArrayList(Arrays.asList(
				new BuildUtils(),
				new BuildMaths(),
				new WWDependencyTask("winterwell.bob", "jobs.BuildBob"),
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
		ProcessTask pubas = new ProcessTask(getProjectPublishBashCommand());
		pubas.setEcho(true);
		pubas.run();
		if ( ! Utils.isBlank(pubas.getError())) {
			Log.w(pubas.getCommand(), pubas.getError());
		}
		pubas.close();
		Log.d(pubas.getCommand(), pubas.getOutput());
	}

	
	private String getProjectPublishBashCommand() {	
		String cmd = bashScript+" "+typeOfPublish +" "+ component +(notests?" notests":"");
		return cmd;
	}

	/**
	 * Find jars and move them into tmp-lib
	 */
	void collectJars() {
		EclipseClasspath ec = new EclipseClasspath(localWebAppDir);
		ec.setIncludeProjectJars(true);
		Set<File> jars = ec.getCollectedLibs();
		Log.d(LOGTAG, "Dependency graph:\n"
				+Printer.toString(ec.getDepsFor(), "\n", " <- "));
		// Create local lib dir			
		localLib.mkdirs();
		assert localLib.isDirectory();
		// Ensure desired jars are present
		for (File jar : jars) {
			File localJar = new File(localLib, jar.getName()).getAbsoluteFile();
			
			// check versions and pick which one to keep?
			if (localJar.isFile()) {
				File newJar = JarTask.pickNewerVersion(localJar, jar);
				if (newJar.equals(localJar)) continue;
			}
			FileUtils.copy(jar, localJar);
		}
		
		// Remove unwanted jars? -- no too dangerous		
		
		// This jar
		if (buildProjectTask instanceof BuildWinterwellProject) {
			File jar = ((BuildWinterwellProject) buildProjectTask).getJar();
			FileUtils.copy(jar, localLib);
		}
		
		System.out.println("Jars: "+Printer.toString(Arrays.asList(localLib.list()), "\n"));
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
