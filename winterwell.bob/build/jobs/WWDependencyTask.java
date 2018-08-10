package jobs;

import java.io.File;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.web.FakeBrowser;

/**
 * Hack for BuildWWProject, which handles project-not-on-classpath
 * by downloading the jar 
 * @author daniel
 *
 */
public class WWDependencyTask extends BuildWinterwellProject {

	private String builderClass;

	public WWDependencyTask(String projectName, String builderClass) {
		super(projectName);
		this.builderClass = builderClass;
	}

	@Override
	public void doTask() throws Exception {
		// Do NOT run the super
		// build?
		BuildWinterwellProject builder = null;
		if (builderClass!=null) {			
			try {
				Class<?> clazz = Class.forName(builderClass);
				builder = (BuildWinterwellProject) clazz.newInstance();
			} catch(Throwable ex) {
				Log.w(LOGTAG, "Cannot run local BuildTask "+builderClass);
			}			
		}
		if (builder!=null) {
			builder.run();
			jarFile = builder.getJar();
			return;
		}
		
		// download jar
		FakeBrowser fb = new FakeBrowser();
		File jar = fb.getFile("https://www.winterwell.com/software/downloads/"+projectName+".jar");
//		String remoteJar = "/home/winterwell/public-software/"+projectName+".jar";
//		SCPTask scp = new SCPTask("winterwell@winterwell.com", remoteJar, getJar());
//		// this is online at: https://www.winterwell.com/software/downloads
//		scp.setMkdirTask(false);
//		scp.runInThread();
		
		getJar().getParentFile().mkdirs();
		FileUtils.move(jar, getJar());
		Log.i(LOGTAG, "Downloaded jar "+getJar());
	}

}
