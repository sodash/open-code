package jobs;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.BigJarTask;
import com.winterwell.bob.tasks.CompileTask;
import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.JUnitTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
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
		jt.setManifestProperty(JarTask.MANIFEST_TITLE, 
				projectDir.getName()+" library (c) Winterwell. All rights reserved.");
		if (mainClass!=null) {
			jt.setManifestProperty(JarTask.MANIFEST_MAIN_CLASS, mainClass);
		}
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
	
	protected boolean incGitInManifest;

	public final File projectDir;
	protected boolean incSrc;
	protected File jarFile;

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
		FileUtils.delete(getJar());
		JarTask jar = new JarTask(getJar(), getBinDir());
		jar.setAppend(false);
		jar.setManifestProperty(JarTask.MANIFEST_TITLE, 
				projectDir.getName()+" library (c) Winterwell. All rights reserved.");
		if (mainClass!=null) {
			jar.setManifestProperty(JarTask.MANIFEST_MAIN_CLASS, mainClass);
		}
		// Version
		String gitiv = "", by = "";
		try {
			// go up until we're in git or fail
			File repo = srcDir.getParentFile();
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
			by = " by: "+WebUtils2.hostname();
		} catch(Throwable ex) {
			Log.w(LOGTAG, ex);
		}
		jar.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, 
				"version: "+(Utils.isBlank(version)? new Time().ddMMyyyy() : version)
				+gitiv+by);
		// vendor
		jar.setManifestProperty("Implementation-Vendor", "Winterwell");	
//		// Git details? No this upsets IntelliJ
		if (incGitInManifest) {
			String branch = GitTask.getGitBranch(srcDir.getParentFile());
			jar.setManifestProperty("branch", branch);
		}
		jar.run();
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

	private void doSCP() {
		if ( ! scpToWW) return;			
		String remoteJar = "/home/winterwell/public-software/"+getJar().getName();
		SCPTask scp = new SCPTask(getJar(), "winterwell@winterwell.com",				
				remoteJar);
		// this is online at: https://www.winterwell.com/software/downloads
		scp.setMkdirTask(false);			
		scp.runInThread();
		report.put("scp to remote", "winterwell.com:"+remoteJar);		
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

}
