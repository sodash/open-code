package com.winterwell.bob.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Proc;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
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

	File outDir;
	File projectDir = FileUtils.getWorkingDirectory();
	
	public MavenDependencyTask setOutputDirectory(File outDir) {
		this.outDir = outDir;
		return this;
	}
	public MavenDependencyTask setProjectDir(File projectDir) {
		this.projectDir = projectDir;
		return this;
	}
	
	List<String> dependencies = new ArrayList();
	private File pom;
	
	private boolean keepJarVersioning;
	
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
	
	public MavenDependencyTask setPom(File pom) {
		this.pom = pom;
		return this;
	}
	
	@Override
	protected void doTask() throws Exception {
		// files
		if (outDir==null) {
			outDir = new File(projectDir, "dependencies");
		}
		if (dependencies.isEmpty()) {
			if (pom==null) pom = FileUtils.or(new File(projectDir, "pom.xml"), new File(projectDir, "pom.bob.xml"));
			if (pom==null || ! pom.isFile()) {
				throw new IllegalStateException("No pom.xml found and no in-Java dependencies were added: "+pom+" "+projectDir);
			}
		} else {		
			if (pom==null) pom = new File(projectDir, "pom.bob.xml");
			doMakePom(pom);
		}
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
			
			Proc proc = new Proc("mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.2:copy-dependencies"
					+( ! keepJarVersioning? " -Dmdep.stripVersion=true" : "")
					+ " -DoutputDirectory="+outDir);
			proc.setDirectory(projectDir);
			Log.d(LOGTAG, "dir: "+projectDir+" run: "+proc.getCommand());
			proc.start();
			proc.waitFor(new Dt(10, TUnit.MINUTE));
			proc.close();
			Log.w(LOGTAG, proc.getError());
			Log.d(LOGTAG, proc.getOutput());
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
