package com.winterwell.web.app;



import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

import javax.swing.text.StyledEditorKit.ForegroundAction;

import org.junit.Test;

import com.winterwell.bob.BobSettings;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.bob.tasks.ProcessTask;
import com.winterwell.bob.tasks.RSyncTask;
import com.winterwell.bob.tasks.RemoteTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.es.BuildESJavaClient;
import com.winterwell.utils.Environment;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.Warning;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.DnsUtils;

import com.winterwell.utils.io.LineReader;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.app.BuildWWAppBase;
import com.winterwell.web.app.ManifestServlet;
import com.winterwell.web.email.SMTPClient;
import com.winterwell.web.email.SimpleMessage;
import com.winterwell.youagain.client.BuildYouAgainJavaClient;

import jobs.BuildBob;
import jobs.BuildFlexiGson;
import jobs.BuildMaths;
import jobs.BuildDataLog;
import jobs.BuildDepot;
import jobs.BuildUtils;
import jobs.BuildWeb;
import jobs.BuildWinterwellProject;


/**
 * 
 */
public class PublishProjectTask extends BuildTask {
	
	/** typeOfPublish can be set to either 'test' or 'production' or 'local' (local=your machine only)
	 */
	protected KPubType typeOfPublish = null;
	
	// preClean can be set to 'clean' in order to sanitize a target before the files are synced to it
	protected String preClean =
			"";
//			"clean";
	
	protected String remoteUser = "winterwell";
	protected String remoteWebAppDir;
	protected File localWebAppDir;
	protected File jarFile;
	protected String bashScript;
	protected File localLib;

	/**
	 * frontend backend everything
	 */
	protected String codePart = "everything";

	protected boolean compile;
			
	public PublishProjectTask(String projectName, String remoteWebAppDir) throws Exception {
		this.projectName = projectName;
		this.remoteWebAppDir = remoteWebAppDir;
		// local
		this.localWebAppDir = FileUtils.getWorkingDirectory();
		localLib = new File(localWebAppDir,"tmp-lib");
		jarFile = new File(localLib, projectName+".jar");
		bashScript = "./publish-"+projectName+".sh";		
	}

	@Override
	public List<BuildTask> getDependencies() {
		List<BuildTask> deps = new ArrayList(Arrays.asList(
				new BuildUtils(),
				new BuildMaths(),
				new BuildBob(),
				new BuildWeb(),
				new BuildDataLog(), // This!
				new BuildDepot(),
				new BuildESJavaClient(),
				new BuildFlexiGson(),
				new BuildYouAgainJavaClient(),
				new BuildWWAppBase()
				));
		for (BuildTask buildTask : deps) {
			if (buildTask instanceof BuildWinterwellProject) {
				((BuildWinterwellProject) buildTask).setCompile(compile);
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
		
		{	// make version.properties		
			Log.report("publish","Uploading .properties...", Level.INFO);
			File localConfigDir = new File(localWebAppDir, "config");
			File creolePropertiesForSite = new File(localConfigDir, "version.properties");
			Properties props = new Properties();
			// fill in
			ManifestServlet.setVersionProperties(props);
			// dep info??
			for(BuildTask bt : getDependencies()) {
				try {
					if (bt instanceof BuildWinterwellProject) {
						File jar = ((BuildWinterwellProject) bt).getJar();
						Map<String, Object> manifest = JarTask.getManifest(jar);
						Map<String, Object> namedManifest = Containers.applyToKeys(manifest, k -> jar.getName()+"."+k);
						props.putAll(namedManifest);
					}
				} catch(Exception ex) {
					Log.e("publish", ex);
				}
			}
			// save
			BufferedWriter w = FileUtils.getWriter(creolePropertiesForSite);
			props.store(w, null);
			FileUtils.close(w);
		}

		// Find jars and move them into tmp-lib
		{
			EclipseClasspath ec = new EclipseClasspath(localWebAppDir);
			Set<File> jars = ec.getCollectedLibs();
			// Create local lib dir			
			localLib.mkdirs();
			assert localLib.isDirectory();
			// Ensure desired jars are present
			for (File jar : jars) {
				File localJar = new File(localLib, jar.getName());
				if (localJar.isFile() && localJar.lastModified() >= jar.lastModified()) {
					continue;
				}
				FileUtils.copy(jar, localJar);
			}
	//		// Remove unwanted jars
	//		for (File jar : localLib.listFiles()) {
	//			boolean found = false;
	//			for (File jar2 : jars) {
	//				if (jar.getName().equals(jar2.getName())) found = true;
	//			}
	//			if (!found) {
	//				// This was in the lib directory, but not in the classpath
	//				Log.w("publish", "Deleteing apparently unwanted file " + jar.getAbsolutePath());
	//				FileUtils.delete(jar);
	//			}
	//		}
			
			// WW jars
			Collection<? extends BuildTask> deps = getDependencies();
			for (BuildTask buildTask : deps) {
				if (buildTask instanceof BuildWinterwellProject) {
					File jar = ((BuildWinterwellProject) buildTask).getJar();
					FileUtils.copy(jar, localLib);
				}
			}			
			
			// This jar
			if (jarFile!=null) {
				JarTask jarTask = new JarTask(jarFile, new File(localWebAppDir, "bin"));
				jarTask.run();
				jarTask.close();
			}
		}
		
//		// Bash script which does the rsync work
		ProcessTask pubas = new ProcessTask(bashScript+" "+typeOfPublish + " "+codePart+" "+preClean);
		pubas.setEcho(true);
		pubas.run();
		System.out.println(pubas.getError());
		pubas.close();
		Log.d(pubas.getCommand(), pubas.getOutput());
		
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
