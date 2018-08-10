package com.winterwell.bob.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.IFn;
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
		} catch(Exception ex) {
			// no WINTERWELL_HOME
			Log.w("BuildWinterwellProject", "No WINTERWELL_HOME found "+ex);			
		}		
		File pdir = FileUtils.or(possDirs);
		if (pdir==null) {
			throw new FailureException("Could not find project directory for "+_projectName+" Tried "+possDirs);
		}
		return pdir;
	}

}
