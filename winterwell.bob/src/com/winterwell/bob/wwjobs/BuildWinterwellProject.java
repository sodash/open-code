package com.winterwell.bob.wwjobs;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.BigJarTask;
import com.winterwell.bob.tasks.CompileTask;
import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.ForkJVMTask;
import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.JUnitTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;

/**
 * Build & copy into code/lib
 * @author daniel
 *
 */
public class BuildWinterwellProject extends BuildTask {
	
	protected boolean makeFatJar;
	
	protected String mainClass;

	/**
	 * {@inheritDoc}
	 * 
	 * Uses Eclipse .classpath file to find projects
	 */
	@Override
	public Collection<? extends BuildTask> getDependencies() {
		ArraySet deps = new ArraySet();
		// what projects does Eclipse specify?
		EclipseClasspath ec = new EclipseClasspath(projectDir);
		List<String> projects = ec.getReferencedProjects();
		for (String pname : projects) {			
			WinterwellProjectFinder pf = new WinterwellProjectFinder();
			getDependency2_project(deps, pname, pf);
		}
		return deps;
	}

	private void getDependency2_project(ArraySet deps, String pname, WinterwellProjectFinder pf) {
		File pdir = pf.apply(pname);
		if (pdir==null || ! pdir.isDirectory()) {
			return;
		}
		File bfile = Bob.findBuildScript2(pdir, null);
		if (bfile != null) {					
			// Use a forked Bob to pull in dependencies??
			// or a WW task??
			String builderClass = FileUtils.getRelativePath(bfile, pdir);
			// HACK: pop the first folder?? usually builder/
			int slashi = builderClass.indexOf('/');
			if (slashi > 0) {
				builderClass = builderClass.substring(slashi+1, builderClass.length());	
			}			
			// make file path into package name
			builderClass = builderClass.replace('/', '.').substring(0, builderClass.length()-5);
			// make a WWDep task
			WWDependencyTask wwdt = new WWDependencyTask(pname, builderClass);
			deps.add(wwdt);			
		} else {
			// HACK look in wwjobs
			try {
				String pname2 = pname.replace("winterwell.", "");
				String cname = BuildUtils.class.getPackage().getName()+".Build"+StrUtils.toTitleCase(pname2);
				Class<?> bt = Class.forName(cname);
				deps.add(bt.newInstance());
			} catch(Throwable ex) {
				// oh well
				Log.d("BuildWinterwellProject", "skip dep for project "+pname);
			}
		}		
	}

	protected boolean isCompile() {
		return compile;
	}

	protected File doFatJar() {		
		Collection<File> jars = new ArraySet();
		// this projects jar!
		jars.add(getJar());
		// lib
		File libs = new File(projectDir, "lib");
		if (libs.isDirectory()) {
			List<File> jars2 = FileUtils.find(libs, ".*\\.jar");
			jars.addAll(jars2);
		}		
		// maven deps
		File deps = new File(projectDir, "dependencies");
		if (deps.isDirectory()) {
			List<File> jars2 = FileUtils.find(deps, ".*\\.jar");
			jars.addAll(jars2);
		}	
		// eclipse deps
		EclipseClasspath ec = new EclipseClasspath(projectDir);
		ec.setIncludeProjectJars(true);
		List<String> projects = ec.getReferencedProjects();
		Set<File> ecjars = ec.getCollectedLibs();
		jars.addAll(ecjars);
		// bundle
		File fatjar = new File(projectName+"-all.jar");
//		System.out.println(Printer.toString(jars,"\n\t"));
		BigJarTask jt = new BigJarTask(fatjar, jars);
		setJarManifest(jt, projectDir, 
				projectDir.getName()+" fat-jar (c) Winterwell. All rights reserved.");
		jt.run();
		jt.close();
		// done
		report.put("fat-jar", jt.getJar().getAbsolutePath());
		return jt.getJar();
	}

	
	/**
	 * @return the jar file (after building!)
	 */
	public File getJar() {
		if (jarFile==null) {
			jarFile = new File(getOutputDir(), projectName+ ".jar");
		}
		return jarFile;
	}
	
	/**
	 * This is normally auto-set.
	 * Use this only if you need to give the jar a special name.
	 * @param _jarFile
	 */
	public void setJar(File _jarFile) {
		this.jarFile = _jarFile;
	}
	
	private File getOutputDir() {
		if (outDir==null) {
			return projectDir;
		}
		return outDir;
	}

	/**
	 * null by default. If set, put output files into here
	 */
	File outDir;
	
	/**
	 * null by default. If set, put output files into here
	 */
	public void setOutDir(File outDir) {
		this.outDir = outDir;
	}
	
	public void setMainClass(Class mainClass) {
		setMainClass(mainClass.getCanonicalName());
	}
	
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}
	
	public final File projectDir;
	protected boolean incSrc;
	private File jarFile;

	private String version;
	
	private boolean compile = true;

	protected boolean scpToWW;

	protected String projectName;
	
	public String getProjectName() {
		return projectName;
	}
	
	public BuildWinterwellProject setScpToWW(boolean scpToWW) {
		this.scpToWW = scpToWW;
		return this;
	}
	
	/**
	 * true by default. If false, dont run the compiler
	 * @param compile
	 * @return
	 */
	public BuildWinterwellProject setCompile(boolean compile) {
		this.compile = compile;
		return this;
	}
	
	public BuildWinterwellProject setVersion(String version) {
		this.version = version;
		return this;
	}
	
	public BuildWinterwellProject setIncSrc(boolean incSrc) {
		this.incSrc = incSrc;
		return this;
	}
	
	/**
	 * HACK try a few "standard" places to find the project
	 * @param projectName
	 */
	public BuildWinterwellProject(String projectName) {
		this(guessProjectDir(projectName), projectName);
	}
	
	/**
	 * TODO refactor with {@link EclipseClasspath} so they share code.
	 * @see WinterwellProjectFinder
	 * @param _projectName
	 * @return
	 */
	private static File guessProjectDir(String _projectName) {
		WinterwellProjectFinder wpg = new WinterwellProjectFinder();
		return wpg.apply(_projectName);
	}

	public BuildWinterwellProject(File projectDir, String projectName) {
		assert projectDir != null : projectName;
		this.projectDir = projectDir;
		assert projectDir.isDirectory() : projectDir+" "+this;
		if (projectName==null) projectName = projectDir.getName();
		this.projectName = projectName;
	}

	public BuildWinterwellProject(File projectDir) {
		this(projectDir, null);
	}

	@Override
	public void doTask() throws Exception {
		File srcDir = getJavaSrcDir();
		File binDir = getBinDir();
		binDir.mkdir();
		assert binDir.isDirectory() : binDir.getAbsoluteFile();
		
		// compile
		doTask2_compile(srcDir, binDir);
		
		// Jar	
		File tempJar = File.createTempFile("temp", ".jar");
		JarTask jar = new JarTask(tempJar, getBinDir());
		jar.setAppend(false);
		setJarManifest(jar, srcDir, projectDir.getName()+" library (c) Winterwell. All rights reserved.");
		jar.run();
		if ( ! tempJar.isFile()) throw new FailureException("make jar failed?! "+this+" "+getJar());
		// replace the old jar
		FileUtils.move(tempJar, getJar());
		report.put("jar", getJar().getAbsolutePath());
		
		// source code?
		if (incSrc) {
			JarTask jar2 = new JarTask(getJar(), new File(projectDir, "src"));
			jar2.setAppend(true);
			jar2.run();			
		}
		// Test
		// JUnitTask junit = new JUnitTask(srcDir, binDir, new File(projectDir,
		// "unit-tests.html"));
		// junit.run();
		
//		// copy into code/lib
//		File lib = new File(FileUtils.getWinterwellDir(), "code/lib");
//		lib.mkdirs();
//		File libjar = FileUtils.copy(getJar(), lib);
//		Log.d(LOGTAG, "Copied "+getJar().getName()+" to "+lib);
//		report.put("jar-copy", libjar);
		
		// attempt to upload (but don't block)
		doSCP();
	}

	private void setJarManifest(JarTask jar, File projectDir, String title) {
		jar.setManifestProperty(JarTask.MANIFEST_TITLE, title);
		if (mainClass!=null) {
			jar.setManifestProperty(JarTask.MANIFEST_MAIN_CLASS, mainClass);
		}
		// Version
		String gitiv = "", by = "";
		try {
			// go up until we're in git or fail
			File repo = projectDir;
			while(repo!=null) {
				if (new File(repo, ".git").exists()) break;
				repo = repo.getParentFile();
			}
			if (repo!=null) {
				Map<String, Object> gitInfo = GitTask.getLastCommitInfo(repo);
				Object branch = gitInfo.get("branch");
				gitiv = " git: "+gitInfo.get("hash")
					+" "+gitInfo.get("subject")
					// non-master branch (master is not worth stating)
					+ (branch!=null && ! "master".equals(branch)? " "+branch : "") 
					;
			}
			// Git details as their own property e.g. "branch"? No this upsets IntelliJ
			// So we pack them into version.
			by = " by: "+WebUtils2.hostname();
		} catch(Throwable ex) {
			Log.w(LOGTAG, ex);
		}
		jar.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, 
				"version: "+StrUtils.joinWithSkip(" ", version, new Time().ddMMyyyy())
				+gitiv+by);
		// vendor
		jar.setManifestProperty("Implementation-Vendor", "Winterwell");	
	}

	private void doSCP() {
		if ( ! scpToWW) return;
		{
			String remoteJar = "/home/winterwell/public-software/"+getJar().getName();
			SCPTask scp = new SCPTask(getJar(), "winterwell@winterwell.com",				
					remoteJar);
			// this is online at: https://www.winterwell.com/software/downloads
			scp.setMkdirTask(false);			
			scp.runInThread();
			report.put("scp to remote", "winterwell.com:"+remoteJar);
		}
		// also scp maven file?
		// Hm -- transitive dependencies??
		// Maybe the best solution is for WWDepProject to try and checkout from git??
//		{
//			String remoteJar = "/home/winterwell/public-software/pom.bob."+getProjectName()+".xml";
//			SCPTask scp = new SCPTask(pom, "winterwell@winterwell.com",	remoteJar);
//			// this is online at: https://www.winterwell.com/software/downloads
//			scp.setMkdirTask(false);
//			scp.runInThread();
//			report.put("scp pom to remote", "winterwell.com:"+remoteJar);
//		}
	}
	
	

	protected File getBinDir() {
		return new File(projectDir, "bin");
	}

	/**
	 * 
	 * @return the Java source directory
	 */
	protected File getJavaSrcDir() {
		// flat /src or maven-style src/java?
		File s = new File(projectDir, "src/java");
		if (s.isDirectory()) {
			return s;
		}
		s = new File(projectDir, "src");
		return s;
	}

	protected void doTask2_compile(File srcDir, File binDir) {		
		// FIXME Compile seeing errors in Windows re XStream dependency!
		if (compile) {
			assert projectDir != null : this;
			CompileTask compile = new CompileTask(srcDir, binDir);
			// classpath
			EclipseClasspath ec = new EclipseClasspath(projectDir);
			ec.setIncludeProjectJars(true);
			Set<File> libs = ec.getCollectedLibs();
			compile.setClasspath(libs);		
//			compile.setSrcJavaVersion("1.9");
//			compile.setOutputJavaVersion("1.8");
			compile.run();
			compile.close();
		}
		// also copy any resources across??
		CopyTask nonJava = new CopyTask(srcDir, binDir);
		nonJava.setResolveSymLinks(true);
		nonJava.setNegativeFilter(".*\\.java");
		nonJava.setIncludeHiddenFiles(false);
//		nonJava.setVerbosity(Level.ALL);
		nonJava.run();
	}
	
	/**
	 * TODO this finds no tests?? maybe cos we have to compile the tests dir too. Dunno -- parking for now.
	 * @return
	 */
	public int doTest() {
//		if (true) return 0; // FIXME
		File outputFile = new File(projectDir, "test-output/unit-tests-report.html");
		JUnitTask junit = new JUnitTask(
				null,
				getTestBinDir(),
				outputFile);		
		junit.run();		
		int good = junit.getSuccessCount();
		int bad = junit.getFailureCount();		
		return bad;
	}

	protected File getTestBinDir() {
		// NB not all projects are set to use this (yet)
		return new File(projectDir, "bin.test");
	}

	public BuildWinterwellProject setMakeFatJar(boolean b) {
		makeFatJar = b;
		return this;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"[projectName=" + projectName + "]";
	}

	
	
}
