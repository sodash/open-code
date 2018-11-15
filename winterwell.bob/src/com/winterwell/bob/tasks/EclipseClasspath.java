package com.winterwell.bob.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.w3c.dom.Node;

import com.winterwell.utils.IFn;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;

/**
 * HACK Interrogate an Eclipse .classpath file (which is where
 * the dependencies for a Java project are kept).
 *
 * WARNING: Assumes the Winterwell directory layout!
 *
 * This relies on various assumptions about the workspace layout and specific files that may not be valid.
 *
 * @author Daniel
 */
public class EclipseClasspath {

	@Override
	public String toString() {
		return "EclipseClasspath[projectDir=" + projectDir + "]";
	}

	/**
	 * the .classpath file
	 */
	private File file;
	private File projectDir;

	/**
	 *
	 * @param file Either the .classpath file or the project directory
	 * will do fine, thank you.
	 */
	public EclipseClasspath(File file) {
		assert file != null;
		if (file.isDirectory()) file = new File(file, ".classpath");
		this.file = file;
		this.projectDir = file.getParentFile();
		if ( ! file.exists()) {
			throw Utils.runtime(new FileNotFoundException(file.getAbsolutePath()));
		}
	}

	/**
	 * @return The jar files referenced by this project.
	 * This does NOT include jar files referenced by projects which this
	 * project references (i.e. it's not recursive -- see {@link #getCollectedLibs()}).
	 *
	 * The method for resolving workspace paths is far from foolproof. It
	 * assumes a flat workspace structure, where each project has a folder in
	 * the "workspace directory" (the default is set to [winterwell home]/code).
	 * @see #setWorkspace(File)
	 * @see #getCollectedLibs()
	 */
	public List<File> getReferencedLibraries() {
		String xml = FileUtils.read(file);
		List<Node> tags = WebUtils.xpathQuery("//classpathentry[@kind='lib']", xml);
		if (tags.isEmpty()) {
			Log.report("eclipse", "No classpath info found in "+file+". Is this a valid .classpath file?", Level.WARNING);
		}
		List<File> files = new ArrayList<File>();
		for (Node node : tags) {
			Node path = node.getAttributes().getNamedItem("path");
			File f = getFileFromEPath(path.getTextContent());
			files.add(f);
		}
		return files;
	}
	
	KErrorPolicy onError = KErrorPolicy.REPORT;
	
	public void setErrorPolicy(KErrorPolicy onError) {
		this.onError = onError;
	}
	

	private File getFileFromEPath(String path) {
		File f = getFileFromEPath2(path);
		if (f.exists()) return f;
		// fail!
		switch(onError) {
		case THROW_EXCEPTION: 
			throw new WrappedException(new FileNotFoundException(path));
		case REPORT:
			Log.e("eclipse.classpath", "Cannot resolve file for path: "+path);
		case IGNORE: case ACCEPT:
			break;
		case DELETE_CAUSE: case RETURN_NULL: 
			return null;
		case ASK:
			throw new TodoException(path);			
		case DIE:
			Log.e("eclipse.classpath", "Cannot resolve file for path: "+path);
			System.exit(1);
		}
		return f;
	}
	
	

	private File getFileFromEPath2(String path) {
		if ( ! path.startsWith("/") || path.startsWith("\\")) {
			// Our project
			File f = new File(projectDir, path);		 
			return f;
		}
		// need to locate the damn project!
		String[] pathBits = path.split("[/\\\\]");
		// Drop the first bit (which wil be empty -- from the leading /)
		List<String> rest = Arrays.asList(Arrays.copyOfRange(pathBits, 2, pathBits.length));
		File f = new File(getProjectDir(pathBits[1]),
				StrUtils.join(rest, "/"));
		return f;
	}

	/**
	 * Patterned after getReferencedProjects
	 * @return a list of Eclipse "user library" names
	 */
	public List<String> getUserLibraries() {
		String xml = FileUtils.read(file);
		List<String> result = new ArrayList<String>();
		List<Node> tags = WebUtils.xpathQuery("//classpathentry[@kind='con']", xml);
		for (Node node : tags) {
			Node pathNode = node.getAttributes().getNamedItem("path");
			String path = pathNode.getTextContent();
			if (!path.startsWith("org.eclipse.jdt.USER_LIBRARY")) continue;
			String[] bits = path.split("/", 2);
			assert bits.length == 2 : "Unexpected user library format in " + path;
			result.add(bits[1]);
		}
		return result;
	}

	/**
	 * Retrieve jars stored in the Winterwell eclipse user library dictionary in
	 * middleware/userlibraries.userlibraries
	 * @param name the short name of the user library e.g. "akka"
	 * @return The list of jars in that library
	 */
	public Set<File> getUserLibrary(String name) {
		File mware = projectFinder.apply("middleware");
		File userLibraries =  new File(mware, "userlibraries.userlibraries");
		String xml = FileUtils.read(userLibraries);
		HashSet<File> result = new HashSet<File>();
		// TODO: I suppose we should error out if the library is not defined!
		List<Node> tags = WebUtils.xpathQuery("//library[@name='" + name + "']/archive", xml);
		for (Node node : tags) {
			Node pathNode = node.getAttributes().getNamedItem("path");
			String path = pathNode.getTextContent();
			File f = getFileFromEPath(path);
			result.add(f);
		}
		return result;
	}
	
	IFn<String,File> projectFinder = new WinterwellProjectFinder();
	private boolean includeProjectJars;

	/**
	 * @return The Eclipse projects referenced by this project.
	 */
	public List<String> getReferencedProjects() {
		String xml = FileUtils.read(file);
		List<Node> tags = WebUtils.xpathQuery("//classpathentry[@kind='src']", xml);
		List<String> files = new ArrayList<String>();
		for (Node node : tags) {
			Node path = node.getAttributes().getNamedItem("path");
			String f = path.getTextContent();
			// local src folder?
			if ( ! (f.startsWith("/")
				|| f.startsWith("\\"))) continue;
			// need to locate the damn project!
			files.add(f.substring(1));
		}
		return files;
	}

	/**
	 * @param projectName
	 * @return the directory for this project, based on {@link #workspaceDir}
	 */
	private File getProjectDir(String projectName) {
		assert ! Utils.isBlank(projectName);
		File f = projectFinder.apply(projectName);
		return f;
	}

	/**
	 * @return all the jar files needed? Never null.
	 * 
	 * This can get confused by jars from Eclipse user-libraries :(
	 */
	public Set<File> getCollectedLibs() {
		Set<File> libs = new HashSet();
		Set<String> projects = new HashSet();
		getCollectedLibs2(libs, projects);
		return libs;
	}

	/**
	 * 
	 * @param libs
	 * @param projects Avoid repeats
	 */
	private void getCollectedLibs2(Set<File> libs, Set<String> projects)
	{
		List<File> libs2 = getReferencedLibraries();
		libs.addAll(libs2);

		// User libraries
		for (String lib : getUserLibraries()) {
			libs.addAll(getUserLibrary(lib));
		}

		String pro = getProjectName();
		projects.add(pro);
		List<String> pros = getReferencedProjects();
		for (String p : pros) {
			if (projects.contains(p)) continue;			
			// prefer top level projects
			File fp = null;
			try {
				fp = projectFinder.apply(p);
				if (fp==null) throw new FileNotFoundException(); 
			} catch(Exception ex) {
				Log.i(LOGTAG, "Could not find project "+p+" for collecting libs");
				continue;
			}
			try {
				EclipseClasspath pec = new EclipseClasspath(fp);
				pec.setIncludeProjectJars(includeProjectJars);
				pec.getCollectedLibs2(libs, projects);
			} catch(Exception ex) {
				Log.w("eclipse", ex);
			}
			// HACK add in the project jar?
			if (includeProjectJars) {
				File projectJar = new File(fp, p+".jar");
				if (projectJar.isFile()) {
					libs.add(projectJar);
				} else {
					Log.d(LOGTAG, "No project jar for "+p);
				}
			}
		}
	}
	
	static final String LOGTAG = "EclipseClasspath";

	public String getProjectName() {
		File dotProject = new File(file.getParentFile(),".project");
		String xml = FileUtils.read(dotProject);
		List<Node> tags = WebUtils.xpathQuery("//name", xml);
		return tags.get(0).getTextContent();
	}

	/**
	 * HACK method - if a referenced project has a jar named
	 * (project)(version numbers?).jar
	 * Then include that jar
	 */
	public void setIncludeProjectJars(boolean b) {
		includeProjectJars = b;		
	}

}
