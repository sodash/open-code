package com.winterwell.bob.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Proc;
import com.winterwell.utils.StrUtils;
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

	File outDir = new File("maven-dependencies");
	File projectDir = FileUtils.getWorkingDirectory();
	
	public void setOutputDirectory(File outDir) {
		this.outDir = outDir;
	}
	
	List<String> dependencies = new ArrayList();
	
	public void addDependency(String groupId, String artifactId, String version) {
		dependencies.add("<dependency><groupId>"+groupId+"</groupId><artifactId>"+artifactId+"</artifactId>" 
				+"<version>"+version+"</version>" 
//				"			<scope>test</scope>\n" + 
				+"</dependency>"); 
	}
	
	@Override
	protected void doTask() throws Exception {
		File pom = new File(projectDir, "pom.xml");
		if (dependencies.isEmpty()) {
			if ( ! pom.isFile()) {
				throw new IllegalStateException("No pom.xml found and no in-Java dependencies were added: "+pom.getAbsolutePath());
			}
		} else {		
			doMakePom(pom);
		}
		// 
		// http://maven.apache.org/plugins/maven-dependency-plugin/copy-dependencies-mojo.html
//		-DoutputDirectory (defaults to build/dependency)
//		-DstripVersion=true or useBaseVersion for less aggressive 
		
		Proc proc = new Proc("mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.2:copy-dependencies -DstripVersion=true -DoutputDirectory="+outDir);
		proc.start();
		proc.waitFor(new Dt(10, TUnit.MINUTE));
		proc.close();
		Log.w(LOGTAG, proc.getError());
		Log.d(LOGTAG, proc.getOutput());
		// did it work??		
		if ( ! proc.getOutput().contains("BUILD SUCCESS")) {
			throw new FailureException(proc.getError());
		}		
		FileUtils.delete(pom);
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

}
