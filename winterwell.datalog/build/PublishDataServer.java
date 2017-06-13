


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

import com.winterwell.bob.BobSettings;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.RSyncTask;
import com.winterwell.bob.tasks.RemoteTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.es.BuildESJavaClient;
import com.winterwell.maths.datastorage.DataTable;
import com.winterwell.utils.Environment;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.Warning;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.DnsUtils;

import com.winterwell.utils.io.LineReader;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.email.SMTPClient;
import com.winterwell.web.email.SimpleMessage;

import jobs.BuildBob;
import jobs.BuildFlexiGson;
import jobs.BuildMaths;
import jobs.BuildDataLog;
import jobs.BuildUtils;
import jobs.BuildWeb;
import jobs.BuildWinterwellProject;


/**
 * FIXME rsync is making sub-dirs :(
 */
public class PublishDataServer extends BuildTask {

	String server = "lg.good-loop.com"; // datalog.soda.sh
	String remoteUser;
	private String remoteWebAppDir;
	private File localWebAppDir;
			
	public PublishDataServer() throws Exception {
		this.remoteUser = "winterwell";
		this.remoteWebAppDir = "/home/winterwell/"+server;
		// local
		this.localWebAppDir = FileUtils.getWorkingDirectory();
	}

	@Override
	public Collection<? extends BuildTask> getDependencies() {
		return Arrays.asList(
				new BuildUtils(),
				new BuildMaths(),
				new BuildBob(),
				new BuildWeb(),
				new BuildDataLog(), // This!
				new BuildESJavaClient(),
				new BuildFlexiGson(),
				new BuildWWAppBase()
				);
	}

	private void doUploadProperties(Object timestampCode) throws IOException {
		// Copy up creole.properties and version.properties
		Log.report("publish","Uploading .properties...", Level.INFO);
		File localConfigDir = new File(localWebAppDir, "config");
		{	// create the version properties
			File creolePropertiesForSite = new File(localConfigDir, "version.properties");
			Properties props = creolePropertiesForSite.exists()? FileUtils.loadProperties(creolePropertiesForSite)
								: new Properties();
			// set the publish time
			props.setProperty("publishDate", ""+System.currentTimeMillis());
			// set info on the git branch
			String branch = GitTask.getGitBranch(null);
			props.setProperty("branch", branch);
			// ...and commit IDs
			try {
				Map<String, Object> info = GitTask.getLastCommitInfo(localWebAppDir);
				props.setProperty("lastCommitId", (String)info.get("hash"));
				props.setProperty("lastCommitInfo", StrUtils.compactWhitespace(XStreamUtils.serialiseToXml(info)));
			} catch(Exception ex) {
				// oh well;
				Log.d("git.info", ex);
			}

			// the timestamp code
			props.setProperty("timecode", ""+timestampCode);

			// save
			BufferedWriter w = FileUtils.getWriter(creolePropertiesForSite);
			props.store(w, null);
			FileUtils.close(w);
		}

		// Don't upload any local properties
		File localProps = new File(localConfigDir, "local.properties");
		File localPropsOff = new File(localConfigDir, "local.props.off");
		if (localProps.exists()) {
			FileUtils.move(localProps, localPropsOff);
		}
		
		// rsync the directory
		try {
			assert localConfigDir.isDirectory() : localConfigDir;
			Log.d("publish","Sending config dir files: "+Printer.toString(localConfigDir.list()));
			String remoteConfig = remoteUser+"@"+server+":"+remoteWebAppDir+"/config";
			RSyncTask task = new RSyncTask(localConfigDir.getAbsolutePath()+"/", remoteConfig, true);
			task.run();		
			
		} finally {
			// put local-props back
			if (localPropsOff.exists()) {
				FileUtils.move(localPropsOff, localProps);
			}
		}		
		
		// How to restart??
	}

	@Override
	protected void doTask() throws Exception {
		// Setup file paths
		// Check that we are running from a plausible dir:
		if (! localWebAppDir.exists() || ! new File(localWebAppDir, "bin").isDirectory()
			|| !new File(localWebAppDir,"web").exists()) {
			throw new IOException("Not in the expected directory! dir="+FileUtils.getWorkingDirectory());
		}
		Log.i("publish", "Publishing to "+server+":"+ remoteWebAppDir);
		// What's going on?
		Environment.get().push(BobSettings.VERBOSE, true);

		// TIMESTAMP code to avoid caching issues: epoch-seconds, mod 10000 to shorten it
		String timestampCode = "" + ((System.currentTimeMillis()/1000) % 10000);

		{	// copy all the properties files
			doUploadProperties(timestampCode);
		}

		// Copy up the code		
		// TODO copy up all the jars needed
		{
			EclipseClasspath ec = new EclipseClasspath(localWebAppDir);
			Set<File> jars = ec.getCollectedLibs();
			System.out.println(jars); // Why no mixpanel getting copied??
			// Create local lib dir
			File localLib = new File(localWebAppDir,"tmp-lib");
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
			
			// Do the rsync!
			String from = localLib.getAbsolutePath();
			String dest = rsyncDest("lib");			
			RSyncTask task = new RSyncTask(from, dest, true).setDirToDir();
			task.run();
			task.close();
			System.out.println(task.getOutput());
		}
		{	// web
			// Rsync code with delete=true, so we get rid of old html templates
			// ??This is a bit wasteful, but I'm afraid of what delete might do in the more general /web/static directory ^Dan
			RSyncTask rsyncCode = new RSyncTask(
					new File(localWebAppDir,"web").getAbsolutePath(),
					rsyncDest("web"), true);
			rsyncCode.setDirToDir();
			rsyncCode.run();
			String out = rsyncCode.getOutput();
			rsyncCode.close();
		}
	}

	private String rsyncDest(String dir) {
//		if ( ! dir.endsWith("/")) dir += "/";
		return remoteUser+"@"+server+ ":" + new File(remoteWebAppDir, dir).getAbsolutePath();
	}


}
