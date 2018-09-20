package com.winterwell.bob.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Proc;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * EITHER setup via
 * {@link #addDependency(String, String, String)}
 * OR setup via
 * pom.bob.xml
 * 
 * Warning: This will not overwrite existing .jar files 
 * ??does setForceUpdate(boolean) affect this??
 * 
 * See https://stackoverflow.com/questions/1895492/how-can-i-download-a-specific-maven-artifact-in-one-command-line
 * @author daniel
 *
 */
public class MavenDependencyTask extends BuildTask {

	private String mavenArtifactSpec;

	/**
	 * 
	 * @param mavenArtifactSpec "groupId:artifactId:version"
	 */
	public MavenDependencyTask() {
	}

	/**
	 * Convenience for a single artifact dependency
	 * @param artifactId e.g. "mystuff:artifact:1.2" 
	 * (which is Buildr format in Maven central mvnrepository.com)
	 */
	public MavenDependencyTask(String artifactId) {
		String[] bits = artifactId.split(":");
		assert bits.length == 3;
		addDependency(bits[0], bits[1], bits[2]);
	}

	File outDir;
	File projectDir = FileUtils.getWorkingDirectory();

	boolean incSrc;
	
	public MavenDependencyTask setIncSrc(boolean incSrc) {
		this.incSrc = incSrc;
		return this;
	}
	
	public MavenDependencyTask setOutputDirectory(File outDir) {
		this.outDir = outDir;
		return this;
	}
	
	/**
	 * Best practice is to call this, for robust composable build scripts. Defaulst to working-dir.
	 * @param projectDir
	 * @return
	 */
	public MavenDependencyTask setProjectDir(File projectDir) {
		this.projectDir = projectDir;
		return this;
	}
	
	List<String> dependencies = new ArrayList();
	private File pom;
	
	private boolean keepJarVersioning;
	private boolean forceUpdate;
	public MavenDependencyTask setForceUpdate(boolean forceUpdate) {
		this.forceUpdate = forceUpdate;
		return this;
	}
	
	/**
	 * By default, stripVersion so jars have stable names.
	 * @param keepJarVersioning
	 */
	public void setKeepJarVersioning(boolean keepJarVersioning) {
		this.keepJarVersioning = keepJarVersioning;
	}
	
	public MavenDependencyTask addDependency(String groupId, String artifactId, String version) {
		dependencies.add("<dependency><groupId>"+groupId+"</groupId><artifactId>"+artifactId+"</artifactId>" 
				+"<version>"+version+"</version>" 
//				"			<scope>test</scope>\n" + 
				+"</dependency>");
		return this;
	}
	
	/**
	 * Normally it will just find pom.bob.xml. Use this if you have to explicitly set the pom.
	 * @param pom
	 * @return
	 */
	public MavenDependencyTask setPom(File pom) {
		Log.d(LOGTAG, "Set pom "+pom+" "+ReflectionUtils.getSomeStack(8));
		this.pom = pom;
		return this;
	}
	
	@Override
	protected void doTask() throws Exception {
		// files
		// output  jars into ./dependencies by default
		if (outDir==null) {
			setOutputDirectory(new File(projectDir, "dependencies"));
		}
		boolean md = outDir.mkdirs();
		if (md) {
			// if we made the dir, we can take charge of it -- lets git ignore by default
			File gi = new File(outDir, ".gitignore");
			FileUtils.write(gi, "*.jar");
		}
		assert outDir.isDirectory() : this;
		
		if (dependencies.isEmpty()) {
			if (pom==null) pom = FileUtils.or(new File(projectDir, "pom.bob.xml"), new File(projectDir, "pom.xml"));
			if (pom==null || ! pom.isFile()) {
				throw new IllegalStateException("No pom.xml found and no in-Java dependencies were added: "+pom+" "+projectDir);
			}
		} else {		
			if (pom==null) pom = new File(projectDir, "pom.bob.xml");
			doMakePom(pom);
		}
		assert pom.exists() : "EITHER setup via addDependency() OR setup via pom.bob.xml";
		// 
		// http://maven.apache.org/plugins/maven-dependency-plugin/copy-dependencies-mojo.html
//		-DoutputDirectory (defaults to build/dependency)
//		-DstripVersion=true or useBaseVersion for less aggressive
		File pomPrev = null;
		File pomProper = new File(projectDir, "pom.xml");
		if (pomProper.isFile()) {
			pomPrev = new File(projectDir, "pom.xml."+Utils.getRandomString(4)+".temp");
			FileUtils.move(pomProper, pomPrev);
		}
		try {
			// maven expects the pom in the exact place
			if ( ! pom.equals(pomProper)) {
				FileUtils.copy(pom, pomProper);
			}
			assert pomProper.exists():  "no pom file?! "+pomProper+" should be a copy of "+pom;
			
			Proc proc = new Proc(
					"mvn "
					+ (forceUpdate? "-U " : "")
					+ (incSrc? "dependency:sources ": "") // This will stick the sources into ~/.m2/repository :(
					// TODO copy sources into somewhere local
					+"org.apache.maven.plugins:maven-dependency-plugin:3.0.2:copy-dependencies"
					+( ! keepJarVersioning? " -Dmdep.stripVersion=true" : "")
					+ " -DoutputDirectory="+outDir.getAbsoluteFile()					
					);
						
			proc.setDirectory(projectDir);
			Log.d(LOGTAG, "dir: "+projectDir+" run: "+proc.getCommand());
			proc.start();
			proc.waitFor(new Dt(10, TUnit.MINUTE));
			proc.close();
			Log.w(LOGTAG, proc.getError());
			Log.d(LOGTAG, proc.getOutput());
			// TODO store version info somewhere?? e.g. in a versions.txt file??
			// copy sources
			if (incSrc) {
				doTask2_sources(proc.getOutput());
			}
			
			// did it work??		
			if ( ! proc.getOutput().contains("BUILD SUCCESS")) {
				throw new FailureException(proc.getError());
			}		
//			FileUtils.delete(pom);
		} finally {
			if (pomPrev != null) {
				FileUtils.move(pomPrev, pomProper);
			} else {
				FileUtils.delete(pomProper);
			}
		}
	}

	private void doTask2_sources(String output) throws IOException {
		String home = System.getProperty("user.home");
		if (Utils.isBlank(home)) home = "~";
		File localRepo = new File(home, ".m2/repository").getCanonicalFile();
		if ( ! localRepo.isDirectory()) {			
			Log.w(LOGTAG, "incSrc: Cannot find Maven local repository to look for sources: Not a directory: "+localRepo);
			return;
		}
		// TODO find the right jars
		List<File> found = FileUtils.find(localRepo, ".+-sources\\.jar");
		for (File file : found) {
			if (output.contains(file.getName())) {				
				File out = new File(outDir, stripVersion(file.getName()));
				FileUtils.copy(file, out);
			}
		}
		System.out.println(found);		 	
	}

	private String stripVersion(String name) {
		Pattern p = Pattern.compile("(.+)-[^-]+-sources.jar");
		Matcher m = p.matcher(name);
		boolean ok = m.matches();
		if ( ! ok) return null;
		return m.group(1)+"-sources.jar";
	}

	private void doMakePom(File pom) {
		if (pom.isFile()) {
			// one of ours??
			if ( ! FileUtils.read(pom).contains("MavenDependencyTask")) {
				throw new IllegalStateException("Cannot overwrite pom.xml (unless made by MavenDependencyTask, which this is not): "+pom.getAbsolutePath());
			}
		}
		FileUtils.write(pom, 
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<project>	\n" + 				
				"	<modelVersion>4.0.0</modelVersion>\n" +
				"<!-- Not a real pom! MavenDependencyTask pom for Bob -->" +
				"	<groupId>com.example</groupId>\n" + 
				"	<artifactId>dummy</artifactId>\n" + 
				"	<version>0.0.1-SNAPSHOT</version>\n" + 
				"	<name>dummy</name>\n" + 
				"	<dependencies>\n" + 
				StrUtils.join(dependencies,"\n") +
				"	</dependencies>\n" + 
				"</project>\n" + 
				"");
	}
	@Override
	public String toString() {
		return "MavenDependencyTask[mavenArtifactSpec=" + mavenArtifactSpec + ", outDir=" + outDir + ", projectDir="
				+ projectDir + "]";
	}

	
	
}
