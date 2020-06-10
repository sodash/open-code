package com.winterwell.bob;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

import com.winterwell.bob.tasks.Classpath;
import com.winterwell.bob.tasks.CompileTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

public class BobScriptFactory {

	private static final String LOGTAG = "BobScriptFactory";
	private final File baseDir;

	public BobScriptFactory(File workingDirectory) {
		baseDir = workingDirectory;
	}
	

	File findBuildScript(String optionalName) {
		File f = findBuildScript2(baseDir, optionalName);
		return f;
	}
	
	/**
	 * ??how best to expose this method
	 * @param projectDir
	 * @param optionalName
	 * @return BuildX.java, or null 
	 * ??change to List so we can better communicate around failed-to-find/pick
	 */
	public File findBuildScript2(File projectDir, String optionalName) {
		File bdir = new File(projectDir, "builder");
		if ( ! bdir.isDirectory()) {
			return null;
		}
		String namePattern = ".*Build.*\\.java";
		if (optionalName!=null) {
			namePattern = ".*"+optionalName+"\\.java";
		}
		List<File> files = FileUtils.find(bdir, namePattern);
		if (files.isEmpty()) return null;
		if (files.size()==1) {
			Log.i(LOGTAG, "Auto-build: found file "+files.get(0));
			return files.get(0);
		}
		Log.w(LOGTAG, "Auto-build: could not pick between "+files.size()+" tasks in "+bdir);
		Log.w(LOGTAG, Containers.apply(files, FileUtils::getBasename));
		return null;
	}


	/**
	 * TODO refactor into a BobBuildScript class 'cos this is actually quite complex
	 * @throws Exception 
	 */
	Class getClass(String classOrFileName) throws Exception {
		try {
			return getClass2_instantiateOrCompile(classOrFileName);
		} catch(Exception ex) {
			// partial name? try a find
			File buildFile = findBuildScript(classOrFileName);
			if (buildFile!=null && ! buildFile.toString().equals(classOrFileName)) {
				return getClass2_instantiateOrCompile(buildFile.toString());
			}
			throw ex;
		}
	}
	
	Class getClass2_instantiateOrCompile(String classOrFileName) throws Exception {
		String className = classOrFileName;
		// Strip endings if they were used
		if (classOrFileName.endsWith(".java")) {
			className = classOrFileName.substring(0, classOrFileName.length() - 5);
		}
		if (classOrFileName.endsWith(".class")) {
			className = classOrFileName.substring(0, classOrFileName.length() - 6);
		}		
		try {
			Class<?> clazz = Class.forName(className);
			return clazz;
		} catch(ClassNotFoundException ex) {
			// is it a directory?
			File dir = new File(classOrFileName);
			if (dir.isDirectory()) {
				File found = findBuildScript2(dir, null);
				if (found!=null) {
					Log.d(LOGTAG, "located build-script "+found+" in directory "+dir);
					classOrFileName = found.toString();
				}
			}
			
			File baseDir = FileUtils.getWorkingDirectory();
			Pair2<File, File> klass = getClass3_compileClass(baseDir, classOrFileName);
			if (klass == null) {
				throw ex; // oh well - failed
			}
			assert claspath != null;
			// dynamically load a class from a file?
			Class clazz = ReflectionUtils.loadClassFromFile(klass.first, klass.second, claspath.getFiles());
			return clazz;
		}
	}
	
	Classpath claspath; 
	
	/**
	 * From a .java file or fully-qualified classname,
	 * compile it to a .class file 
	 * @param baseDir 
	 * @param classOrFileName
	 * @return (temp-output-dir, class-file)
	 * @throws Exception
	 */
	Pair<File> getClass3_compileClass(File baseDir, String classOrFileName) throws Exception {
		// Can we compile it here and now?
		// 1. Look for the .java file		
		String fileName = classOrFileName;
		File f = new File(fileName);
		if ( ! f.isFile()) {
			// classname? turn into a file
			fileName = fileName.replace('.', '/');
			// .java ending
			if (fileName.endsWith("/java")) {
				fileName = fileName.substring(0, fileName.length()-5)+".java";
			}
			if ( ! fileName.endsWith(".java")) {
				fileName = fileName+".java";
			}
			f = new File(fileName);
			if ( f.isDirectory()) {
				throw new IllegalArgumentException(f+" from "+classOrFileName+" should have been handled via find-build-script");
			}
			if ( ! f.isFile()) {
				throw new FileNotFoundException(f+" = "+f.getAbsolutePath()+" from "+classOrFileName);
			}
		}
		
		// sniff package
		String src = FileUtils.read(f);
		String[] fnd = StrUtils.find("package (.+);", src);
		// full classname
		String className = (fnd==null? "" : fnd[1]+".") + new File(FileUtils.getBasename(f)).getName();
		
		// output
		File tempDir = FileUtils.createTempDir();
		
		// simple compile?
		try {
			CompileTask cp = new CompileTask(null, tempDir);
			// classpath
			claspath = Bob.getSingleton().getClasspath();									
			cp.setClasspath(claspath);
			// our .java file to compile
			cp.setSrcFiles(f);
			// ...compile
			Log.i(LOGTAG, "Compiling the Bob build script...");
			cp.doTask();
			FileUtils.close(cp);
		} catch(FailureException fex) {
			try {
				Log.d(LOGTAG, "Simple single-file compile failed: "+fex+". Trying for eclipse info...");
				EclipseClasspath ec = new EclipseClasspath(baseDir);
				List<File> srcDirs = ec.getSrcDirs();
				CompileTask compile = new CompileTask(null, tempDir);
				for (File sd : srcDirs) {
					compile.addSrcDir(sd);
				}
				ec.setIncludeProjectJars(true);
				claspath = Bob.getSingleton().getClasspath();
				Set<File> libs = ec.getCollectedLibs();				
				claspath.addAll(libs);
				compile.setClasspath(claspath);
				compile.setDebug(true);
				// our .java file to compile
				compile.addSrcFile(f);
				// ...compile
				compile.doTask();	
				FileUtils.close(compile);
			} catch(Exception fex2) {
				Log.d(LOGTAG, "Trief sniffing Eclipse .classpath - compile also failed: "+fex2);
				throw fex;
			}
		}
		
		// success?
		File klass = new File(tempDir, className.replace('.', '/')+".class");
		if (klass.isFile()) {
			return new Pair<File>(tempDir, klass);
		}
		throw new FailureException("Bootstrap compile failed for "+classOrFileName+" = "+f);
	}
}
