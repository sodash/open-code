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


	private static volatile boolean initFlag;

	public WinterwellProjectFinder() {
		init();
	}

	/**
	 * Just spit out some debug info (once)
	 */
	private void init() {
		if (initFlag) return;
		initFlag = true;
		// Where are we looking?
		File wdir = FileUtils.getWinterwellDir();
		Log.d("ProjectFinder", "WINTERWELL_HOME: "+wdir);		
		Log.d("ProjectFinder", "bobwarehouse: "+new File(wdir, "bobwarehouse"));
	}

	/**
	 * @return null on failure
	 */
	@Override
	public File apply(String _projectName) {
		// HACK portal is in adserver
		if ("portal".equals(_projectName)) {
			_projectName = "adserver";
		}
		List<File> possDirs = new ArrayList();
		// are we in the project dir?
		if (FileUtils.getWorkingDirectory().getName().equals(_projectName)) {
			possDirs.add(FileUtils.getWorkingDirectory());
		}
		// ...also check the Eclipse project file
		try {
			// This will likely fail on a "strange" computer as it uses winterwell-home :(
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
			// prefer the warehouse
			possDirs.add(new File(wdir, "bobwarehouse/"+_projectName));
			possDirs.add(new File(wdir, "bobwarehouse/open-code/"+_projectName));
			// NB: winterwell-code is typically cloned as code, so let's check both options
			possDirs.add(new File(wdir, "bobwarehouse/code/"+_projectName));
			possDirs.add(new File(wdir, "bobwarehouse/winterwell-code/"+_projectName));
			// only the warehouse? For robustly repeatable builds
//			possDirs.add(new File(wdir, "open-code/"+_projectName));
//			possDirs.add(new File(wdir, "code/"+_projectName));
//			possDirs.add(new File(wdir, _projectName));
		} catch(Exception ex) {
			// no WINTERWELL_HOME
			Log.w("BuildWinterwellProject", "No WINTERWELL_HOME found "+ex);			
		}		
		// Bug seen May 2020: beware of basically empty dirs
		for (File pd : possDirs) {
			if ( ! pd.exists()) continue;
			// HACK look for some file to confirm its valid
			for(String f : "src .classpath .project config".split(" ")) {
				if (new File(pd, f).exists()) {
					return pd;
				}
			}
		}
		// failed
		Log.e("BuildWinterwellProject", "Could not find project directory for "+_projectName+" Tried "+possDirs);
		return null;
	}

	/**
	 * HACK for deploying WW libs
	 * {project-name: "repo_url repo_folder sub_folder"}
	 */
	static final Map<String,String> KNOWN_PROJECTS = new ArrayMap(
		"winterwell.utils", 
			"https://github.com/good-loop/open-code open-code winterwell.utils",
		"winterwell.web", 
			"https://github.com/good-loop/open-code open-code winterwell.web",
		"winterwell.webappbase", 
			"https://github.com/good-loop/open-code open-code winterwell.webappbase",
		"winterwell.nlp", 
			"https://github.com/good-loop/open-code open-code winterwell.nlp",
		"winterwell.maths", 
			"https://github.com/good-loop/open-code open-code winterwell.maths",
		"winterwell.datalog", 
			"https://github.com/good-loop/open-code open-code winterwell.datalog",
		"winterwell.depot", 
			"https://github.com/good-loop/open-code open-code winterwell.depot",
		"winterwell.bob", 
			"https://github.com/good-loop/open-code open-code winterwell.bob",
		"youagain-java-client", 
			"https://github.com/good-loop/open-code open-code youagain-java-client",
			
		"elasticsearch-java-client",
			"https://github.com/good-loop/elasticsearch-java-client.git",
		"juice",
			"https://github.com/good-loop/juice",
		"play.good-loop.com",
			"https://github.com/good-loop/play.good-loop.com.git",
			
		"jtwitter",
			"https://github.com/winterstein/JTwitter.git",
		"flexi-gson", 
			"https://github.com/winterstein/flexi-gson.git",
			
		"dataloader",
			"git@github.com:/good-loop/code code dataloader",
		"youagain-server",
			"git@github.com:/good-loop/code code youagain-server",
		"winterwell.demographics",
			"git@github.com:/good-loop/code code winterwell.demographics"

	);
	

}
