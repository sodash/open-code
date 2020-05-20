package com.winterwell.bob.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.IFn;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

/**
 * TODO find the dir for a WW Eclipse or node project.
 * This is a place for WW specific hacks
 * TODO use this in e.g. EclipseClassPath
 * @author daniel
 *
 */
public class WinterwellProjectFinder implements IFn<String, File> {

	/**
	 * @return null on failure
	 */
	@Override
	public File apply(String _projectName) {
		List<File> possDirs = new ArrayList();
		// are we in the project dir?
		if (FileUtils.getWorkingDirectory().getName().equals(_projectName)) {
			possDirs.add(FileUtils.getWorkingDirectory());
		}
		try {
			// TODO this will fail on a "strange" computer as it uses winterwell-home :(
			EclipseClasspath ec = new EclipseClasspath(FileUtils.getWorkingDirectory());
			String pname = ec.getProjectName();
			if (_projectName.equals(pname)) {
				possDirs.add(FileUtils.getWorkingDirectory());
			}
		} catch(Exception ex) {
			// oh well
		}
		try {
			File wdir = FileUtils.getWinterwellDir();
			possDirs.add(new File(wdir, "open-code/"+_projectName));
			possDirs.add(new File(wdir, "code/"+_projectName));
			possDirs.add(new File(wdir, _projectName));
			possDirs.add(new File(wdir, "bobwarehouse/"+_projectName));
			possDirs.add(new File(wdir, "bobwarehouse/open-code/"+_projectName));
//			possDirs.add(new File(wdir, "egbot")); // hack to allow egbot publish, otherwise getting this error: com.winterwell.utils.FailureException: Could not find project directory for home Tried [/home/irina/winterwell/open-code/home, /home/irina/winterwell/code/home, /home/irina/winterwell/home] @com.winterwell.bob.tasks.WinterwellProjectFinder.apply(WinterwellProjectFinder.java:49)
		} catch(Exception ex) {
			// no WINTERWELL_HOME
			Log.w("BuildWinterwellProject", "No WINTERWELL_HOME found "+ex);			
		}		
		File pdir = FileUtils.or(possDirs);
		if (pdir==null) {
			Log.e("BuildWinterwellProject", "Could not find project directory for "+_projectName+" Tried "+possDirs);
			return null;
		}
		return pdir;
	}

	/**
	 * HACK for deploying WW libs
	 * project-name: repo_url repo_folder
	 */
	private static final Map<String,String> KNOWN_PROJECTS = new ArrayMap(
		"winterwell.utils", 
			"https://github.com/sodash/open-code open-code winterwell.utils",
		"winterwell.web", 
			"https://github.com/sodash/open-code open-code winterwell.web",
		"winterwell.webappbase", 
			"https://github.com/sodash/open-code open-code winterwell.webappbase",
		"winterwell.nlp", 
			"https://github.com/sodash/open-code open-code winterwell.nlp",
		"winterwell.maths", 
			"https://github.com/sodash/open-code open-code winterwell.maths",
		"winterwell.datalog", 
			"https://github.com/sodash/open-code open-code winterwell.datalog",
		"winterwell.depot", 
			"https://github.com/sodash/open-code open-code winterwell.depot",
		"winterwell.bob", 
			"https://github.com/sodash/open-code open-code winterwell.bob",
		"youagain-java-client", 
			"https://github.com/sodash/open-code open-code youagain-java-client",
		"elasticsearch-java-client",
			"https://github.com/winterstein/elasticsearch-java-client.git",
//		"juice",
//			"https://github.com/winterstein/juice", not in github - in our repo
		"jtwitter",
			"https://github.com/winterstein/JTwitter.git",
		"flexi-gson", 
			"https://github.com/winterstein/flexi-gson.git"
	);
	
	public static GitBobProjectTask getKnownProject(String pname) {
		String g_s = KNOWN_PROJECTS.get(pname);
		if (g_s==null) return null;
		String[] gs = g_s.split(" ");
		boolean isSubdir = gs.length > 1; 
		File bobdir = GitBobProjectTask.getGitBobDir();
		File dir = new File(bobdir, isSubdir? gs[1] : pname);
		GitBobProjectTask gb = new GitBobProjectTask(gs[0], dir);
		if (isSubdir) {
			gb.setSubDir(gs[2]);
		}
		return gb;
	}

}
