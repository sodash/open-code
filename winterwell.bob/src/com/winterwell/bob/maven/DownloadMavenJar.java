package com.winterwell.bob.maven;

import java.io.File;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.FakeBrowser;

public class DownloadMavenJar extends BuildTask {

	private String groupId;
	private String artifactId;
	private String version;
	private File libDir;
	private File downloadedJar;

	public DownloadMavenJar(String groupId, String artifactId, String version, File libDir) {
		this.groupId=groupId;
		this.artifactId=artifactId;
		this.version = version;
		this.libDir=libDir;		
	}
	
	@Override
	protected void doTask() throws Exception {
		FakeBrowser fb = new FakeBrowser();
	    File download = fb.getFile("http://search.maven.org/remotecontent?filepath="+groupId+"/"+artifactId+"/"+version+"/"+artifactId+"-"+version+".jar");
	    libDir.mkdirs();
	    assert libDir.isDirectory();
	    downloadedJar = new File(libDir, download.getName());
	    FileUtils.move(download, downloadedJar);
//		compile 'org.eclipse.jetty:jetty-servlet:8.1.13.v20130916'
//	    http://search.maven.org/remotecontent?filepath=org/eclipse/jetty/jetty-servlet/8.1.13.v20130916/jetty-servlet-8.1.13.v20130916.jar
	}
	
	public File getDownloadedJar() {
		return downloadedJar;
	}

}
