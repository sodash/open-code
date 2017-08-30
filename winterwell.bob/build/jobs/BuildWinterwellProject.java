package jobs;

import java.io.File;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.CompileTask;
import com.winterwell.bob.tasks.CopyTask;
import com.winterwell.bob.tasks.GitTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;

/**
 * Build & copy into code/lib
 * @author daniel
 *
 */
public class BuildWinterwellProject extends BuildTask {
	private String mainClass;

	/**
	 * @return the jar file (after building!)
	 */
	public File getJar() {
		return jarFile;
	}
	
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}
	
	protected boolean incGitInManifest;

	public final File projectDir;
	protected boolean incSrc;
	protected File jarFile;

	private String version;

	private boolean compile;
	
	public void setCompile(boolean compile) {
		this.compile = compile;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public BuildWinterwellProject setIncSrc(boolean incSrc) {
		this.incSrc = incSrc;
		return this;
	}
	
	public BuildWinterwellProject(File projectDir) {
		this.projectDir = projectDir;
		assert projectDir.isDirectory() : projectDir+" "+this;
		jarFile = new File(projectDir, projectDir.getName()+ ".jar");
	}

	@Override
	public void doTask() throws Exception {
		File srcDir = getSrcDir();
		File binDir = getBinDir();
		binDir.mkdir();
		assert binDir.isDirectory() : binDir.getAbsoluteFile();
		
		// compile
		doTask2_compile(srcDir, binDir);
		
		// Jar		
		FileUtils.delete(jarFile);
		JarTask jar = new JarTask(jarFile, getBinDir());
		jar.setAppend(false);
		jar.setManifestProperty(JarTask.MANIFEST_TITLE, 
				projectDir.getName()+" library (c) Winterwell. All rights reserved.");
		if (mainClass!=null) {
			jar.setManifestProperty(JarTask.MANIFEST_MAIN_CLASS, mainClass);
		}
		// Version = date Is this good or bogus?
		Time vt = new Time();
		try {
			jar.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, 
					"date: "+vt.ddMMyyyy()+" git: "+GitTask.getLastCommitId(srcDir.getParentFile()));
		} catch(Exception ex) {
			jar.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, vt.ddMMyyyy());
		}
//		// Git details? No this upsets IntelliJ
		if (incGitInManifest) {
			String branch = GitTask.getGitBranch(srcDir.getParentFile());
			jar.setManifestProperty("branch", branch);
		}
		jar.run();
		
		// source code?
		if (incSrc) {
			JarTask jar2 = new JarTask(jarFile, new File(projectDir, "src"));
			jar2.setAppend(true);
			jar2.setManifestProperty(JarTask.MANIFEST_TITLE, 
					projectDir.getName()+" library (c) Winterwell. All rights reserved.");
			if (version!=null) jar2.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, version);
			jar2.run();			
		}
		// Test
		// JUnitTask junit = new JUnitTask(srcDir, binDir, new File(projectDir,
		// "unit-tests.html"));
		// junit.run();
		
		// copy into code/lib
		File lib = new File(FileUtils.getWinterwellDir(), "code/lib");
		lib.mkdirs();
		FileUtils.copy(jarFile, lib);
		Log.d(LOGTAG, "Copied "+jarFile.getName()+" to "+lib);
	}

	protected File getBinDir() {
		return new File(projectDir, "bin");
	}

	protected File getSrcDir() {
		return new File(projectDir, "src");
	}

	protected void doTask2_compile(File srcDir, File binDir) {
		// FIXME Compile seeing errors in Windows re XStream dependency!
		if (compile) {
			CompileTask compile = new CompileTask(srcDir, binDir);
			compile.run();
		}
		CopyTask nonJava = new CopyTask(srcDir, binDir);
		nonJava.setNegativeFilter(".*\\.java");
		nonJava.setIncludeHiddenFiles(false);
		nonJava.run();
	}

}
