package com.winterwell.bob.tasks;

import java.io.File;
import java.util.List;

import com.winterwell.utils.Printer;

import com.winterwell.utils.ReflectionUtils;

/**
 * Run JavaDoc. Calls the command line. TODO fix for Windows
 * 
 * @author Daniel
 * 
 */
public class JavaDocTask extends ProcessTask {

	/**
	Oh this is daft! this argument breaks Java 7 but is essential for backwards compatibility on Java 8.
	See http://blog.joda.org/2014/02/turning-off-doclint-in-jdk-8-javadoc.html
	 */
	public void setDoclintFlag(boolean doclintFlag) {
		this.doclintFlag = doclintFlag;
	}
	
	private final File outputDir;
	/**
	 * // TODO use javadoc -X and check the output to see if doclint is supported
			// -- since your javadoc might not match your JVM java version
			// Oh this is daft! this argument breaks Java 7 but is essential for backwards compatibility on Java 8
			// See http://blog.joda.org/2014/02/turning-off-doclint-in-jdk-8-javadoc.html
	 */
	Boolean doclintFlag;
	
	public JavaDocTask(String topLevelPackage, File srcDir, File outputDir) {
		super("javadoc");
		this.outputDir = outputDir;
		addArg("-sourcepath " + srcDir.getAbsolutePath());
		addArg("-subpackages " + topLevelPackage);		
	}
	
	public JavaDocTask(String topLevelPackage, File srcDir, File outputDir, 
			List<File> classpath) 
	{
		this(topLevelPackage, srcDir, outputDir);
		if (classpath != null && !classpath.isEmpty()) {
			addArg("-classpath");
			addArg(Printer.toString(classpath, ":"));
		}		
	}

	@Override
	public void doTask() throws Exception {
		if (doclintFlag==null) {
			double jv = ReflectionUtils.getJavaVersion();
			doclintFlag = jv > 1.7;
		}
		if (doclintFlag) {
			// TODO use javadoc -X and check the output to see if doclint is supported
			// -- since your javadoc might not match your JVM java version
			// Oh this is daft! this argument breaks Java 7 but is essential for backwards compatibility on Java 8
			// See http://blog.joda.org/2014/02/turning-off-doclint-in-jdk-8-javadoc.html
			addArg("-Xdoclint:none");
		}
		
		outputDir.mkdirs();
		addArg("-d " + outputDir.getAbsolutePath());
		super.doTask();
	}

}
