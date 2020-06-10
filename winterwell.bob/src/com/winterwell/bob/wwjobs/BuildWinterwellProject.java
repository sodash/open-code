package com.winterwell.bob.wwjobs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BobScriptFactory;
import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.BigJarTask;
import com.winterwell.bob.tasks.CompileTask;
import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.bob.tasks.EclipseClasspath;
import com.winterwell.bob.tasks.GitBobProjectTask;
import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.JUnitTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.bob.tasks.MakeVersionPropertiesTask;
import com.winterwell.bob.tasks.MavenDependencyTask;
import com.winterwell.bob.tasks.SCPTask;
import com.winterwell.bob.tasks.SyncEclipseClasspathTask;
import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ATask.QStatus;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.app.KServerType;

/**
 * Build & copy into code/lib
 * @author daniel
 *
 */
public class BuildWinterwellProject extends BuildTask {
	
	/**
	 * @deprecated Is this used??
	 */
	private static final String DEPENDENCIES_ALL = "lib";

	protected boolean makeFatJar;
	
	private String mainClass;

	private File fatJar;

	@Override
	protected String getTaskName() {
		return super.getTaskName()+"-"+getProjectName();
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Uses Eclipse .classpath file to find projects.
	 * Returns a fresh ArrayList which can be modified safely.
	 */
	@Override
	public List<BuildTask> getDependencies() {
		// what projects does Eclipse specify?
		ArraySet deps = new ArraySet();
		// what projects does Eclipse specify?
		assert projectDir != null : this;
		EclipseClasspath ec = new EclipseClasspath(projectDir);
		List<String> projects = ec.getReferencedProjects();
		for (String pname : projects) {			
			WinterwellProjectFinder pf = new WinterwellProjectFinder();			
			File pdir = pf.apply(pname);
			// no local project? maybe GitBob can get it
			if (pdir==null || ! pdir.isDirectory()) {
				// known GitBob project?
				BuildTask bt = WinterwellProjectFinder.getKnownProject(pname);
				if (bt!=null) {
					deps.add(bt);				
				}
				continue;
			}
			
			BuildTask bt;
			if (false) {
				// Try a git pull (fail quietly)
				GitTask gt = new GitTask(GitTask.PULL, pdir);
				gt.setErrorHandler(IGNORE_EXCEPTIONS);
				deps.add(gt);
				
				// NB: We'd prefer to use the "local" builder class over GitBob
				// But the GitBob version is wanted for deployment
				// ... and best to have local & deployment behave the same
				bt = getDependencies2a_builderClass(pname, pdir);
			} else {
			
				bt = getDependencies2b_gitBob(pdir);
			}
			
			if (bt!=null) deps.add(bt);
		}
		// done
		return new ArrayList(deps);
	}
	
	private GitBobProjectTask getDependencies2b_gitBob(File pdir) {
		File bfile = new BobScriptFactory(pdir).findBuildScript2(pdir, null);
		if (bfile==null) {
			return null;
		}
		
		File bobdir = GitBobProjectTask.getGitBobDir();
		File dir = new File(bobdir, pdir.getName());
		
		// Git repo
		File conf = new File(pdir,".git/config");
		String subdir = null;
		if ( ! conf.isFile()) {
			// up one eg open-code?
			subdir = pdir.getName();
			dir = new File(bobdir, pdir.getParentFile().getName());
			conf = new File(dir, ".git/config");				
		}
		if ( ! conf.isFile()) {
			return null;
		}
		String gitconfig = FileUtils.read(conf);
		String[] found = StrUtils.find(Pattern.compile("url\\s=\\s(\\S+)",Pattern.DOTALL), gitconfig);
		if (found==null) {
			return null;
		}
		String gitUrl = found[1];
		
		GitBobProjectTask gb = new GitBobProjectTask(gitUrl, dir);
		if (subdir!=null) gb.setSubDir(subdir);
		Log.i("GitBob", gitUrl+" "+dir.getName()+" "+subdir);
		return gb;
	}

	/**
	 * Use in-JVM bob build or jar download
	 * @param deps
	 * @param pname
	 * @param pf
	 */
	private BuildTask getDependencies2a_builderClass(String pname, File pdir) {
		File bfile = new BobScriptFactory(pdir).findBuildScript2(pdir, null);
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
			return wwdt;			
		} else {
			// HACK look in wwjobs
			try {
				String pname2 = pname.replace("winterwell.", "");
				String cname = BuildUtils.class.getPackage().getName()+".Build"+StrUtils.toTitleCase(pname2);
				Class<?> bt = Class.forName(cname);
				return (BuildTask) bt.newInstance();
			} catch(Throwable ex) {
				// oh well
				Log.w(LOGTAG, "Whilst building "+projectName+" Skip dep "+pname+" with dir "+pdir);
				return null;
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
		File libs = new File(projectDir, DEPENDENCIES_ALL);
		if (libs.isDirectory()) {
			List<File> jars2 = FileUtils.find(libs, ".*\\.jar");
			jars.addAll(jars2);
		}		
		// maven deps
		File deps = new File(projectDir, MavenDependencyTask.MAVEN_DEPENDENCIES_FOLDER);
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
		jt.setDepth(getDepth()+1);
		setJarManifest(jt, projectDir, 
				projectDir.getName()+" fat-jar (c) Winterwell. All rights reserved.");
		jt.run();
		jt.close();
		// done
		report.put("fat-jar", jt.getJar().getAbsolutePath());
		this.fatJar = jt.getJar();
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
	protected File outDir;
	
	/**
	 * null by default. If set, put output files into here
	 */
	public void setOutDir(File outDir) {
		this.outDir = outDir;
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

	/**
	 * @deprecated Better to use the String constructor, and let {@link WinterwellProjectFinder} find the dir
	 * @param projectDir
	 * @param projectName
	 */	
	public BuildWinterwellProject(File projectDir, String projectName) {
		assert projectDir != null : projectName;
		this.projectDir = projectDir;
		assert projectDir.isDirectory() : projectDir+" "+this;
		if (projectName==null) projectName = projectDir.getName();
		this.projectName = projectName;
		// dependencies shouldnt need rebuilding all the time
		setSkipGap(TUnit.DAY.dt);
		
		// HACK scp to release the jar?
		int bc = Bob.getSingleton().getBobCount();
		if (BuildHacks.getServerType()==KServerType.LOCAL 
				&& getConfig().depth==0
				&& bc==0
				) 
		{
			setScpToWW(true);
		}
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
		jar.setDepth(getDepth()+1);
		jar.setAppend(false);
		setJarManifest(jar, srcDir, projectDir.getName()+" library (c) Good-Loop. All rights reserved.");
		jar.run();
		if ( ! tempJar.isFile()) throw new FailureException("make jar failed?! "+this+" "+getJar());
		// replace the old jar
		FileUtils.move(tempJar, getJar());
		report.put("jar", getJar().getAbsolutePath());
		
		// source code?
		if (incSrc) {
			JarTask jar2 = new JarTask(getJar(), new File(projectDir, "src"));
			jar2.setDepth(getDepth()+1);
			jar2.setAppend(true);
			jar2.run();			
		}
		
		// collect jars
		collectJars(new File(projectDir, "build-lib"));
		
		// version.properties
		MakeVersionPropertiesTask mvpt = new MakeVersionPropertiesTask().setAppDir(projectDir);
		Properties props = new Properties();
		mvpt.setProperties(props);
		mvpt.run();
		mvpt.close();
		
		// fat jar?
		if (makeFatJar) {
			doFatJar();
		}		
		
		// attempt to upload (but don't block)
		doSCP();
		
		// update classpath? HACK (we could prob run this more but safer to do less often)
		List<MavenDependencyTask> mdts = Containers.filterByClass(getDependencies(), MavenDependencyTask.class);
		doUpdateClasspath(mdts);		
	}

	private void doUpdateClasspath(List<MavenDependencyTask> mdts) {
		if (mdts.isEmpty()) return;			
		boolean isClean = mdts.get(0).isCleanOutputDirectory();
		boolean skipped = mdts.get(0).isSkipFlag();
		if ( ! isClean || skipped) {
			return;
		}
		if (BuildHacks.getServerType() != KServerType.LOCAL) {
			return;
		}
		try {
			SyncEclipseClasspathTask sync = new SyncEclipseClasspathTask(projectDir);
			sync.setDepth(getDepth()+1);
			sync.run();
		} catch(Exception ex) {
			// allow failure eg file permissions as this is a nicety not a vital build step
			Log.w(LOGTAG, ex);
		}
	}

	private void setJarManifest(JarTask jar, File projectDir, String title) {
		jar.setManifestProperty(JarTask.MANIFEST_TITLE, title);
		if (mainClass!=null) {
			// TODO itd be nice to test after whether the mainClass is in the jar
			jar.setManifestProperty(JarTask.MANIFEST_MAIN_CLASS, mainClass);
		}
		// Version
		String gitiv = null, by = null;
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
				gitiv = "git: "+gitInfo.get("hash")
					+" "+gitInfo.get("subject")
					// non-master branch (master is not worth stating)
					+ (branch!=null && ! "master".equals(branch)? " "+branch : "") 
					;
			}
			// Git details as their own property e.g. "branch"? No this upsets IntelliJ
			// So we pack them into version.
			by = "by: "+WebUtils2.hostname();
		} catch(Throwable ex) {
			Log.w(LOGTAG, this+" "+ex);
		}
		// include version, time, and a unique nonce
		jar.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, 				
			StrUtils.joinWithSkip(" ", 
				version, new Time().ddMMyyyy(), "nonce_"+Utils.getRandomString(4), gitiv, by
		));
		// vendor
		jar.setManifestProperty("Implementation-Vendor", "Winterwell");	
	}

	private void doSCP() {
		if ( ! scpToWW) return;
		{
			String remoteJar = "/home/winterwell/public-software/"+getJar().getName();
			SCPTask scp = new SCPTask(getJar(), "winterwell@winterwell.com",				
					remoteJar);
			scp.setDepth(getDepth()+1);
			// this is online at: https://www.winterwell.com/software/downloads
			scp.setMkdirTask(false);			
			scp.runInThread();
			report.put("scp to remote", "winterwell.com:"+remoteJar);
		}
		if (makeFatJar && getFatJar()!=null) {
			// copy to local bin
			try {
				FileUtils.copy(getFatJar(), new File(FileUtils.getUserDirectory(), "bin"));
			} catch (Exception ex) {
				Log.w(LOGTAG, "jar copy to local bin failed: "+ex);
			}
			// remote
			String remoteJar = "/home/winterwell/public-software/"+getFatJar().getName();
			SCPTask scp = new SCPTask(getFatJar(), "winterwell@winterwell.com",				
					remoteJar);
			scp.setDepth(getDepth()+1);
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
	
	public File getFatJar() {
		return fatJar;
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
		// FIXME Compile seeing errors on Linux when run from JUnit but not when bob is run from the command line ?! Nov 2018
		if (compile) {
			try {
				assert projectDir != null : this;
				CompileTask compile = new CompileTask(srcDir, binDir);
				compile.setCleanOutputDir(true);
				compile.setDepth(getDepth()+1);
				// classpath
				EclipseClasspath ec = new EclipseClasspath(projectDir);
				ec.setIncludeProjectJars(true);
				Set<File> libs = ec.getCollectedLibs();
				compile.setClasspath(libs);		
	//			compile.setSrcJavaVersion("1.9");
				compile.setOutputJavaVersion("11"); // Java 11 jars
				compile.setDebug(true);
				compile.run();
				compile.close();
			} catch(Exception ex) {
				// HACK to allow ignoring via -ignore flag
				if (getConfig().ignoreAllExceptions) {
					Log.e(ex); // :'( Dec 2018 Why??
					setStatus(QStatus.ERROR);
				} else {
					throw ex;
				}
			}
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

	

	/**
	 * Find jars and move them into tmp-lib
	 */
	protected void collectJars(File libBuild) {
		EclipseClasspath ec = new EclipseClasspath(projectDir);
		ec.setIncludeProjectJars(true);
		Set<File> jars = ec.getCollectedLibs();
		Log.d(LOGTAG, "Dependency graph:\n"
				+Printer.toString(ec.getDepsFor(), "\n", " <- "));
		// Create local lib dir			
		libBuild.mkdirs();
		assert libBuild.isDirectory();
		// Ensure desired jars are present
		for (File jar : jars) {
			File localJar = new File(libBuild, jar.getName()).getAbsoluteFile();
			
			// check versions and pick which one to keep?
			if (localJar.isFile()) {
				File newJar = JarTask.pickNewerVersion(localJar, jar);
				if (newJar.equals(localJar)) continue;
			}
			FileUtils.copy(jar, localJar);
		}
		
		// Remove unwanted jars? -- no too dangerous		
		
		// Copy this jar in to
		if (getJar().isFile()) {
			FileUtils.copy(getJar(), libBuild);	
		}		
		
		System.out.println("Jars: "+Printer.toString(Arrays.asList(libBuild.list()), "\n"));
	}

}
